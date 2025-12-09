/**
 * Types for AI Event Recommendation System
 */

/**
 * Event summary sent to the AI for analysis
 */
export interface AiEventSummary {
  id: string;
  title: string;
  description?: string;
  startTime?: {
    _seconds: number;
    _nanoseconds: number;
  };
  endTime?: {
    _seconds: number;
    _nanoseconds: number;
  };
  tags: string[];
  distanceKm?: number;
  locationDescription?: string;
  capacityRemaining?: number;
  price: number;
}

/**
 * User context for personalization
 */
export interface AiUserContext {
  approxLocation?: string;
  maxDistanceKm?: number;
  timeWindowStart?: {
    _seconds: number;
    _nanoseconds: number;
  };
  timeWindowEnd?: {
    _seconds: number;
    _nanoseconds: number;
  };
}

/**
 * Request to the AI recommendation endpoint
 */
export interface AiRecommendationRequest {
  userQuery: string;
  userContext: AiUserContext;
  events: AiEventSummary[];
}

/**
 * Single recommended event in the response
 */
export interface AiRecommendedEvent {
  id: string;
  reason: string;
}

/**
 * Response from the AI recommendation endpoint
 */
export interface AiRecommendationResponse {
  assistantMessage: string;
  recommendedEvents: AiRecommendedEvent[];
  followupQuestions?: string[];
}

