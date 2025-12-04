package com.swent.mapin.model.ai

// Assisted by AI

/**
 * Feature flags for the Map-In application.
 *
 * This object provides centralized control over feature availability.
 * In the future, these could be tied to remote configuration or BuildConfig.
 */
object FeatureFlags {
  /**
   * Controls whether the AI assistant feature is enabled.
   *
   * When true, the AI recommendation assistant UI and functionality will be available.
   * When false, the feature will be hidden from users.
   */
  const val AI_ASSISTANT_ENABLED: Boolean = true
}

