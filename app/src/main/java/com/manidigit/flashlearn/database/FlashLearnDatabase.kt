package com.manidigit.flashlearn.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.manidigit.flashlearn.database.entity.*
import com.manidigit.flashlearn.database.dao.ReviewDao
import com.manidigit.flashlearn.database.dao.VocabularyDao

@Database(
    entities = [
        LanguageEntity::class,
        LanguagePairEntity::class,
        CategoryEntity::class,
        ConceptEntity::class,
        ContentEntity::class,
        TranslationEntity::class,
        ConceptLanguageKeyEntity::class,
        TagEntity::class,
        ConceptTagEntity::class,
        LearningStateEntity::class,
        ReviewSessionEntity::class,
        ReviewHistoryEntity::class,
        AppSettingEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class FlashLearnDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
    abstract fun vocabularyDao(): VocabularyDao
}

object DatabaseMigrations {
    val V1_TO_V2 = object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN difficultyScore INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN totalReviewCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN totalCorrectCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN totalIncorrectCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN dailyReviewCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN dailyCorrectCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN dailyIncorrectCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN weeklyReviewCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN weeklyCorrectCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN weeklyIncorrectCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN monthlyReviewCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN monthlyCorrectCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN monthlyIncorrectCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN monthlyFailureCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN consecutiveCorrect INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN consecutiveIncorrect INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN highestStageReached TEXT NOT NULL DEFAULT 'DAILY'")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN weeklyToDailyReturns INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN monthlyToDailyReturns INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN successfulMonthlyCompletions INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN learnedCount INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE learning_states ADD COLUMN lastReviewCorrect INTEGER DEFAULT NULL")
            connection.execSQL("ALTER TABLE review_history ADD COLUMN userAnswer TEXT DEFAULT NULL")
            connection.execSQL("ALTER TABLE review_history ADD COLUMN correctAnswer TEXT DEFAULT NULL")
            connection.execSQL("ALTER TABLE review_history ADD COLUMN difficultyScore INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_review_history_conceptId_reviewDate ON review_history(conceptId, reviewDate)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_learning_states_languagePairId_difficulty_nextReviewAt ON learning_states(languagePairId, difficulty, nextReviewAt)")
        }
    }

    /**
     * Review history is an immutable audit log. It must survive deletion/archive of
     * the current learning state, concept, or review session, so v3 removes all
     * cascading foreign keys from review_history.
     */
    val V2_TO_V3 = object : Migration(2, 3) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("CREATE TABLE IF NOT EXISTS translations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, conceptId INTEGER NOT NULL, languageCode TEXT NOT NULL, text TEXT NOT NULL, normalizedText TEXT NOT NULL, pronunciation TEXT, definition TEXT, example TEXT, grammarNote TEXT, usageNote TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(conceptId) REFERENCES concepts(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(languageCode) REFERENCES languages(code) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_translations_conceptId_languageCode_normalizedText ON translations(conceptId, languageCode, normalizedText)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_translations_languageCode_normalizedText ON translations(languageCode, normalizedText)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_translations_conceptId ON translations(conceptId)")

            connection.execSQL("CREATE TABLE review_history_v3 (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, conceptId INTEGER NOT NULL, learningStateId INTEGER NOT NULL, sessionId TEXT NOT NULL, reviewStage TEXT NOT NULL, reviewDate INTEGER NOT NULL, isCorrect INTEGER NOT NULL, userAnswer TEXT, correctAnswer TEXT, previousStage TEXT NOT NULL, newStage TEXT NOT NULL, previousDifficulty TEXT NOT NULL, newDifficulty TEXT NOT NULL, difficultyScore INTEGER NOT NULL DEFAULT 0, responseTimeMs INTEGER)")
            connection.execSQL("INSERT INTO review_history_v3 (id, conceptId, learningStateId, sessionId, reviewStage, reviewDate, isCorrect, userAnswer, correctAnswer, previousStage, newStage, previousDifficulty, newDifficulty, difficultyScore, responseTimeMs) SELECT id, conceptId, learningStateId, sessionId, reviewStage, reviewDate, isCorrect, userAnswer, correctAnswer, previousStage, newStage, previousDifficulty, newDifficulty, difficultyScore, responseTimeMs FROM review_history")
            connection.execSQL("DROP TABLE review_history")
            connection.execSQL("ALTER TABLE review_history_v3 RENAME TO review_history")
            connection.execSQL("CREATE INDEX index_review_history_conceptId ON review_history(conceptId)")
            connection.execSQL("CREATE INDEX index_review_history_learningStateId ON review_history(learningStateId)")
            connection.execSQL("CREATE INDEX index_review_history_sessionId ON review_history(sessionId)")
            connection.execSQL("CREATE INDEX index_review_history_reviewDate ON review_history(reviewDate)")
            connection.execSQL("CREATE INDEX index_review_history_conceptId_reviewDate ON review_history(conceptId, reviewDate)")

            // Backfill the first translation from the existing one-per-language content model.
            connection.execSQL("INSERT OR IGNORE INTO translations (conceptId, languageCode, text, normalizedText, pronunciation, definition, example, grammarNote, usageNote, createdAt, updatedAt) SELECT conceptId, languageCode, text, lower(trim(text)), pronunciation, definition, example, grammarNote, usageNote, createdAt, updatedAt FROM contents")
        }
    }

    val V3_TO_V4 = object : Migration(3, 4) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("CREATE TABLE IF NOT EXISTS concept_language_keys (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, conceptId INTEGER NOT NULL, languageCode TEXT NOT NULL, normalizedText TEXT NOT NULL, FOREIGN KEY(conceptId) REFERENCES concepts(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_concept_language_keys_languageCode_normalizedText ON concept_language_keys(languageCode, normalizedText)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_concept_language_keys_conceptId ON concept_language_keys(conceptId)")
        }
    }
    /** Adds language-pair identity to the audit record so difficulty is isolated per learning pair. */
    val V4_TO_V5 = object : Migration(4, 5) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE review_history ADD COLUMN languagePairId INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("UPDATE review_history SET languagePairId = COALESCE((SELECT languagePairId FROM learning_states WHERE learning_states.id = review_history.learningStateId), 0)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_review_history_languagePairId ON review_history(languagePairId)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS index_review_history_languagePairId_reviewDate ON review_history(languagePairId, reviewDate)")
        }
    }

}
