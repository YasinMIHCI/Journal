package com.yasinmihci.journal.domain.calendar

import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Vérifie le vrai export ADE s'il est fourni (données perso, jamais commitées) :
 * `ADE_ICS=/chemin/ADECal.ics ./gradlew testDebugUnitTest`. Sinon ignoré.
 */
class RealIcsTest {

    @Test
    fun realExport_noOverlapAfterFilter() {
        val path = System.getenv("ADE_ICS")
        assumeTrue(path != null && File(path).exists())
        val all = IcsParser.parse(File(path!!).readText())
        val kept = all.filter { it.matches(listOf("IT4 FISA", "IT4 FISA - Cyber", "4A")) }.sortedBy { it.start }
        println("ADE : ${all.size} événements, ${kept.size} gardés")
        val overlaps = kept.zipWithNext().filter { (a, b) -> b.start < a.end }
        assertEquals(emptyList<Pair<Course, Course>>(), overlaps)
        System.getenv("ADE_ICS_EXPECTED")?.let { assertEquals(it.toInt(), kept.size) }
    }
}
