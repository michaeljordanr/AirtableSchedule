package com.airtable.interview.airtableschedule.models

const val dayWidthDefault = 150f

/**
 * Takes a list of [Event]s and assigns them to lanes based on start/end dates.
 */
fun assignLanes(events: List<Event>): List<List<Event>> {
    val lanes = mutableListOf<MutableList<Event>>()

    // Go through the list of events sorted by start date
    events.sortedBy { event -> event.startDate }
        .forEach { event ->
            // Attempt to assign the event to an existing lane
            val availableLane = lanes.find { lane ->
                lane.last().endDate < event.startDate
            }

            if (availableLane != null) {
                availableLane.add(event)
            } else {
                // Create a new lane if there are currently no free lanes to assign the event
                lanes.add(mutableListOf(event))
            }
        }
    return lanes
}
