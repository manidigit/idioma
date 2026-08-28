package com.manidigit.flashlearn.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.manidigit.flashlearn.database.entity.ConceptLanguageKeyEntity
import com.manidigit.flashlearn.database.entity.TranslationEntity

@Dao
interface VocabularyDao {
    /** Returns the existing concept instead of creating a duplicate. */
    @Query("SELECT conceptId FROM concept_language_keys WHERE languageCode = :languageCode AND normalizedText = :normalizedText LIMIT 1")
    suspend fun findConceptId(languageCode: String, normalizedText: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConceptLanguageKey(key: ConceptLanguageKeyEntity): Long

    /** Same normalized meaning cannot be inserted twice for one concept/language. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTranslation(translation: TranslationEntity): Long

    @Query("SELECT * FROM translations WHERE conceptId = :conceptId AND languageCode = :languageCode ORDER BY id ASC")
    suspend fun getTranslations(conceptId: Long, languageCode: String): List<TranslationEntity>
}
