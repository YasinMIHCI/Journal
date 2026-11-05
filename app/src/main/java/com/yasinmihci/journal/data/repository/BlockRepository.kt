package com.yasinmihci.journal.data.repository

import com.yasinmihci.journal.data.local.dao.BlockDao
import com.yasinmihci.journal.data.local.entity.BlockEntity
import com.yasinmihci.journal.data.local.entity.TagEntity
import com.yasinmihci.journal.domain.blocks.Block
import com.yasinmihci.journal.domain.blocks.BlockSchedule
import com.yasinmihci.journal.domain.blocks.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalTime

class BlockRepository(private val dao: BlockDao) {

    val schedule: Flow<BlockSchedule> = combine(dao.observeTags(), dao.observeBlocks()) { tags, blocks ->
        BlockSchedule(tags.map { it.toTag() }, blocks.map { it.toBlock() })
    }

    suspend fun current(): BlockSchedule =
        BlockSchedule(dao.tags().map { it.toTag() }, dao.blocks().map { it.toBlock() })

    suspend fun saveTag(tag: Tag): Long =
        dao.upsertTag(TagEntity(tag.id, tag.name, tag.color, tag.travelMinutes)).let { if (it == -1L) tag.id else it }

    suspend fun deleteTag(id: Long) = dao.deleteTag(id)

    suspend fun saveBlock(block: Block) {
        dao.upsertBlock(
            BlockEntity(
                id = block.id,
                tagId = block.tagId,
                title = block.title,
                startMinute = block.start.toSecondOfDay() / 60,
                endMinute = block.end.toSecondOfDay() / 60,
                date = block.date,
                weekdays = block.weekdays.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) },
                fromDate = block.from,
                untilDate = block.until,
            ),
        )
    }

    suspend fun deleteBlock(id: Long) = dao.deleteBlock(id)
}

private fun TagEntity.toTag() = Tag(id, name, color, travelMinutes)

private fun BlockEntity.toBlock() = Block(
    id = id,
    tagId = tagId,
    title = title,
    start = LocalTime.ofSecondOfDay(startMinute.coerceIn(0, 1439) * 60L),
    end = LocalTime.ofSecondOfDay(endMinute.coerceIn(0, 1439) * 60L),
    date = date,
    weekdays = DayOfWeek.entries.filter { weekdays and (1 shl (it.value - 1)) != 0 }.toSet(),
    from = fromDate,
    until = untilDate,
)
