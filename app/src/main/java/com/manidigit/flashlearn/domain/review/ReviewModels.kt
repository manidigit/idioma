package com.manidigit.flashlearn.domain.review

/** Learning stage is independent from difficulty. */
enum class ReviewStage { DAILY, WEEKLY, MONTHLY, LEARNED }
enum class Difficulty { EASY, MEDIUM, HARD, VERY_HARD }
enum class ReviewResultType { CORRECT, INCORRECT }

data class ReviewEvent(
    val timestamp: Long,
    val stage: ReviewStage,
    val result: ReviewResultType,
    val responseTimeMs: Long? = null
)

data class LearningState(
    val stage: ReviewStage = ReviewStage.DAILY,
    val difficulty: Difficulty = Difficulty.EASY,
    val difficultyScore: Int = 0,
    val nextReviewAt: Long = 0L,
    val totalReviewCount: Int = 0,
    val totalCorrectCount: Int = 0,
    val totalIncorrectCount: Int = 0,
    val dailyReviewCount: Int = 0,
    val dailyCorrectCount: Int = 0,
    val dailyIncorrectCount: Int = 0,
    val weeklyReviewCount: Int = 0,
    val weeklyCorrectCount: Int = 0,
    val weeklyIncorrectCount: Int = 0,
    val monthlyReviewCount: Int = 0,
    val monthlyCorrectCount: Int = 0,
    val monthlyIncorrectCount: Int = 0,
    val monthlyFailureCount: Int = 0,
    val consecutiveCorrect: Int = 0,
    val consecutiveIncorrect: Int = 0,
    val highestStageReached: ReviewStage = ReviewStage.DAILY,
    val weeklyToDailyReturns: Int = 0,
    val monthlyToDailyReturns: Int = 0,
    val successfulMonthlyCompletions: Int = 0,
    val learnedCount: Int = 0,
    val lastReviewAt: Long? = null,
    val lastReviewCorrect: Boolean? = null
)

data class ReviewResult(
    val state: LearningState,
    val event: ReviewEvent,
    val difficultyAssessment: DifficultyAssessment
)

data class DifficultyAssessment(
    val score: Int,
    val difficulty: Difficulty,
    val rawScore: Int,
    val recoveryCredit: Int
)

object DifficultyClassifier {
    private const val DAILY_WEIGHT = 1
    private const val WEEKLY_WEIGHT = 3
    private const val MONTHLY_WEIGHT = 5

    /**
     * Derives the current difficulty from the complete immutable review history.
     * History is never discarded when a card returns to DAILY.
     */
    fun calculateDifficulty(history: List<ReviewEvent>): DifficultyAssessment {
        if (history.isEmpty()) return DifficultyAssessment(0, Difficulty.EASY, 0, 0)

        val ordered = history.sortedBy { it.timestamp }
        var raw = 0
        var previousStage: ReviewStage? = null
        var sameStageFailureRun = 0
        var weeklyFailures = 0
        var monthlyFailures = 0
        var dailyFailures = 0

        ordered.forEach { event ->
            if (event.result == ReviewResultType.INCORRECT) {
                when (event.stage) {
                    ReviewStage.DAILY -> {
                        dailyFailures++
                        raw += DAILY_WEIGHT
                        raw += when (dailyFailures) {
                            2 -> 1
                            3 -> 2
                            else -> if (dailyFailures > 3) 3 else 0
                        }
                    }
                    ReviewStage.WEEKLY -> {
                        weeklyFailures++
                        raw += WEEKLY_WEIGHT
                        // Returning from WEEKLY to DAILY is a separate retention signal.
                        raw += 3
                        raw += when (weeklyFailures) {
                            2 -> 4
                            else -> if (weeklyFailures >= 3) 5 else 0
                        }
                    }
                    ReviewStage.MONTHLY -> {
                        monthlyFailures++
                        raw += MONTHLY_WEIGHT
                        // Returning from MONTHLY to DAILY is the strongest retention signal.
                        raw += 5
                        raw += when (monthlyFailures) {
                            1 -> 0
                            2 -> 5
                            else -> 8
                        }
                    }
                    ReviewStage.LEARNED -> Unit
                }

                if (previousStage == event.stage) sameStageFailureRun++ else sameStageFailureRun = 1
                raw += when (sameStageFailureRun) {
                    2 -> 2
                    3 -> 4
                    4 -> 6
                    else -> if (sameStageFailureRun > 4) 6 else 0
                }
            } else {
                sameStageFailureRun = 0
            }
            previousStage = event.stage
        }

        // Recovery is deliberately capped. Old mistakes remain evidence; successful
        // recent performance reduces their influence without erasing history.
        var consecutiveCorrect = 0
        for (event in ordered.asReversed()) {
            if (event.result == ReviewResultType.CORRECT) consecutiveCorrect++ else break
        }
        val recentWindow = ordered.takeLast(8)
        val recentCorrect = recentWindow.count { it.result == ReviewResultType.CORRECT }
        val recentIncorrect = recentWindow.count { it.result == ReviewResultType.INCORRECT }
        val recentNet = recentCorrect - recentIncorrect
        val recoveryCredit = (consecutiveCorrect.coerceAtMost(6) / 2) +
            if (recentNet > 0) recentNet.coerceAtMost(3) else 0

        // A completed clean learning path remains EASY. Any meaningful failure keeps
        // a minimum evidence floor so a single successful answer cannot hide it.
        val score = (raw - recoveryCredit).coerceAtLeast(0)

        val thresholdDifficulty = when {
            score >= 15 -> Difficulty.VERY_HARD
            score >= 8 -> Difficulty.HARD
            score >= 3 -> Difficulty.MEDIUM
            else -> Difficulty.EASY
        }

        val successfulMonthly = ordered.count {
            it.stage == ReviewStage.MONTHLY && it.result == ReviewResultType.CORRECT
        }
        val evidenceFloor = when {
            monthlyFailures >= 3 && successfulMonthly < 2 -> Difficulty.VERY_HARD
            monthlyFailures >= 1 && successfulMonthly < 1 -> Difficulty.HARD
            weeklyFailures >= 1 || dailyFailures >= 1 -> Difficulty.MEDIUM
            else -> Difficulty.EASY
        }

        val difficulty = maxDifficulty(thresholdDifficulty, evidenceFloor)

        return DifficultyAssessment(
            score = score,
            difficulty = difficulty,
            rawScore = raw,
            recoveryCredit = recoveryCredit
        )
    }

