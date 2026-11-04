package com.yasinmihci.journal.data.periods

import android.content.Context
import com.yasinmihci.journal.domain.day.DateRange
import com.yasinmihci.journal.domain.day.Periods
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Calendrier de l'alternance, lu depuis `assets/periods_4a_2026_2027.json`. */
class PeriodsRepository(private val context: Context) {
    private val mutex = Mutex()
    private var cached: Periods? = null

    suspend fun get(): Periods = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            val json = context.assets.open(ASSET).bufferedReader().use { it.readText() }
            parsePeriods(json)
        }.also { cached = it }
    }

    companion object {
        const val ASSET = "periods_4a_2026_2027.json"
    }
}

/** Format : `{ "school": [{start,end}], "company": [{start,end}], "holidays": ["yyyy-MM-dd"] }`. */
fun parsePeriods(json: String): Periods {
    val root = JSONObject(json)
    fun ranges(name: String): List<DateRange> {
        val arr = root.optJSONArray(name) ?: JSONArray()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            DateRange(LocalDate.parse(o.getString("start")), LocalDate.parse(o.getString("end")))
        }
    }
    val holidays = root.optJSONArray("holidays") ?: JSONArray()
    return Periods(
        school = ranges("school"),
        company = ranges("company"),
        holidays = (0 until holidays.length()).map { LocalDate.parse(holidays.getString(it)) }.toSet(),
    )
}
