package com.manidigit.flashlearn.domain.vocabulary

/**
 * Keeps duplicate rules out of UI code. A source-language word identifies a
 * concept; target-language meanings are independent records on that concept.
 */
object VocabularyDuplicatePolicy {
    fun normalize(value: String): String = TextNormalizer.normalize(value)

    fun isSameText(first: String, second: String): Boolean =
        normalize(first) == normalize(second)
}
