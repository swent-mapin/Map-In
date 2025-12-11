package com.swent.mapin.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Recursively searches for the Activity instance in the Context hierarchy.
 *
 * This extension function traverses the Context wrapper chain to find the underlying Activity. It's
 * particularly useful in Compose where you might have nested ContextWrappers but need to access the
 * Activity for operations like showing dialogs or checking lifecycle state.
 *
 * **Tail-Recursive Implementation:**
 * This function is marked with `tailrec`, allowing the Kotlin compiler to optimize the recursion
 * into an iterative loop, preventing stack overflow issues when traversing deep Context hierarchies.
 *
 * **Common Android/Compose Use Case:**
 * In Jetpack Compose, the `LocalContext.current` often provides a ContextWrapper rather than the
 * Activity directly. This is a common gotcha when you need Activity-specific methods (e.g.,
 * `startActivity()`, `requestPermissions()`, or accessing the `ComponentActivity` for lifecycle).
 *
 * **Usage Example:**
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val context = LocalContext.current
 *     val activity = context.findActivity()
 *
 *     Button(onClick = {
 *         activity?.let {
 *             // Use the activity for permissions, navigation, etc.
 *             it.requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
 *         }
 *     }) {
 *         Text("Request Permission")
 *     }
 * }
 * ```
 *
 * **Caveats:**
 * - Returns `null` if the Context is not associated with an Activity (e.g., Application context,
 *   Service context, or other non-Activity contexts).
 * - Always check for `null` before using the returned Activity to avoid NPEs.
 * - In tests or special contexts (e.g., background services), this may not return an Activity.
 *
 * @return The Activity instance if found in the Context hierarchy, null otherwise
 * @see [ContextWrapper Documentation](https://developer.android.com/reference/android/content/ContextWrapper)
 * @see [Compose LocalContext](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/package-summary#LocalContext())
 */
tailrec fun Context.findActivity(): Activity? =
    when (this) {
      is Activity -> this
      is ContextWrapper -> baseContext.findActivity()
      else -> null
    }
