package com.yasinmihci.journal.data.repository

import com.yasinmihci.journal.data.local.dao.JournalDao
import com.yasinmihci.journal.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

class JournalRepository(
    private val dao: JournalDao,
    private val scope: CoroutineScope,
) {
    // Sérialise lectures/écritures : une lecture voit toujours la dernière frappe enregistrée.
    private val mutex = Mutex()

    suspend fun get(date: LocalDate): String = mutex.withLock { dao.get(date)?.text.orEmpty() }

    /** Enregistre sans bloquer l'appelant ; survit à la fermeture de l'écran. Texte vide = suppression. */
    fun save(date: LocalDate, text: String) {
        // UNDISPATCHED : les appels successifs prennent le verrou dans l'ordre des frappes.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.withLock {
                if (text.isBlank()) dao.delete(date)
                else dao.upsert(JournalEntryEntity(date, text, System.currentTimeMillis()))
            }
        }
    }
}
