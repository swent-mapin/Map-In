/**
 * Cloud Functions for Map-In Push Notifications
 *
 * This function triggers when a new notification document is created in Firestore
 * and sends push notifications to all the recipient's devices.
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Initialize Firebase Admin SDK
admin.initializeApp();

/**
 * Notification type enum - must match the Android NotificationType enum
 */
enum NotificationType {
  INFO = "INFO",
  ALERT = "ALERT",
  REMINDER = "REMINDER",
  MESSAGE = "MESSAGE",
  FRIEND_REQUEST = "FRIEND_REQUEST",
  EVENT_INVITATION = "EVENT_INVITATION",
  SYSTEM = "SYSTEM"
}

/**
 * Notification document structure from Firestore
 */
interface NotificationData {
  notificationId: string;
  title: string;
  message: string;
  type: NotificationType;
  recipientId: string;
  senderId?: string;
  readStatus: boolean;
  timestamp: admin.firestore.Timestamp;
  metadata?: { [key: string]: string };
  actionUrl?: string;
  priority?: number;
}

/**
 * Cloud Function that triggers when a new notification is created.
 * Sends push notifications to all devices registered for the recipient user.
 */
export const sendNotificationOnCreate = functions.firestore
  .document("notifications/{notificationId}")
  .onCreate(async (snap, context) => {
    const notificationId = context.params.notificationId;
    const notification = snap.data() as NotificationData;

    console.log(`Processing notification ${notificationId} for user ${notification.recipientId}`);

    try {
      // Get the recipient user's FCM tokens
      const userDoc = await admin.firestore()
        .collection("users")
        .doc(notification.recipientId)
        .get();

      if (!userDoc.exists) {
        console.log(`User ${notification.recipientId} not found`);
        return;
      }

      const userData = userDoc.data();
      const tokens: string[] = userData?.fcmTokens || [];

      if (tokens.length === 0) {
        console.log(`No FCM tokens found for user ${notification.recipientId}`);
        return;
      }

      console.log(`Found ${tokens.length} device(s) for user ${notification.recipientId}`);

      // Build the notification payload for individual sending (FCM v1 API)
      // Safely handle optional fields - default to empty/fallback values if undefined/null
      const metadata = notification.metadata ?? {};
      const priority = notification.priority ?? 0;

      // Send to each token individually to avoid /batch endpoint issues
      let successCount = 0;
      let failureCount = 0;
      const failedTokens: string[] = [];

      for (const token of tokens) {
        try {
          const message: admin.messaging.Message = {
            notification: {
              title: notification.title,
              body: notification.message,
            },
            data: {
              notificationId: notificationId,
              type: notification.type,
              actionUrl: notification.actionUrl || "",
              recipientId: notification.recipientId,
              senderId: notification.senderId || "",
              // Convert metadata object to strings for FCM data payload
              ...Object.keys(metadata).reduce((acc, key) => {
                acc[`metadata_${key}`] = metadata[key];
                return acc;
              }, {} as { [key: string]: string }),
            },
            // Set Android-specific options
            android: {
              priority: priority >= 2 ? "high" : "normal",
              notification: {
                channelId: getChannelIdForType(notification.type),
                priority: priority >= 2 ? "high" : "default",
                defaultSound: true,
                defaultVibrateTimings: true,
              },
            },
            token: token,
          };

          await admin.messaging().send(message);
          successCount++;
          console.log(`Successfully sent notification to token ${tokens.indexOf(token) + 1}/${tokens.length}`);
        } catch (error: any) {
          failureCount++;
          console.error(`Failed to send to token ${tokens.indexOf(token) + 1}:`, error?.code, error?.message);

          // Remove token if it's invalid or unregistered
          if (
            error?.code === "messaging/invalid-registration-token" ||
            error?.code === "messaging/registration-token-not-registered"
          ) {
            failedTokens.push(token);
          }
        }
      }

      console.log(`Successfully sent ${successCount} notification(s)`);
      console.log(`Failed to send ${failureCount} notification(s)`);

      // Clean up invalid tokens
      if (failedTokens.length > 0) {
        console.log(`Removing ${failedTokens.length} invalid token(s)`);
        await admin.firestore()
          .collection("users")
          .doc(notification.recipientId)
          .update({
            fcmTokens: admin.firestore.FieldValue.arrayRemove(...failedTokens),
          });
      }

      return {
        success: true,
        successCount: successCount,
        failureCount: failureCount,
      };
    } catch (error) {
      console.error(`Error sending notification ${notificationId}:`, error);
      throw error;
    }
  });

/**
 * Get the appropriate Android notification channel ID based on notification type.
 * Must match the channel IDs defined in NotificationBackgroundManager.kt
 */
function getChannelIdForType(type: NotificationType): string {
  switch (type) {
    case NotificationType.FRIEND_REQUEST:
      return "mapin_friend_notifications";
    case NotificationType.EVENT_INVITATION:
    case NotificationType.REMINDER:
      return "mapin_event_notifications";
    case NotificationType.MESSAGE:
      return "mapin_message_notifications";
    case NotificationType.ALERT:
    case NotificationType.SYSTEM:
      return "mapin_alert_notifications";
    case NotificationType.INFO:
    default:
      return "mapin_notifications";
  }
}

/**
 * Optional: Cleanup function to delete old read notifications
 * Run this as a scheduled function (e.g., daily) to keep database clean
 */
export const cleanupOldNotifications = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async (context) => {
    console.log("Starting cleanup of old notifications...");

    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const snapshot = await admin.firestore()
      .collection("notifications")
      .where("readStatus", "==", true)
      .where("timestamp", "<", admin.firestore.Timestamp.fromDate(thirtyDaysAgo))
      .get();

    console.log(`Found ${snapshot.size} old notifications to delete`);

    // Delete in batches of 500 (Firestore limit)
    const batches: admin.firestore.WriteBatch[] = [];
    let batch = admin.firestore().batch();
    let count = 0;

    snapshot.docs.forEach((doc) => {
      batch.delete(doc.ref);
      count++;

      if (count === 500) {
        batches.push(batch);
        batch = admin.firestore().batch();
        count = 0;
      }
    });

    if (count > 0) {
      batches.push(batch);
    }

    await Promise.all(batches.map((b) => b.commit()));

    console.log(`Deleted ${snapshot.size} old notifications`);
    return null;
  });

