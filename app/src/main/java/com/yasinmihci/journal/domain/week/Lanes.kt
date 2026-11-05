package com.yasinmihci.journal.domain.week

import java.time.LocalTime

/** Élément placé dans une colonne de jour : [lane] parmi [lanes] colonnes côte à côte. */
data class Placed<T>(val item: T, val lane: Int, val lanes: Int)

/**
 * Disposition façon agenda : les éléments qui se chevauchent sont mis côte à côte.
 * Un groupe = éléments reliés par des chevauchements ; tous partagent le même nombre de colonnes.
 */
fun <T> layoutLanes(items: List<T>, start: (T) -> LocalTime, end: (T) -> LocalTime): List<Placed<T>> {
    val sorted = items.sortedWith(compareBy<T>({ start(it) }, { end(it) }))
    val result = mutableListOf<Placed<T>>()
    var group = mutableListOf<Pair<T, Int>>()
    var laneEnds = mutableListOf<LocalTime>()
    var groupEnd: LocalTime? = null

    fun flush() {
        val lanes = laneEnds.size.coerceAtLeast(1)
        group.forEach { (item, lane) -> result += Placed(item, lane, lanes) }
        group = mutableListOf()
        laneEnds = mutableListOf()
        groupEnd = null
    }

    for (item in sorted) {
        val s = start(item)
        val e = end(item)
        if (groupEnd != null && s >= groupEnd!!) flush()
        val lane = laneEnds.indexOfFirst { it <= s }.let { if (it == -1) laneEnds.size else it }
        if (lane == laneEnds.size) laneEnds += e else laneEnds[lane] = e
        group += item to lane
        groupEnd = if (groupEnd == null || e > groupEnd!!) e else groupEnd
    }
    flush()
    return result
}
