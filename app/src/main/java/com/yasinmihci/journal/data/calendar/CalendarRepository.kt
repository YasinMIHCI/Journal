package com.yasinmihci.journal.data.calendar

import com.yasinmihci.journal.data.local.dao.CourseDao
import com.yasinmihci.journal.data.local.entity.CourseEntity
import com.yasinmihci.journal.data.settings.SettingsRepository
import com.yasinmihci.journal.domain.calendar.Course
import com.yasinmihci.journal.domain.calendar.IcsParser
import com.yasinmihci.journal.domain.calendar.PARIS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate

class CalendarRepository(
    private val dao: CourseDao,
    private val settings: SettingsRepository,
    private val syncState: SyncStateRepository,
) {
    private val syncMutex = Mutex()

    /** Cours de [date] filtrés par les groupes des réglages (réagit aux changements de groupes). */
    fun observeCourses(date: LocalDate): Flow<List<Course>> {
        val from = date.atStartOfDay(PARIS).toInstant().toEpochMilli()
        val to = date.plusDays(1).atStartOfDay(PARIS).toInstant().toEpochMilli()
        return combine(dao.observeBetween(from, to), settings.settings) { entities, s ->
            entities.map { it.toCourse() }.filter { it.matches(s.icsGroups) }
        }
    }

    /**
     * Télécharge l'ICS et remplace les cours stockés. En cas d'erreur, les cours existants sont gardés.
     * @return nombre de cours gardés après filtre.
     */
    suspend fun sync(): SyncOutcome = syncMutex.withLock {
        val s = settings.current()
        if (s.icsUrl.isBlank()) return SyncOutcome.NoUrl.also { syncState.recordError(NO_URL) }
        try {
            val text = download(s.icsUrl)
            if (!text.contains("BEGIN:VCALENDAR")) throw IOException("réponse non ICS")
            val courses = IcsParser.parse(text)
            dao.replaceAll(courses.map { it.toEntity() })
            val kept = courses.count { it.matches(s.icsGroups) }
            syncState.recordSuccess(System.currentTimeMillis(), kept)
            SyncOutcome.Success(kept)
        } catch (e: IOException) {
            syncState.recordError(e.message ?: e.javaClass.simpleName)
            SyncOutcome.Error(e)
        }
    }

    private suspend fun download(url: String): String = withContext(Dispatchers.IO) {
        val fixed = url.trim().replaceFirst(Regex("^webcal://", RegexOption.IGNORE_CASE), "https://")
        val conn = URL(fixed).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IllegalArgumentException) {
            throw IOException("URL invalide", e)
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val NO_URL = "URL ICS manquante"
    }
}

sealed interface SyncOutcome {
    data class Success(val courses: Int) : SyncOutcome
    data object NoUrl : SyncOutcome
    data class Error(val cause: Exception) : SyncOutcome
}

private fun Course.toEntity() = CourseEntity(
    uid = uid,
    startMs = start.toEpochMilli(),
    endMs = end.toEpochMilli(),
    summary = summary,
    location = location,
    description = descriptionLines.joinToString("\n"),
)

private fun CourseEntity.toCourse() = Course(
    uid = uid,
    start = Instant.ofEpochMilli(startMs),
    end = Instant.ofEpochMilli(endMs),
    summary = summary,
    location = location,
    descriptionLines = description.split('\n').filter { it.isNotBlank() },
)
