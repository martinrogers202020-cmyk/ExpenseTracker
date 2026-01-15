package com.example.expensetracker.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.puzzleDataStore by preferencesDataStore(name = "daily_puzzle")

private const val DEFAULT_TRIES = 5

data class PuzzlePrefs(
    val dayIndex: Long = -1L,
    val triesLeft: Int = DEFAULT_TRIES,
    val solved: Boolean = false,
    val lastResult: PuzzleResult? = null
)

data class PuzzleResult(
    val value: Int,
    val moves: Int,
    val win: Boolean,
    val usedCells: List<Int>
)

class PuzzleDataStore(private val context: Context) {

    private object Keys {
        val DAY_INDEX = longPreferencesKey("day_index")
        val TRIES_LEFT = intPreferencesKey("tries_left")
        val SOLVED = booleanPreferencesKey("solved")
        val LAST_RESULT_AVAILABLE = booleanPreferencesKey("last_result_available")
        val LAST_RESULT_VALUE = intPreferencesKey("last_result_value")
        val LAST_RESULT_MOVES = intPreferencesKey("last_result_moves")
        val LAST_RESULT_WIN = booleanPreferencesKey("last_result_win")
        val LAST_USED_CELLS = stringPreferencesKey("last_used_cells")
    }

    val prefs: Flow<PuzzlePrefs> = context.puzzleDataStore.data.map { p ->
        val hasResult = p[Keys.LAST_RESULT_AVAILABLE] ?: false
        val lastResult = if (hasResult) {
            PuzzleResult(
                value = p[Keys.LAST_RESULT_VALUE] ?: 0,
                moves = p[Keys.LAST_RESULT_MOVES] ?: 0,
                win = p[Keys.LAST_RESULT_WIN] ?: false,
                usedCells = parseCellList(p[Keys.LAST_USED_CELLS].orEmpty())
            )
        } else {
            null
        }
        PuzzlePrefs(
            dayIndex = p[Keys.DAY_INDEX] ?: -1L,
            triesLeft = p[Keys.TRIES_LEFT] ?: DEFAULT_TRIES,
            solved = p[Keys.SOLVED] ?: false,
            lastResult = lastResult
        )
    }

    suspend fun ensureDay(dayIndex: Long) {
        context.puzzleDataStore.edit { prefs ->
            val storedDay = prefs[Keys.DAY_INDEX] ?: -1L
            if (storedDay != dayIndex) {
                prefs[Keys.DAY_INDEX] = dayIndex
                prefs[Keys.TRIES_LEFT] = DEFAULT_TRIES
                prefs[Keys.SOLVED] = false
                prefs[Keys.LAST_RESULT_AVAILABLE] = false
                prefs[Keys.LAST_RESULT_VALUE] = 0
                prefs[Keys.LAST_RESULT_MOVES] = 0
                prefs[Keys.LAST_RESULT_WIN] = false
                prefs[Keys.LAST_USED_CELLS] = ""
            }
        }
    }

    suspend fun recordAttemptResult(
        dayIndex: Long,
        triesLeft: Int,
        win: Boolean,
        value: Int,
        moves: Int,
        usedCells: List<Int>
    ) {
        val updatedTries = if (win) triesLeft else (triesLeft - 1).coerceAtLeast(0)
        context.puzzleDataStore.edit { prefs ->
            prefs[Keys.DAY_INDEX] = dayIndex
            prefs[Keys.TRIES_LEFT] = updatedTries
            prefs[Keys.SOLVED] = win || (prefs[Keys.SOLVED] ?: false)
            prefs[Keys.LAST_RESULT_AVAILABLE] = true
            prefs[Keys.LAST_RESULT_VALUE] = value
            prefs[Keys.LAST_RESULT_MOVES] = moves
            prefs[Keys.LAST_RESULT_WIN] = win
            prefs[Keys.LAST_USED_CELLS] = usedCells.joinToString(",")
        }
    }

    private fun parseCellList(value: String): List<Int> {
        if (value.isBlank()) return emptyList()
        return value.split(",")
            .mapNotNull { it.toIntOrNull() }
    }
}
