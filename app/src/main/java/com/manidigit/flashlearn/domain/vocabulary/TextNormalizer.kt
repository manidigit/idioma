package com.manidigit.flashlearn.domain.vocabulary

import java.text.Normalizer

/** Canonical form used only for duplicate detection; original text is preserved. */
object TextNormalizer {
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()
        .replace('ي', 'ی')
        .replace('ى', 'ی')
        .replace('ك', 'ک')
        .replace('ۀ', 'ه')
}
