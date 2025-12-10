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
 * @return The Activity instance if found in the Context hierarchy, null otherwise
 */
tailrec fun Context.findActivity(): Activity? =
    when (this) {
      is Activity -> this
      is ContextWrapper -> baseContext.findActivity()
      else -> null
    }
