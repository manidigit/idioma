package com.manidigit.flashlearn.domain.review

/**
 * Pure application service. A data layer can load the state/history, call this
 * service, then persist the event and updated state in one transaction.
 */
class AdaptiveReviewService {
    fun record(
        currentState: LearningState,
        completeHistory: List<ReviewEvent>,
        isCorrect: Boolean,
        now: Long,
        responseTimeMs: Long? = null
    ): ReviewResult = ReviewEngine.recordReviewResult(
        state = currentState,
        history = completeHistory,
        isCorrect = isCorrect,
        now = now,
        responseTimeMs = responseTimeMs
    )

    fun calculateDifficulty(completeHistory: List<ReviewEvent>): DifficultyAssessment =
        ReviewEngine.calculateDifficulty(completeHistory)

    fun calculateNextReviewDate(stage: ReviewStage, isCorrect: Boolean, now: Long): Long =
        ReviewEngine.calculateNextReviewDate(stage, isCorrect, now)

    fun calculateNextLearningStage(stage: ReviewStage, isCorrect: Boolean): ReviewStage =
        ReviewEngine.calculateNextLearningStage(stage, isCorrect)
}
