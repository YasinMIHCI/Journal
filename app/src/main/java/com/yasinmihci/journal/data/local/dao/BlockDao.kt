package com.yasinmihci.journal.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.yasinmihci.journal.data.local.entity.BlockEntity
import com.yasinmihci.journal.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BlockDao {
    @Query("SELECT * FROM tags ORDER BY name")
    abstract fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM blocks")
    abstract fun observeBlocks(): Flow<List<BlockEntity>>

    @Query("SELECT * FROM tags")
    abstract suspend fun tags(): List<TagEntity>

    @Query("SELECT * FROM blocks")
    abstract suspend fun blocks(): List<BlockEntity>

    @Upsert
    abstract suspend fun upsertTag(tag: TagEntity): Long

    @Upsert
    abstract suspend fun upsertBlock(block: BlockEntity): Long

    @Query("DELETE FROM blocks WHERE id = :id")
    abstract suspend fun deleteBlock(id: Long)

    @Query("DELETE FROM blocks WHERE tagId = :tagId")
    protected abstract suspend fun deleteBlocksOfTag(tagId: Long)

    @Query("DELETE FROM tags WHERE id = :id")
    protected abstract suspend fun deleteTagRow(id: Long)

    /** Supprime le tag et tous ses blocs. */
    @Transaction
    open suspend fun deleteTag(id: Long) {
        deleteBlocksOfTag(id)
        deleteTagRow(id)
    }
}
