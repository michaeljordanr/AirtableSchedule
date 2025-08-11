package com.airtable.interview.airtableschedule.timeline

import com.airtable.interview.airtableschedule.models.Event
import com.airtable.interview.airtableschedule.models.dayWidthDefault

/**
 * UI state for the timeline screen.
 */
data class TimelineUiState(
    val events: List<Event> = emptyList(),
    val isNewEvent: Boolean = false,
    val editingEvent: Event? = null,
    val dayWidthPx: Float = dayWidthDefault
)
