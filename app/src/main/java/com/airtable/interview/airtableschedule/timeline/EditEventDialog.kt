import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airtable.interview.airtableschedule.models.Event
import com.airtable.interview.airtableschedule.models.SampleTimelineItems.timelineItems
import com.airtable.interview.airtableschedule.models.toDate
import com.airtable.interview.airtableschedule.models.toLocalDate
import java.time.LocalDate
import java.util.Calendar

@Composable
fun EditEventDialog(
    event: Event?,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit
) {
    var name by remember { mutableStateOf(event?.name ?: "") }
    var startDate by remember {
        mutableStateOf(
            event?.startDate?.toLocalDate() ?: timelineItems.first().startDate.toLocalDate()
        )
    }
    var endDate by remember {
        mutableStateOf(
            event?.endDate?.toLocalDate() ?: timelineItems.first().startDate.toLocalDate()
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current


    val startDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                startDate = LocalDate.of(year, month + 1, dayOfMonth)
                // If new startDate is after endDate, adjust endDate automatically
                if (endDate.isBefore(startDate)) {
                    endDate = startDate
                }
            },
            startDate.year,
            startDate.monthValue - 1,
            startDate.dayOfMonth
        )
    }.apply {
        val minDateCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2020)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        this.datePicker.minDate = minDateCalendar.timeInMillis

        val maxDateCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2020)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
        }
        this.datePicker.maxDate = maxDateCalendar.timeInMillis
    }


    val endDatePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                if (!selected.isBefore(startDate)) {
                    endDate = selected
                    errorMessage = null
                } else {
                    errorMessage = "End date cannot be before start date"
                }
            },
            endDate.year,
            endDate.monthValue - 1,
            endDate.dayOfMonth
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            event?.let {
                Text("Edit Event")
            } ?: Text("New Event")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                Spacer(Modifier.height(16.dp))


                OutlinedButton(
                    onClick = { startDatePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Start Date: $startDate", textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(8.dp))


                OutlinedButton(
                    onClick = { endDatePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "End Date: $endDate", textAlign = TextAlign.Center)
                }

                errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (endDate.isBefore(startDate)) {
                    errorMessage = "End date cannot be before start date"
                    return@TextButton
                }
                if (name.isBlank()) {
                    errorMessage = "Name cannot be empty"
                    return@TextButton
                }
                val newEvent = Event(
                    id = event?.id ?: (timelineItems.last().id + 1),
                    name = name,
                    startDate = startDate.toDate(),
                    endDate = endDate.toDate()
                )
                onSave(newEvent)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
