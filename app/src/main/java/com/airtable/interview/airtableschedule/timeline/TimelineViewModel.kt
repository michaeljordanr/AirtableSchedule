package com.airtable.interview.airtableschedule.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airtable.interview.airtableschedule.models.Event
import com.airtable.interview.airtableschedule.models.dayWidthDefault
import com.airtable.interview.airtableschedule.repositories.EventDataRepository
import com.airtable.interview.airtableschedule.repositories.EventDataRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the state of the timeline screen.
 */
class TimelineViewModel : ViewModel() {
    private val eventDataRepository: EventDataRepository = EventDataRepositoryImpl()

    private val _events = MutableStateFlow<List<Event>>(emptyList())

    private val _editingEvent = MutableStateFlow<Event?>(null)

    private val _showAddNewEvent = MutableStateFlow<Boolean>(false)

    private val _dayWidthPx = MutableStateFlow(dayWidthDefault)

    val uiState: StateFlow<TimelineUiState> = combine(
        _events,
        _editingEvent,
        _dayWidthPx,
        _showAddNewEvent
    ) { events, editingEvent, dayWidthPx, showAddNewEvent ->
        TimelineUiState(events = events)
            .copy(editingEvent = editingEvent)
            .copy(dayWidthPx = dayWidthPx)
            .copy(isNewEvent = showAddNewEvent)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        TimelineUiState()
    )

    init {
        viewModelScope.launch {
            eventDataRepository.getTimelineItems().collect { list ->
                _events.value = list
            }
        }
    }

    fun startEditing(event: Event) {
        _editingEvent.value = event
    }

    fun cancelEditing() {
        _editingEvent.value = null
    }

    fun saveEvent(editedEvent: Event) {
        _events.update { currentEvents ->
            currentEvents.map { if (it.id == editedEvent.id) editedEvent else it }
        }
        _editingEvent.value = null
    }

    fun setDayWidthPx(widthPx: Float) {
        _dayWidthPx.value = widthPx
    }

    fun showAddNewEvent(show: Boolean) {
        _showAddNewEvent.value = show
    }

    fun addEvent(event: Event) {
        _events.value = _events.value + event
        showAddNewEvent(false)
    }
}
