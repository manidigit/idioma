package com.manidigit.flashlearn.domain.review

import com.manidigit.flashlearn.database.entity.LearningStateEntity
import com.manidigit.flashlearn.database.entity.ReviewHistoryEntity

object ReviewStateMapper {
    fun toDomain(entity: LearningStateEntity): LearningState = LearningState(
        stage = ReviewStage.valueOf(entity.stage),
        difficulty = Difficulty.valueOf(entity.difficulty),
        difficultyScore = entity.difficultyScore,
        nextReviewAt = entity.nextReviewAt,
        totalReviewCount = entity.totalReviewCount,
        totalCorrectCount = entity.totalCorrectCount,
        totalIncorrectCount = entity.totalIncorrectCount,
        dailyReviewCount = entity.dailyReviewCount,
        dailyCorrectCount = entity.dailyCorrectCount,
        dailyIncorrectCount = entity.dailyIncorrectCount,
        weeklyReviewCount = entity.weeklyReviewCount,
        weeklyCorrectCount = entity.weeklyCorrectCount,
        weeklyIncorrectCount = entity.weeklyIncorrectCount,
        monthlyReviewCount = entity.monthlyReviewCount,
        monthlyCorrectCount = entity.monthlyCorrectCount,
        monthlyIncorrectCount = entity.monthlyIncorrectCount,
        monthlyFailureCount = entity.monthlyFailureCount,
        consecutiveCorrect = entity.consecutiveCorrect,
        consecutiveIncorrect = entity.consecutiveIncorrect,
        highestStageReached = ReviewStage.valueOf(entity.highestStageReached),
        weeklyToDailyReturns = entity.weeklyToDailyReturns,
        monthlyToDailyReturns = entity.monthlyToDailyReturns,
        successfulMonthlyCompletions = entity.successfulMonthlyCompletions,
        learnedCount = entity.learnedCount,
        lastReviewAt = entity.lastReviewedAt,
        lastReviewCorrect = entity.lastReviewCorrect
    )

    fun historyToDomain(entity: ReviewHistoryEntity): ReviewEvent = ReviewEvent(
        timestamp = entity.reviewDate,
        stage = ReviewStage.valueOf(entity.reviewStage),
        result = if (entity.isCorrect) ReviewResultType.CORRECT else ReviewResultType.INCORRECT,
        responseTimeMs = entity.responseTimeMs
    )

    fun toEntity(
        result: ReviewResult,
        conceptId: Long,
        learningStateId: Long,
        languagePairId: Long,
        sessionId: String,
        previousState: LearningState,
        userAnswer: String? = null,
        correctAnswer: String? = null
    ): ReviewHistoryEntity = ReviewHistoryEntity(
        conceptId = conceptId,
        learningStateId = learningStateId,
        languagePairId = languagePairId,
        sessionId = sessionId,
        reviewStage = result.event.stage.name,
        reviewDate = result.event.timestamp,
        isCorrect = result.event.result == ReviewResultType.CORRECT,
        userAnswer = userAnswer,
        correctAnswer = correctAnswer,
        previousStage = previousState.stage.name,
        newStage = result.state.stage.name,
        previousDifficulty = previousState.difficulty.name,
        newDifficulty = result.state.difficulty.name,
        difficultyScore = result.state.difficultyScore,
        responseTimeMs = result.event.responseTimeMs
    )

    fun updateEntity(old: LearningStateEntity, state: LearningState, now: Long): LearningStateEntity = old.copy(
        stage = state.stage.name,
        difficulty = state.difficulty.name,
        difficultyScore = state.difficultyScore,
        monthlyWrongCount = state.monthlyIncorrectCount,
        weeklyWrongCount = state.weeklyIncorrectCount,
        dailyWrongCount = state.dailyIncorrectCount,
        nextReviewAt = state.nextReviewAt,
        totalReviewCount = state.totalReviewCount,
        totalCorrectCount = state.totalCorrectCount,
        totalIncorrectCount = state.totalIncorrectCount,
        dailyReviewCount = state.dailyReviewCount,
        dailyCorrectCount = state.dailyCorrectCount,
        dailyIncorrectCount = state.dailyIncorrectCount,
        weeklyReviewCount = state.weeklyReviewCount,
        weeklyCorrectCount = state.weeklyCorrectCount,
        weeklyIncorrectCount = state.weeklyIncorrectCount,
        monthlyReviewCount = state.monthlyReviewCount,
        monthlyCorrectCount = state.monthlyCorrectCount,
        monthlyIncorrectCount = state.monthlyIncorrectCount,
        monthlyFailureCount = state.monthlyFailureCount,
        consecutiveCorrect = state.consecutiveCorrect,
        consecutiveIncorrect = state.consecutiveIncorrect,
        highestStageReached = state.highestStageReached.name,
        weeklyToDailyReturns = state.weeklyToDailyReturns,
        monthlyToDailyReturns = state.monthlyToDailyReturns,
        successfulMonthlyCompletions = state.successfulMonthlyCompletions,
        learnedCount = state.learnedCount,
        lastReviewedAt = state.lastReviewAt,
        lastReviewCorrect = state.lastReviewCorrect,
        learnedAt = if (state.stage == ReviewStage.LEARNED && old.learnedAt == null) now else old.learnedAt,
        updatedAt = now
    )
}