    private fun maxDifficulty(a: Difficulty, b: Difficulty): Difficulty =
        if (a.ordinal >= b.ordinal) a else b
}

object ReviewEngine {
    const val DAY_MILLIS = 24L * 60 * 60 * 1000
    const val WEEK_MILLIS = 7L * DAY_MILLIS
    const val MONTH_MILLIS = 30L * DAY_MILLIS

    fun calculateNextReviewDate(stage: ReviewStage, isCorrect: Boolean, now: Long): Long = when (stage) {
        ReviewStage.DAILY -> if (isCorrect) now + WEEK_MILLIS else now + DAY_MILLIS
        ReviewStage.WEEKLY -> if (isCorrect) now + MONTH_MILLIS else now + DAY_MILLIS
        ReviewStage.MONTHLY -> if (isCorrect) Long.MAX_VALUE else now + DAY_MILLIS
        ReviewStage.LEARNED -> Long.MAX_VALUE
    }

    fun calculateNextLearningStage(stage: ReviewStage, isCorrect: Boolean): ReviewStage = when (stage) {
        ReviewStage.DAILY -> if (isCorrect) ReviewStage.WEEKLY else ReviewStage.DAILY
        ReviewStage.WEEKLY -> if (isCorrect) ReviewStage.MONTHLY else ReviewStage.DAILY
        ReviewStage.MONTHLY -> if (isCorrect) ReviewStage.LEARNED else ReviewStage.DAILY
        ReviewStage.LEARNED -> ReviewStage.LEARNED
    }

    /**
     * Records one immutable event and derives the new state from the complete history.
     * The caller persists both the event and the returned state in one DB transaction.
     */
    fun recordReviewResult(
        state: LearningState,
        history: List<ReviewEvent>,
        isCorrect: Boolean,
        now: Long,
        responseTimeMs: Long? = null
    ): ReviewResult {
        val event = ReviewEvent(
            timestamp = now,
            stage = state.stage,
            result = if (isCorrect) ReviewResultType.CORRECT else ReviewResultType.INCORRECT,
            responseTimeMs = responseTimeMs
        )
        val updatedHistory = history + event
        val assessment = DifficultyClassifier.calculateDifficulty(updatedHistory)
        val newStage = calculateNextLearningStage(state.stage, isCorrect)
        val nextReview = calculateNextReviewDate(state.stage, isCorrect, now)
        val stats = aggregate(updatedHistory)

        val newState = state.copy(
            stage = newStage,
            difficulty = assessment.difficulty,
            difficultyScore = assessment.score,
            nextReviewAt = nextReview,
            totalReviewCount = stats.totalReviewCount,
            totalCorrectCount = stats.totalCorrectCount,
            totalIncorrectCount = stats.totalIncorrectCount,
            dailyReviewCount = stats.dailyReviewCount,
            dailyCorrectCount = stats.dailyCorrectCount,
            dailyIncorrectCount = stats.dailyIncorrectCount,
            weeklyReviewCount = stats.weeklyReviewCount,
            weeklyCorrectCount = stats.weeklyCorrectCount,
            weeklyIncorrectCount = stats.weeklyIncorrectCount,
            monthlyReviewCount = stats.monthlyReviewCount,
            monthlyCorrectCount = stats.monthlyCorrectCount,
            monthlyIncorrectCount = stats.monthlyIncorrectCount,
            monthlyFailureCount = stats.monthlyIncorrectCount,
            consecutiveCorrect = stats.consecutiveCorrect,
            consecutiveIncorrect = stats.consecutiveIncorrect,
            highestStageReached = maxStage(stats.highestStageReached, newStage),
            weeklyToDailyReturns = stats.weeklyToDailyReturns,
            monthlyToDailyReturns = stats.monthlyToDailyReturns,
            successfulMonthlyCompletions = stats.successfulMonthlyCompletions,
            learnedCount = stats.learnedCount,
            lastReviewAt = now,
            lastReviewCorrect = isCorrect
        ).let {
            if (newStage == ReviewStage.LEARNED && isCorrect) it.copy(learnedCount = stats.learnedCount) else it
        }

        return ReviewResult(newState, event, assessment)
    }

