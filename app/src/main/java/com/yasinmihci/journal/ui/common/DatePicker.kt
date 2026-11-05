package com.yasinmihci.journal.ui.common

import android.app.DatePickerDialog
import android.content.Context
import java.time.LocalDate

/** Sélecteur de date système. */
fun showDatePicker(context: Context, initial: LocalDate, onSet: (LocalDate) -> Unit) {
    DatePickerDialog(
        context,
        { _, y, m, d -> onSet(LocalDate.of(y, m + 1, d)) },
        initial.year, initial.monthValue - 1, initial.dayOfMonth,
    ).show()
}
