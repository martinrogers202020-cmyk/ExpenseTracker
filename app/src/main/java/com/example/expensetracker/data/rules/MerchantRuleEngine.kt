package com.example.expensetracker.data.rules

import com.example.expensetracker.data.db.MerchantRuleDao
import com.example.expensetracker.data.model.MerchantRuleEntity
import java.util.Locale

class MerchantRuleEngine(
    private val dao: MerchantRuleDao
) {
    // Load rules once (fast enough). You can cache later.
    suspend fun resolveCategory(description: String): Long? {
        val text = description.lowercase(Locale.ROOT)
        val rules = dao.getAllOnce().filter { it.enabled }

        // already sorted in query, but keep safe:
        val sorted = rules.sortedWith(
            compareByDescending<MerchantRuleEntity> { it.priority }.thenByDescending { it.id }
        )

        for (r in sorted) {
            if (matches(r, text)) return r.categoryId
        }
        return null
    }

    private fun matches(rule: MerchantRuleEntity, descriptionLower: String): Boolean {
        val p = rule.pattern.lowercase(Locale.ROOT).trim()
        if (p.isBlank()) return false

        return when (rule.matchType.uppercase(Locale.ROOT)) {
            "STARTS_WITH" -> descriptionLower.startsWith(p)
            "REGEX" -> runCatching { Regex(p, RegexOption.IGNORE_CASE).containsMatchIn(descriptionLower) }
                .getOrDefault(false)
            else -> descriptionLower.contains(p) // CONTAINS default
        }
    }
}
