package com.yasinmihci.journal.ui.common

import android.app.TimePickerDialog
import android.content.Context
import java.time.LocalTime

/** Sélecteur d'heure système, format 24 h. */
fun showTimePicker(context: Context, initial: LocalTime, onSet: (LocalTime) -> Unit) {
    TimePickerDialog(context, { _, h, m -> onSet(LocalTime.of(h, m)) }, initial.hour, initial.minute, true).show()
}
