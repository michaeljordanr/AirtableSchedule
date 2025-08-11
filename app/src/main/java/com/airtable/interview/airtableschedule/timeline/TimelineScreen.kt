package com.airtable.interview.airtableschedule.timeline

import EditEventDialog
import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airtable.interview.airtableschedule.models.Event
import com.airtable.interview.airtableschedule.models.assignLanes
import com.airtable.interview.airtableschedule.models.toDate
import com.airtable.interview.airtableschedule.models.toDateFormatted
import com.airtable.interview.airtableschedule.models.toDateFormattedShort
import com.airtable.interview.airtableschedule.models.toLocalDate
import kotlinx.coroutines.launch
import java.util.Date

/**
 * A screen that displays a timeline of events.
 */
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val minDayWidthPx = 80f
    val maxDayWidthPx = 300f

    val dayWidthDp: Dp = with(LocalDensity.current) { uiState.dayWidthPx.toDp() }

    Column {
        Row(
            modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                viewModel.showAddNewEvent(true)
            }) {
                Text("Add")
            }
            Button(onClick = {
                viewModel.setDayWidthPx((uiState.dayWidthPx - 10f).coerceAtLeast(minDayWidthPx))
            }) {
                Text("Zoom Out")
            }
            Button(onClick = {
                viewModel.setDayWidthPx((uiState.dayWidthPx + 10f).coerceAtLeast(minDayWidthPx))
            }) {
                Text("Zoom In")
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid: Offset, pan: Offset, zoom: Float, rotation: Float ->
                        viewModel.setDayWidthPx(
                            (uiState.dayWidthPx * zoom).coerceIn(
                                minDayWidthPx, maxDayWidthPx
                            )
                        )
                    }
                }) {
            TimelineView(events = uiState.events, dayWidth = dayWidthDp, onEventClick = { event ->
                viewModel.startEditing(event)
            }, onEventDragEnd = { updatedEvent ->
                viewModel.saveEvent(updatedEvent)
            })

            uiState.editingEvent?.let { editingEvent ->
                EditEventDialog(
                    event = editingEvent,
                    onDismiss = { viewModel.cancelEditing() },
                    onSave = { updatedEvent -> viewModel.saveEvent(updatedEvent) })
            }

            if (uiState.isNewEvent) {
                EditEventDialog(
                    event = null,
                    onDismiss = { viewModel.showAddNewEvent(false) },
                    onSave = { newEvent -> viewModel.addEvent(newEvent) }
                )
            }
        }
    }
}

/**
 * A view that displays a list of events in swimlanes format.
 *
 * @param events The list of events to display.
 */
@Composable
private fun TimelineView(
    events: List<Event>,
    dayWidth: Dp,
    onEventClick: (Event) -> Unit,
    onEventDragEnd: (Event) -> Unit
) {
    if (events.isEmpty()) {
        Text("No events available", modifier = Modifier.padding(16.dp))
        return
    }

    val lanes = assignLanes(events)

    val minDate = events.minOf { it.startDate }.time
    val maxDate = events.maxOf { it.endDate }.time
    val totalDurationDays = ((maxDate - minDate) / MILLIS_PER_DAY).toInt() + 1

    val totalWidth = dayWidth * totalDurationDays

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .background(Color.LightGray)
        ) {
            (0 until totalDurationDays).forEach { dayOffset ->
                val date = Date(minDate + dayOffset * MILLIS_PER_DAY)
                Box(
                    modifier = Modifier
                        .width(dayWidth)
                        .height(30.dp)
                        .border(0.5.dp, Color.DarkGray), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.toDateFormattedShort(), fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .width(totalWidth)
        ) {
            lanes.forEach { lane ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.Start
                ) {
                    var lastEventEndOffset = 0

                    lane.forEach { event ->
                        val startOffsetDays =
                            ((event.startDate.time - minDate) / MILLIS_PER_DAY).toInt()
                        val eventDurationDays =
                            ((event.endDate.time - event.startDate.time) / MILLIS_PER_DAY).toInt() + 1

                        val gap = startOffsetDays - lastEventEndOffset
                        if (gap > 0) {
                            Spacer(modifier = Modifier.width(dayWidth * gap))
                        }

                        EventView(
                            event = event,
                            durationDays = eventDurationDays,
                            dayWidth = dayWidth,
                            scrollState = scrollState,
                            onClick = { onEventClick(event) },
                            onDragEnd = { onEventDragEnd(it) })

                        lastEventEndOffset = startOffsetDays + eventDurationDays
                    }

                    val remaining = totalDurationDays - lastEventEndOffset
                    if (remaining > 0) {
                        Spacer(modifier = Modifier.width(dayWidth * remaining))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}


/**
 * Single event view.
 */
@Composable
private fun EventView(
    event: Event,
    durationDays: Int,
    dayWidth: Dp,
    scrollState: ScrollState,
    onDragEnd: (Event) -> Unit,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var offsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .width(dayWidth * durationDays)
            .height(40.dp)
            .offset(x = offsetX.dp)
            .background(Color.Cyan, shape = RoundedCornerShape(4.dp))
            .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .pointerInput(event.id) {
                detectDragGesturesAfterLongPress(onDrag = { change, dragAmount ->
                    offsetX += dragAmount.x
                    Log.d("EventView", "onDrag: $offsetX")
                    Log.d("EventView", "dayWidth: $dayWidth")
                    Log.d("EventView", "dayWidthPx: ${dayWidth.toPx()}")
                    change.consume()

                    val viewportWidth = size.width
                    val currentScroll = scrollState.value
                    val maxScroll = scrollState.maxValue

                    val positionOnScreen = offsetX + this.size.width / 2

                    val edgeThreshold = viewportWidth * 0.25f

                    when {
                        positionOnScreen > viewportWidth - edgeThreshold -> {
                            scope.launch {
                                scrollState.animateScrollTo(
                                    (currentScroll + 20).coerceAtMost(maxScroll)
                                )
                            }
                        }

                        positionOnScreen < edgeThreshold -> {
                            scope.launch {
                                scrollState.animateScrollTo(
                                    (currentScroll - 20).coerceAtLeast(0)
                                )
                            }
                        }
                    }
                }, onDragEnd = {
                    val dayWidthPx = dayWidth.toPx()

                    val daysShifted = (Dp(offsetX).toPx() / dayWidthPx).toInt()

                    if (daysShifted != 0) {
                        val newStartLocal =
                            event.startDate.toLocalDate().plusDays(daysShifted.toLong())
                        val newEndLocal = event.endDate.toLocalDate().plusDays(daysShifted.toLong())

                        val updatedEvent = event.copy(
                            startDate = newStartLocal.toDate(), endDate = newEndLocal.toDate()
                        )

                        onDragEnd(updatedEvent)
                    }

                    offsetX = 0f
                }, onDragCancel = {
                    offsetX = 0f
                })
            }
            .clickable { onClick() }) {
        Column {
            Text(
                text = event.name,
                fontSize = 12.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${event.startDate.toDateFormatted()} - ${event.endDate.toDateFormatted()}",
                fontSize = 12.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}

private const val MILLIS_PER_DAY = 1000L * 60 * 60 * 24
