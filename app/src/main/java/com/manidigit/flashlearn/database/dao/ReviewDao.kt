package com.manidigit.flashlearn.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.manidigit.flashlearn.database.entity.LearningStateEntity
import com.manidigit.flashlearn.database.entity.ReviewHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM learning_states WHERE conceptId = :conceptId AND languagePairId = :languagePairId LIMIT 1")
    suspend fun getLearningState(conceptId: Long, languagePairId: Long): LearningStateEntity?

    @Query("SELECT * FROM learning_states WHERE languagePairId = :languagePairId AND stage = :stage AND nextReviewAt <= :now ORDER BY nextReviewAt ASC")
    fun observeDueByStage(languagePairId: Long, stage: String, now: Long): Flow<List<LearningStateEntity>>

    @Query("SELECT * FROM learning_states WHERE languagePairId = :languagePairId AND difficulty = :difficulty AND nextReviewAt <= :now ORDER BY nextReviewAt ASC")
    fun observeDueByDifficulty(languagePairId: Long, difficulty: String, now: Long): Flow<List<LearningStateEntity>>

    @Query("SELECT * FROM learning_states WHERE languagePairId = :languagePairId AND stage != 'LEARNED' AND nextReviewAt <= :now ORDER BY nextReviewAt ASC")
    fun observeAllDue(languagePairId: Long, now: Long): Flow<List<LearningStateEntity>>

    @Query("SELECT * FROM review_history WHERE learningStateId = :learningStateId ORDER BY reviewDate ASC, id ASC")
    suspend fun getHistory(learningStateId: Long): List<ReviewHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReviewHistory(event: ReviewHistoryEntity): Long

    @Update
    suspend fun updateLearningState(state: LearningStateEntity)
}
