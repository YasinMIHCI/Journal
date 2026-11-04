package com.yasinmihci.journal.domain.calendar

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Parser ICS minimal (RFC 5545), suffisant pour l'export ADE :
 * VEVENT avec DTSTART/DTEND en UTC (`...Z`) ou locales (TZID ou flottantes → Paris).
 * Les événements « journée entière » (VALUE=DATE) sont ignorés. Pas de RRULE (ADE n'en produit pas).
 */
object IcsParser {

    private val DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    fun parse(text: String): List<Course> {
        val events = mutableListOf<Course>()
        var props: MutableMap<String, Property>? = null
        for (line in unfold(text)) {
            when {
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> props = mutableMapOf()
                line.equals("END:VEVENT", ignoreCase = true) -> {
                    props?.let { toCourse(it) }?.let(events::add)
                    props = null
                }
                props != null -> parseProperty(line)?.let { props.putIfAbsent(it.name, it) }
            }
        }
        return events
    }

    private class Property(val name: String, val params: Map<String, String>, val value: String)

    /** Lignes « repliées » : une ligne qui commence par espace/tab continue la précédente. */
    internal fun unfold(text: String): List<String> {
        val out = mutableListOf<String>()
        text.replace("\r\n", "\n").replace('\r', '\n').split('\n').forEach { raw ->
            if ((raw.startsWith(' ') || raw.startsWith('\t')) && out.isNotEmpty()) {
                out[out.lastIndex] = out.last() + raw.substring(1)
            } else if (raw.isNotEmpty()) {
                out += raw
            }
        }
        return out
    }

    private fun parseProperty(line: String): Property? {
        val colon = line.indexOf(':')
        if (colon <= 0) return null
        val head = line.substring(0, colon).split(';')
        val params = head.drop(1).mapNotNull { p ->
            val eq = p.indexOf('=')
            if (eq <= 0) null else p.substring(0, eq).uppercase() to p.substring(eq + 1).trim('"')
        }.toMap()
        return Property(head[0].uppercase(), params, line.substring(colon + 1))
    }

    private fun toCourse(p: Map<String, Property>): Course? {
        val start = p["DTSTART"]?.let(::parseInstant) ?: return null
        val end = p["DTEND"]?.let(::parseInstant) ?: start
        val description = unescape(p["DESCRIPTION"]?.value.orEmpty())
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("(Exported") }
        return Course(
            uid = p["UID"]?.value ?: "${start.epochSecond}-${p["SUMMARY"]?.value.hashCode()}",
            start = start,
            end = end,
            summary = unescape(p["SUMMARY"]?.value.orEmpty()).trim(),
            location = unescape(p["LOCATION"]?.value.orEmpty()).trim(),
            descriptionLines = description,
        )
    }

    private fun parseInstant(prop: Property): java.time.Instant? {
        val v = prop.value.trim()
        if (prop.params["VALUE"].equals("DATE", ignoreCase = true) || v.length == 8) return null
        return try {
            if (v.endsWith("Z", ignoreCase = true)) {
                LocalDateTime.parse(v.dropLast(1), DATE_TIME).toInstant(ZoneOffset.UTC)
            } else {
                val zone = prop.params["TZID"]?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: PARIS
                LocalDateTime.parse(v, DATE_TIME).atZone(zone).toInstant()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** `\n` → saut de ligne, `\,` `\;` `\\` → caractère. */
    internal fun unescape(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val n = s[i + 1]
                sb.append(if (n == 'n' || n == 'N') '\n' else n)
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
