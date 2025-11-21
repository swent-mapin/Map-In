package com.swent.mapin.model.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Represents the connectivity state of the device.
 *
 * @property isConnected True if the device has internet connectivity, false otherwise
 * @property networkType The type of network connection (WIFI, CELLULAR, etc.), null if disconnected
 */
data class ConnectivityState(val isConnected: Boolean, val networkType: NetworkType? = null)

/** Enum representing the type of network connection */
enum class NetworkType {
  WIFI,
  CELLULAR,
  ETHERNET,
  OTHER
}

/**
 * Interface for monitoring network connectivity state.
 *
 * Provides a reactive Flow-based API to observe connectivity changes in real-time.
 */
interface ConnectivityService {
  /**
   * Flow that emits the current connectivity state whenever it changes.
   *
   * Subscribers will receive updates when the device transitions between online and offline states,
   * or when the network type changes.
   */
  val connectivityState: Flow<ConnectivityState>

  /**
   * Checks if the device is currently connected to a network.
   *
   * @return True if connected, false otherwise
   */
  fun isConnected(): Boolean
}

/**
 * Default implementation of ConnectivityService using Android's ConnectivityManager.
 *
 * Monitors network connectivity using NetworkCallback for real-time updates. Handles edge cases
 * like airplane mode, WiFi without internet, and mobile data toggles.
 *
 * @param context Application context
 */
class ConnectivityServiceImpl(context: Context) : ConnectivityService {
  private val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  companion object {
    private const val TAG = "ConnectivityService"
  }

  override val connectivityState: Flow<ConnectivityState> =
      callbackFlow {
            // Send initial state with error handling
            try {
              val initialState = getCurrentConnectivityState()
              Log.d(
                  TAG,
                  "Initial connectivity: ${initialState.isConnected} (${initialState.networkType})")
              trySend(initialState)
            } catch (e: Exception) {
              // If we can't determine initial state, assume disconnected
              Log.e(TAG, "Failed to get initial connectivity state", e)
              trySend(ConnectivityState(isConnected = false, networkType = null))
            }

            val networkCallback =
                object : ConnectivityManager.NetworkCallback() {
                  override fun onAvailable(network: Network) {
                    Log.d(TAG, "onAvailable called")
                    // Don't trust onAvailable - wait for onCapabilitiesChanged
                  }

                  override fun onLost(network: Network) {
                    try {
                      // Network is definitely lost - don't query getCurrentConnectivityState
                      // as it may still show the network due to race condition
                      val state = ConnectivityState(isConnected = false, networkType = null)
                      Log.d(TAG, "Network lost: offline")
                      trySend(state)
                    } catch (e: Exception) {
                      // Ignore state update errors - previous state remains
                    }
                  }

                  override fun onCapabilitiesChanged(
                      network: Network,
                      networkCapabilities: NetworkCapabilities
                  ) {
                    try {
                      val state = getCurrentConnectivityState()
                      Log.d(
                          TAG,
                          "Network capabilities changed: ${state.isConnected} (${state.networkType})")
                      trySend(state)
                    } catch (e: Exception) {
                      // Ignore state update errors - previous state remains
                    }
                  }
                }

            val networkRequest =
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build()

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

            awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
          }
          .distinctUntilChanged()

  override fun isConnected(): Boolean {
    return getCurrentConnectivityState().isConnected
  }

  /**
   * Gets the current connectivity state by querying ConnectivityManager.
   *
   * @return Current ConnectivityState
   */
  @VisibleForTesting
  internal fun getCurrentConnectivityState(): ConnectivityState {
    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

    // Check if network has internet and is validated (actually connected to internet)
    val isConnected =
        capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    val networkType =
        if (isConnected) {
          when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                NetworkType.ETHERNET
            else -> NetworkType.OTHER
          }
        } else {
          null
        }

    return ConnectivityState(isConnected = isConnected, networkType = networkType)
  }
}
