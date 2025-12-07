package com.swent.mapin.model.location

/** Custom exception for location search errors. */
class LocationSearchException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
