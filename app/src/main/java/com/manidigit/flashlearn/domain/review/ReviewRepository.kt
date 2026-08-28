package com.manidigit.flashlearn.domain.review

import androidx.room3.withWriteTransaction
import com.manidigit.flashlearn.database.FlashLearnDatabase
import com.manidigit.flashlearn.database.entity.LearningStateEntity
import com.manidigit.flashlearn.database.dao.ReviewDao

/**
 * Atomic review write: load complete history -> calculate -> append immutable
 * history -> update the materialized learning state in one SQLite transaction.
 */
class ReviewRepository(
    private val database: FlashLearnDatabase,
    private val dao: ReviewDao = database.reviewDao()
) {
    suspend fun recordReview(
        stateEntity: LearningStateEntity,
        sessionId: String,
        isCorrect: Boolean,
        now: Long,
        userAnswer: String? = null,
        correctAnswer: String? = null,
        responseTimeMs: Long? = null
    ): ReviewResult = database.withWriteTransaction {
        val historyEntities = dao.getHistory(stateEntity.id)
        val history = historyEntities.map(ReviewStateMapper::historyToDomain)
        val state = ReviewStateMapper.toDomain(stateEntity)
        val result = ReviewEngine.recordReviewResult(state, history, isCorrect, now, responseTimeMs)

        dao.insertReviewHistory(
            ReviewStateMapper.toEntity(
                result = result,
                conceptId = stateEntity.conceptId,
                learningStateId = stateEntity.id,
                languagePairId = stateEntity.languagePairId,
                sessionId = sessionId,
                previousState = state,
                userAnswer = userAnswer,
                correctAnswer = correctAnswer
            )
        )
        dao.updateLearningState(ReviewStateMapper.updateEntity(stateEntity, result.state, now))
        result
    }

    suspend fun calculateDifficulty(learningStateId: Long): DifficultyAssessment {
        val history = dao.getHistory(learningStateId).map(ReviewStateMapper::historyToDomain)
        return ReviewEngine.calculateDifficulty(history)
    }

    suspend fun debugSnapshot(stateEntity: LearningStateEntity): ReviewDebugSnapshot {
        val history = dao.getHistory(stateEntity.id).map(ReviewStateMapper::historyToDomain)
        return ReviewDebugSnapshot(
            state = ReviewStateMapper.toDomain(stateEntity),
            assessment = ReviewEngine.calculateDifficulty(history),
            history = history
        )
    }
}

// Debug/diagnostic projection used by the future vocabulary detail screen.
data class ReviewDebugSnapshot(
    val state: LearningState,
    val assessment: DifficultyAssessment,
    val history: List<ReviewEvent>
)