    fun calculateDifficulty(history: List<ReviewEvent>): DifficultyAssessment =
        DifficultyClassifier.calculateDifficulty(history)

    fun isDue(state: LearningState, now: Long): Boolean =
        state.stage != ReviewStage.LEARNED && state.nextReviewAt <= now

    private fun aggregate(history: List<ReviewEvent>): LearningState {
        val ordered = history.sortedBy { it.timestamp }
        fun count(stage: ReviewStage, result: ReviewResultType? = null) = ordered.count {
            it.stage == stage && (result == null || it.result == result)
        }
        var correctRun = 0
        var incorrectRun = 0
        for (event in ordered.asReversed()) {
            if (event.result == ReviewResultType.CORRECT) correctRun++ else break
        }
        for (event in ordered.asReversed()) {
            if (event.result == ReviewResultType.INCORRECT) incorrectRun++ else break
        }
        val highest = ordered.maxOfOrNull { it.stage.ordinal }?.let { ReviewStage.entries[it] } ?: ReviewStage.DAILY
        val weeklyReturns = ordered.count { it.stage == ReviewStage.WEEKLY && it.result == ReviewResultType.INCORRECT }
        val monthlyReturns = ordered.count { it.stage == ReviewStage.MONTHLY && it.result == ReviewResultType.INCORRECT }
        val monthlySuccesses = ordered.count { it.stage == ReviewStage.MONTHLY && it.result == ReviewResultType.CORRECT }

        return LearningState(
            totalReviewCount = ordered.size,
            totalCorrectCount = count(ReviewStage.DAILY, ReviewResultType.CORRECT) + count(ReviewStage.WEEKLY, ReviewResultType.CORRECT) + count(ReviewStage.MONTHLY, ReviewResultType.CORRECT) + count(ReviewStage.LEARNED, ReviewResultType.CORRECT),
            totalIncorrectCount = ordered.count { it.result == ReviewResultType.INCORRECT },
            dailyReviewCount = count(ReviewStage.DAILY),
            dailyCorrectCount = count(ReviewStage.DAILY, ReviewResultType.CORRECT),
            dailyIncorrectCount = count(ReviewStage.DAILY, ReviewResultType.INCORRECT),
            weeklyReviewCount = count(ReviewStage.WEEKLY),
            weeklyCorrectCount = count(ReviewStage.WEEKLY, ReviewResultType.CORRECT),
            weeklyIncorrectCount = count(ReviewStage.WEEKLY, ReviewResultType.INCORRECT),
            monthlyReviewCount = count(ReviewStage.MONTHLY),
            monthlyCorrectCount = count(ReviewStage.MONTHLY, ReviewResultType.CORRECT),
            monthlyIncorrectCount = count(ReviewStage.MONTHLY, ReviewResultType.INCORRECT),
            monthlyFailureCount = count(ReviewStage.MONTHLY, ReviewResultType.INCORRECT),
            consecutiveCorrect = correctRun,
            consecutiveIncorrect = incorrectRun,
            highestStageReached = highest,
            weeklyToDailyReturns = weeklyReturns,
            monthlyToDailyReturns = monthlyReturns,
            successfulMonthlyCompletions = monthlySuccesses,
            learnedCount = monthlySuccesses
        )
    }

    private fun maxStage(a: ReviewStage, b: ReviewStage): ReviewStage =
        if (a.ordinal >= b.ordinal) a else b
}
