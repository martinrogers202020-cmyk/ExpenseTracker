package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.datastore.PuzzleDataStore
import com.example.expensetracker.data.datastore.PuzzlePrefs
import com.example.expensetracker.data.datastore.PuzzleResult
import com.example.expensetracker.data.puzzle.DailyPuzzleGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val MAX_MOVES = 4

private data class AttemptState(
    val runningValue: Int? = null,
    val movesMade: Int = 0,
    val usedCells: Set<Int> = emptySet(),
    val selectedCell: Int? = null
)

data class PuzzleUiState(
    val dayIndex: Long,
    val target: Int,
    val grid: List<Int>,
    val triesLeft: Int,
    val solved: Boolean,
    val lastResult: PuzzleResult?,
    val runningValue: Int?,
    val movesMade: Int,
    val movesLeft: Int,
    val usedCells: Set<Int>,
    val selectedCell: Int?
)

class PuzzleViewModel(
    private val dataStore: PuzzleDataStore
) : ViewModel() {

    private val attemptState = MutableStateFlow(AttemptState())

    private val prefs: StateFlow<PuzzlePrefs> = dataStore.prefs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PuzzlePrefs()
    )

    val uiState: StateFlow<PuzzleUiState> = combine(prefs, attemptState) { prefsValue, attempt ->
        val dayIndex = currentDayIndex()
        val puzzle = DailyPuzzleGenerator.generate(dayIndex)
        PuzzleUiState(
            dayIndex = dayIndex,
            target = puzzle.target,
            grid = puzzle.grid,
            triesLeft = prefsValue.triesLeft,
            solved = prefsValue.solved,
            lastResult = prefsValue.lastResult,
            runningValue = attempt.runningValue,
            movesMade = attempt.movesMade,
            movesLeft = (MAX_MOVES - attempt.movesMade).coerceAtLeast(0),
            usedCells = attempt.usedCells,
            selectedCell = attempt.selectedCell
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PuzzleUiState(
            dayIndex = currentDayIndex(),
            target = 0,
            grid = emptyList(),
            triesLeft = 5,
            solved = false,
            lastResult = null,
            runningValue = null,
            movesMade = 0,
            movesLeft = MAX_MOVES,
            usedCells = emptySet(),
            selectedCell = null
        )
    )

    init {
        refreshDay()
    }

    fun refreshDay() {
        viewModelScope.launch {
            dataStore.ensureDay(currentDayIndex())
            attemptState.value = AttemptState()
        }
    }

    fun resetAttempt() {
        attemptState.value = AttemptState()
    }

    fun selectCell(index: Int, value: Int) {
        val prefsValue = prefs.value
        val attempt = attemptState.value
        if (prefsValue.triesLeft <= 0 || prefsValue.solved) return
        if (attempt.movesMade >= MAX_MOVES) return
        if (attempt.usedCells.contains(index)) return

        val runningValue = if (attempt.movesMade == 0) value else (attempt.runningValue ?: value)
        attemptState.value = attempt.copy(
            runningValue = runningValue,
            selectedCell = index
        )
    }

    fun applyOperation(operation: (Int) -> Int): Boolean {
        val prefsValue = prefs.value
        val attempt = attemptState.value
        val selectedCell = attempt.selectedCell ?: return false
        val runningValue = attempt.runningValue ?: return false
        if (prefsValue.triesLeft <= 0 || prefsValue.solved) return false

        val updatedValue = operation(runningValue)
        val newMoves = attempt.movesMade + 1
        val newUsedCells = attempt.usedCells + selectedCell

        val puzzle = DailyPuzzleGenerator.generate(currentDayIndex())
        val win = updatedValue == puzzle.target
        val isComplete = win || newMoves >= MAX_MOVES

        if (isComplete) {
            viewModelScope.launch {
                dataStore.recordAttemptResult(
                    dayIndex = puzzle.dayIndex,
                    triesLeft = prefsValue.triesLeft,
                    win = win,
                    value = updatedValue,
                    moves = newMoves,
                    usedCells = newUsedCells.sorted()
                )
            }
            attemptState.value = AttemptState()
        } else {
            attemptState.value = AttemptState(
                runningValue = updatedValue,
                movesMade = newMoves,
                usedCells = newUsedCells,
                selectedCell = null
            )
        }
        return isComplete
    }

    fun buildShareText(target: Int, result: PuzzleResult?): String {
        if (result == null) return ""
        val grid = buildShareGrid(result.usedCells)
        val status = if (result.win) "Solved" else "Missed"
        return "Daily Grid\n" +
            "Target: $target\n" +
            "$status in ${result.moves} moves\n" +
            "Result: ${result.value}\n" +
            grid
    }

    private fun buildShareGrid(usedCells: List<Int>): String {
        if (usedCells.isEmpty()) return ""
        val used = usedCells.toSet()
        val builder = StringBuilder()
        for (row in 0 until 6) {
            for (col in 0 until 6) {
                val index = row * 6 + col
                builder.append(if (used.contains(index)) "■" else "□")
            }
            if (row < 5) builder.append('\n')
        }
        return builder.toString()
    }

    private fun currentDayIndex(): Long {
        val start = LocalDate.of(2025, 1, 1)
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(start, today)
    }
}
