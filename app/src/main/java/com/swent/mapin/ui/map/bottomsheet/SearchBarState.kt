package com.swent.mapin.ui.map.bottomsheet

import androidx.compose.runtime.Stable

/**
 * State holder for the map search bar component.
 *
 * This class encapsulates all the state and callbacks needed to manage the search bar on the map
 * screen, including query text, focus state, and user interactions.
 *
 * @property query Current search query text
 * @property shouldRequestFocus Whether the search bar should request focus
 * @property onQueryChange Callback invoked when the query text changes
 * @property onTap Callback invoked when the search bar is tapped
 * @property onFocusHandled Callback to acknowledge focus request has been handled
 * @property onClear Callback invoked when the clear button is pressed
 * @property onSubmit Callback invoked when the user submits the search query
 */
@Stable
data class SearchBarState(
    val query: String,
    val shouldRequestFocus: Boolean,
    val onQueryChange: (String) -> Unit,
    val onTap: () -> Unit,
    val onFocusHandled: () -> Unit,
    val onClear: () -> Unit,
    val onSubmit: () -> Unit = {}
)
