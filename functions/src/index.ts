/**
 * Cloud Functions for Map-In Push Notifications
 *
 * This function triggers when a new notification document is created in Firestore
 * and sends push notifications to all the recipient's devices.
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import axios from "axios";
import {
  AiRecommendationRequest,
  AiRecommendationResponse,
  AiEventSummary,
} from "./aiTypes";

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

/**
 * AI Event Recommendation Function
 *
 * This function receives a user query and a list of events, then uses OpenRouter
 * (Amazon Nova 2 Lite) to recommend 2-3 relevant events based on the query.
 */
export const recommendEvents = functions.https.onCall(
  async (data: AiRecommendationRequest, context): Promise<AiRecommendationResponse> => {
    console.log("AI recommendation request received");
    console.log("User query:", data.userQuery);
    console.log("Number of events:", data.events.length);

    try {
      // OpenRouter API configuration
      const OPENROUTER_API_KEY = functions.config().openrouter.api_key;
      const OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";
      const MODEL = "amazon/nova-2-lite-v1:free";

      // Get current date for filtering future events
      const now = new Date();
      const currentDateStr = now.toISOString().split("T")[0];
      const currentTimeStr = now.toTimeString().split(" ")[0];

      // Format events for the AI
      const eventsFormatted = data.events.map((event: AiEventSummary) => {
        const startDate = event.startTime
          ? new Date(event.startTime._seconds * 1000).toLocaleString("fr-FR")
          : "Date non spécifiée";

        return {
          id: event.id,
          title: event.title,
          description: event.description || "Pas de description",
          date: startDate,
          tags: event.tags.join(", "),
          location: event.locationDescription || "Lieu non spécifié",
          distance: event.distanceKm ? `${event.distanceKm.toFixed(1)} km` : "Distance inconnue",
          price: event.price === 0 ? "Gratuit" : `${event.price} CHF`,
          places: event.capacityRemaining !== undefined && event.capacityRemaining !== null
            ? `${event.capacityRemaining} places restantes`
            : "Places illimitées"
        };
      });

      // System prompt for the AI
      const systemPrompt = `Tu es un assistant vocal qui recommande des événements à des utilisateurs.

Date et heure actuelles: ${currentDateStr} à ${currentTimeStr}

IMPORTANT:
- Recommande UNIQUEMENT 2 à 3 événements qui correspondent le mieux à la demande de l'utilisateur
- Ne recommande QUE des événements qui ont lieu APRÈS la date/heure actuelle
- Analyse les tags, la description, la date et l'heure de chaque événement
- Donne une raison claire et courte pour chaque recommandation
- Ton message sera lu à haute voix, donc sois naturel et conversationnel
- Réponds en français

Format de réponse OBLIGATOIRE (JSON strict):
{
  "assistantMessage": "Un message conversationnel pour l'utilisateur (2-3 phrases maximum)",
  "recommendedEvents": [
    {
      "id": "event_id_1",
      "reason": "Raison courte et claire (max 15 mots)"
    }
  ],
  "followupQuestions": ["Question 1?", "Question 2?"]
}

NE RÉPONDS QU'EN JSON, RIEN D'AUTRE.`;

      const userPrompt = `Requête de l'utilisateur: "${data.userQuery}"

Événements disponibles:
${JSON.stringify(eventsFormatted, null, 2)}

Recommande 2-3 événements qui correspondent le mieux à cette demande.`;

      console.log("Calling OpenRouter API...");

      // Call OpenRouter API
      const response = await axios.post(
        OPENROUTER_API_URL,
        {
          model: MODEL,
          messages: [
            {
              role: "system",
              content: systemPrompt
            },
            {
              role: "user",
              content: userPrompt
            }
          ],
          temperature: 0.7,
          max_tokens: 1000,
        },
        {
          headers: {
            "Authorization": `Bearer ${OPENROUTER_API_KEY}`,
            "Content-Type": "application/json",
            "HTTP-Referer": "https://mapin.app", // Optional but recommended
            "X-Title": "Map-In Event Recommender" // Optional but recommended
          }
        }
      );

      console.log("OpenRouter API response received");

      // Parse AI response
      const aiResponseText = response.data.choices[0].message.content;
      console.log("AI response text:", aiResponseText);

      // Try to extract JSON from the response (AI might wrap it in markdown)
      let jsonResponse: AiRecommendationResponse;
      try {
        // Remove markdown code blocks if present
        const cleanedResponse = aiResponseText
          .replace(/```json\n?/g, "")
          .replace(/```\n?/g, "")
          .trim();

        const parsed = JSON.parse(cleanedResponse);

        // Validate response structure
        if (!parsed.assistantMessage || typeof parsed.assistantMessage !== "string") {
          throw new Error("Missing or invalid assistantMessage field");
        }
        if (!Array.isArray(parsed.recommendedEvents)) {
          throw new Error("Missing or invalid recommendedEvents field");
        }

        // Validate each recommended event
        for (const event of parsed.recommendedEvents) {
          if (!event.id || !event.reason) {
            throw new Error("Recommended event missing id or reason");
          }
        }

        jsonResponse = parsed as AiRecommendationResponse;
        console.log("Successfully parsed and validated AI response");
      } catch (parseError) {
        console.error("Failed to parse AI response as JSON:", parseError);
        console.error("Raw response:", aiResponseText);

        // Fallback response
        jsonResponse = {
          assistantMessage: "Désolé, je n'ai pas pu analyser correctement les événements disponibles. Peux-tu reformuler ta demande?",
          recommendedEvents: [],
          followupQuestions: [
            "Veux-tu voir tous les événements disponibles?",
            "Peux-tu préciser tes préférences?"
          ]
        };
      }

      console.log("Returning recommendations:", jsonResponse.recommendedEvents.length, "events");
      return jsonResponse;

    } catch (error: any) {
      console.error("Error in AI recommendation:", error);
      console.error("Error details:", error.response?.data || error.message);

      // Return user-friendly error response
      return {
        assistantMessage: "Désolé, j'ai rencontré un problème technique. Peux-tu réessayer dans quelques instants?",
        recommendedEvents: [],
        followupQuestions: [
          "Veux-tu que je réessaie?",
          "Peux-tu reformuler ta demande?"
        ]
      };
    }
  }
);
