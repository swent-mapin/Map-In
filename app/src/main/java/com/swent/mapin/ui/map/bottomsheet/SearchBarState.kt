package com.swent.mapin.ui.map.bottomsheet

import androidx.compose.runtime.Stable

/** State holder for the map search bar. */
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
