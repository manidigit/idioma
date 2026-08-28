package com.manidigit.flashlearn.database.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "languages")
data class LanguageEntity(
    @PrimaryKey val code: String,
    val displayName: String,
    val nativeName: String,
    val isRtl: Boolean,
    val isActive: Boolean = true,
    val createdAt: Long
)

@Entity(
    tableName = "language_pairs",
    foreignKeys = [
        ForeignKey(entity = LanguageEntity::class, parentColumns = ["code"], childColumns = ["sourceLanguage"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = LanguageEntity::class, parentColumns = ["code"], childColumns = ["targetLanguage"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["sourceLanguage", "targetLanguage"], unique = true)]
)
data class LanguagePairEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceLanguage: String,
    val targetLanguage: String,
    val isActive: Boolean = true,
    val createdAt: Long
)

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCustom: Boolean,
    val createdAt: Long
)

@Entity(tableName = "concepts", indices = [Index(value = ["uuid"], unique = true), Index("categoryId"), Index("active")])
data class ConceptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val contentType: String,
    val categoryId: Long? = null,
    val favorite: Boolean = false,
    val active: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "contents",
    foreignKeys = [
        ForeignKey(entity = ConceptEntity::class, parentColumns = ["id"], childColumns = ["conceptId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = LanguageEntity::class, parentColumns = ["code"], childColumns = ["languageCode"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["conceptId", "languageCode"], unique = true), Index("languageCode"), Index("text")]
)
data class ContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceptId: Long,
    val languageCode: String,
    val text: String,
    val pronunciation: String? = null,
    val definition: String? = null,
    val example: String? = null,
    val grammarNote: String? = null,
    val usageNote: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/** Multiple target-language meanings are first-class records. */
@Entity(
    tableName = "translations",
    foreignKeys = [
        ForeignKey(entity = ConceptEntity::class, parentColumns = ["id"], childColumns = ["conceptId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = LanguageEntity::class, parentColumns = ["code"], childColumns = ["languageCode"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index(value = ["conceptId", "languageCode", "normalizedText"], unique = true),
        Index(value = ["languageCode", "normalizedText"]),
        Index("conceptId")
    ]
)
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceptId: Long,
    val languageCode: String,
    val text: String,
    val normalizedText: String,
    val pronunciation: String? = null,
    val definition: String? = null,
    val example: String? = null,
    val grammarNote: String? = null,
    val usageNote: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "concept_language_keys",
    foreignKeys = [ForeignKey(entity = ConceptEntity::class, parentColumns = ["id"], childColumns = ["conceptId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["languageCode", "normalizedText"], unique = true), Index("conceptId")]
)
data class ConceptLanguageKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceptId: Long,
    val languageCode: String,
    val normalizedText: String
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val createdAt: Long)

@Entity(
    primaryKeys = ["conceptId", "tagId"],
    tableName = "concept_tags",
    foreignKeys = [
        ForeignKey(entity = ConceptEntity::class, parentColumns = ["id"], childColumns = ["conceptId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]
)
data class ConceptTagEntity(val conceptId: Long, val tagId: Long)

@Entity(
    tableName = "learning_states",
    foreignKeys = [
        ForeignKey(entity = ConceptEntity::class, parentColumns = ["id"], childColumns = ["conceptId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = LanguagePairEntity::class, parentColumns = ["id"], childColumns = ["languagePairId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["conceptId", "languagePairId"], unique = true),
        Index(value = ["stage", "nextReviewAt"]),
        Index(value = ["difficulty", "stage"]),
        Index(value = ["languagePairId", "stage", "nextReviewAt"]),
        Index(value = ["languagePairId", "difficulty", "nextReviewAt"])
    ]
)
data class LearningStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceptId: Long,
    val languagePairId: Long,
    val stage: String,
    val difficulty: String,
    val difficultyScore: Int = 0,
    // Legacy aliases retained so v1 databases migrate without data loss.
    val monthlyWrongCount: Int = 0,
    val weeklyWrongCount: Int = 0,
    val dailyWrongCount: Int = 0,
    val nextReviewAt: Long,
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
    val highestStageReached: String = "DAILY",
    val weeklyToDailyReturns: Int = 0,
    val monthlyToDailyReturns: Int = 0,
    val successfulMonthlyCompletions: Int = 0,
    val learnedCount: Int = 0,
    val lastReviewedAt: Long? = null,
    val lastReviewCorrect: Boolean? = null,
    val learnedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "review_sessions", indices = [Index("startedAt"), Index("reviewType")])
data class ReviewSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val reviewType: String,
    val languagePairId: Long,
    val totalCards: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0
)

@Entity(
    tableName = "review_history",
    // Intentionally no foreign keys: this is an immutable audit log and must survive
    // concept/archive/session lifecycle operations. IDs remain traceable references.
    indices = [Index("conceptId"), Index("learningStateId"), Index("languagePairId"), Index("sessionId"), Index("reviewDate"), Index(value = ["conceptId", "reviewDate"]), Index(value = ["languagePairId", "reviewDate"])]
)
data class ReviewHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceptId: Long,
    val learningStateId: Long,
    val languagePairId: Long,
    val sessionId: String,
    val reviewStage: String,
    val reviewDate: Long,
    val isCorrect: Boolean,
    val userAnswer: String? = null,
    val correctAnswer: String? = null,
    val previousStage: String,
    val newStage: String,
    val previousDifficulty: String,
    val newDifficulty: String,
    val difficultyScore: Int = 0,
    val responseTimeMs: Long?
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(@PrimaryKey val key: String, val value: String)
