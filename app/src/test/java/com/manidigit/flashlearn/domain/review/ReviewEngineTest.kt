package com.manidigit.flashlearn.domain.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewEngineTest {
    private val t = 1_000_000L

    @Test
    fun cleanPathBecomesLearnedAndEasy() {
        var state = LearningState()
        var history = emptyList<ReviewEvent>()
        listOf(true, true, true).forEachIndexed { i, correct ->
            val result = ReviewEngine.recordReviewResult(state, history, correct, t + i * ReviewEngine.DAY_MILLIS)
            state = result.state
            history += result.event
        }
        assertEquals(ReviewStage.LEARNED, state.stage)
        assertEquals(Difficulty.EASY, state.difficulty)
        assertEquals(0, state.difficultyScore)
    }

    @Test
    fun dailyWrongThenRecoveryIsMedium() {
        var state = LearningState()
        var history = emptyList<ReviewEvent>()
        val answers = listOf(false, false, true, true, true)
        answers.forEachIndexed { i, correct ->
            val result = ReviewEngine.recordReviewResult(state, history, correct, t + i * ReviewEngine.DAY_MILLIS)
            state = result.state
            history += result.event
        }
        assertEquals(ReviewStage.LEARNED, state.stage)
        assertEquals(Difficulty.MEDIUM, state.difficulty)
        assertEquals(5, state.totalReviewCount)
        assertEquals(2, state.dailyIncorrectCount)
    }

    @Test
    fun weeklyFailureMakesMediumEvenAfterSuccessfulRecovery() {
        var state = LearningState()
        var history = emptyList<ReviewEvent>()
        val answers = listOf(true, false, true, true, true)
        answers.forEachIndexed { i, correct ->
            val result = ReviewEngine.recordReviewResult(state, history, correct, t + i * ReviewEngine.DAY_MILLIS)
            state = result.state
            history += result.event
        }
        assertEquals(ReviewStage.LEARNED, state.stage)
        assertEquals(Difficulty.MEDIUM, state.difficulty)
        assertEquals(1, state.weeklyToDailyReturns)
    }

    @Test
    fun repeatedWeeklyFailuresReachHard() {
        var state = LearningState()
        var history = emptyList<ReviewEvent>()
        val answers = listOf(true, false, true, false, true, true)
        answers.forEachIndexed { i, correct ->
            val result = ReviewEngine.recordReviewResult(state, history, correct, t + i * ReviewEngine.DAY_MILLIS)
            state = result.state
            history += result.event
        }
        assertEquals(Difficulty.HARD, state.difficulty)
        assertEquals(2, state.weeklyIncorrectCount)
        assertEquals(2, state.weeklyToDailyReturns)
    }

    @Test
    fun firstMonthlyFailureReturnsToDailyAndRaisesDifficulty() {
        var state = LearningState()
        var history = emptyList<ReviewEvent>()
        listOf(true, true, false).forEachIndexed { i, correct ->
            val result = ReviewEngine.recordReviewResult(state, history, correct, t + i * ReviewEngine.DAY_MILLIS)
            state = result.state
            history += result.event
        }
        assertEquals(ReviewStage.DAILY, state.stage)
        assertEquals(Difficulty.HARD, state.difficulty)
        assertEquals(1, state.monthlyIncorrectCount)
        assertEquals(1, state.monthlyFailureCount)
        assertEquals(1, state.monthlyToDailyReturns)
    }

    @Test
    fun repeatedMonthlyFailuresBecomeVeryHard() {
        var state = LearningState()
        var history = emptyList<ReviewEvent>()
        // Repeat the complete path three times, failing each monthly retention test.
        repeat(3) { cycle ->
            val answers = listOf(true, true, false)
            answers.forEachIndexed { index, correct ->
                val result = ReviewEngine.recordReviewResult(
                    state,
                    history,
                    correct,
                    t + (cycle * 3L + index) * ReviewEngine.DAY_MILLIS
                )
                state = result.state
                history += result.event
            }
        }
        assertEquals(Difficulty.VERY_HARD, state.difficulty)
        assertEquals(3, state.monthlyFailureCount)
        assertEquals(3, state.monthlyToDailyReturns)
    }

    @Test
    fun dailyFailuresIncreaseScoreProgressively() {
        var state = LearningState()
        var history = emptyList<ReviewEvent>()
        var previousScore = -1
        repeat(5) { i ->
            val result = ReviewEngine.recordReviewResult(state, history, false, t + i * ReviewEngine.DAY_MILLIS)
            state = result.state
            history += result.event
            assertTrue(result.state.difficultyScore >= previousScore)
            previousScore = result.state.difficultyScore
        }
        assertTrue(state.dailyIncorrectCount == 5)
        assertTrue(state.difficulty.ordinal >= Difficulty.MEDIUM.ordinal)
    }

    @Test
    fun successfulReviewsCanReduceCurrentScoreWithoutDeletingHistory() {
        var state = LearningState()
        var history = emptyList<ReviewEvent>()
        listOf(true, true, false).forEachIndexed { i, correct ->
            val result = ReviewEngine.recordReviewResult(state, history, correct, t + i * ReviewEngine.DAY_MILLIS)
            state = result.state
            history += result.event
        }
        val failedScore = state.difficultyScore
        repeat(3) { i ->
            val result = ReviewEngine.recordReviewResult(state, history, true, t + (3L + i) * ReviewEngine.DAY_MILLIS)
            state = result.state
            history += result.event
        }
        assertTrue(state.difficultyScore < failedScore)
        assertEquals(6, history.size)
        assertEquals(1, history.count { it.result == ReviewResultType.INCORRECT })
        assertEquals(1, state.monthlyFailureCount)
    }

    @Test
    fun dueDateNeverShowsFutureCards() {
        val state = LearningState(stage = ReviewStage.WEEKLY, nextReviewAt = t + 1000)
        assertFalse(ReviewEngine.isDue(state, t))
        assertTrue(ReviewEngine.isDue(state, t + 1000))
    }

    @Test
    fun learnedStateHasNoScheduledReview() {
        val state = LearningState(stage = ReviewStage.LEARNED, nextReviewAt = Long.MAX_VALUE)
        assertFalse(ReviewEngine.isDue(state, Long.MAX_VALUE))
        assertEquals(Long.MAX_VALUE, ReviewEngine.calculateNextReviewDate(ReviewStage.LEARNED, false, t))
    }

    @Test
    fun monthlyFailureIsMuchStrongerThanDailyFailure() {
        val daily = ReviewEngine.calculateDifficulty(
            listOf(ReviewEvent(t, ReviewStage.DAILY, ReviewResultType.INCORRECT))
        )
        val monthly = ReviewEngine.calculateDifficulty(
            listOf(ReviewEvent(t, ReviewStage.MONTHLY, ReviewResultType.INCORRECT))
        )
        assertTrue(monthly.score > daily.score)
        assertTrue(monthly.difficulty.ordinal > daily.difficulty.ordinal)
    }
}

class VocabularyDuplicatePolicyTest {
    @Test
    fun equivalentWhitespaceAndUnicodeFormsAreDuplicates() {
        assertTrue(com.manidigit.flashlearn.domain.vocabulary.VocabularyDuplicatePolicy.isSameText(" casa  ", "CASA"))
        assertTrue(com.manidigit.flashlearn.domain.vocabulary.VocabularyDuplicatePolicy.isSameText("کتاب", "کتاب"))
        assertTrue(com.manidigit.flashlearn.domain.vocabulary.VocabularyDuplicatePolicy.isSameText("كلمه", "کلمه"))
    }

    @Test
    fun differentTranslationsAreNotDuplicates() {
        assertFalse(com.manidigit.flashlearn.domain.vocabulary.VocabularyDuplicatePolicy.isSameText("خانه", "منزل"))
    }
}
