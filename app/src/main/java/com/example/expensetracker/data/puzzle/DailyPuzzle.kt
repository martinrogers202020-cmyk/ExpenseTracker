package com.example.expensetracker.data.puzzle

import kotlin.random.Random

private const val GRID_SIZE = 36
private const val GRID_MIN = 1
private const val GRID_MAX = 12
private const val TARGET_MIN = 10
private const val TARGET_MAX = 80

data class DailyPuzzle(
    val dayIndex: Long,
    val target: Int,
    val grid: List<Int>
)

object DailyPuzzleGenerator {
    fun generate(dayIndex: Long): DailyPuzzle {
        val random = Random(dayIndex)
        val grid = List(GRID_SIZE) {
            random.nextInt(GRID_MIN, GRID_MAX + 1)
        }
        val target = random.nextInt(TARGET_MIN, TARGET_MAX + 1)
        return DailyPuzzle(dayIndex = dayIndex, target = target, grid = grid)
    }
}
