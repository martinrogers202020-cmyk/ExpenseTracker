package com.example.expensetracker.ui.screens.puzzle

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.R
import com.example.expensetracker.data.datastore.PuzzleResult
import com.example.expensetracker.ui.viewmodel.PuzzleUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPuzzleScreen(
    uiState: PuzzleUiState,
    onStart: () -> Unit,
    onViewResults: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    val background = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF6F1FF),
                Color(0xFFECE6FF),
                Color(0xFFE1DBFF)
            )
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(stringResource(R.string.puzzle_daily_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.puzzle_refresh))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF9FF))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.puzzle_daily_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                R.string.puzzle_daily_target,
                                uiState.target
                            ),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(
                                R.string.puzzle_daily_tries,
                                uiState.triesLeft
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (uiState.solved) {
                            Text(
                                text = stringResource(R.string.puzzle_daily_solved),
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (uiState.triesLeft <= 0) {
                            Text(
                                text = stringResource(R.string.puzzle_daily_out_of_tries),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Button(
                    onClick = onStart,
                    enabled = uiState.triesLeft > 0 && !uiState.solved,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.puzzle_daily_start))
                }

                Button(
                    onClick = onViewResults,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.puzzle_daily_view_results))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.puzzle_daily_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectNumberScreen(
    uiState: PuzzleUiState,
    onSelect: (Int, Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val background = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF5F7FF), Color(0xFFEAF0FF))
        )
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(stringResource(R.string.puzzle_select_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.puzzle_select_status,
                    uiState.movesLeft,
                    uiState.runningValue ?: 0
                ),
                style = MaterialTheme.typography.bodyMedium
            )

            PuzzleGrid(
                grid = uiState.grid,
                usedCells = uiState.usedCells,
                selectedCell = uiState.selectedCell,
                onCellClick = onSelect
            )

            Button(
                onClick = onNext,
                enabled = uiState.selectedCell != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.puzzle_select_next))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyOperationScreen(
    uiState: PuzzleUiState,
    onApply: (PuzzleOperation) -> Unit,
    onBack: () -> Unit
) {
    val hasValue = uiState.runningValue != null
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(stringResource(R.string.puzzle_apply_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.puzzle_apply_running_value),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = uiState.runningValue?.toString() ?: "0",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.puzzle_apply_moves_left, uiState.movesLeft),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            OperationButton(
                label = stringResource(R.string.puzzle_op_add_two),
                enabled = hasValue,
                onClick = { onApply(PuzzleOperation.AddTwo) }
            )
            OperationButton(
                label = stringResource(R.string.puzzle_op_minus_three),
                enabled = hasValue,
                onClick = { onApply(PuzzleOperation.MinusThree) }
            )
            OperationButton(
                label = stringResource(R.string.puzzle_op_double),
                enabled = hasValue,
                onClick = { onApply(PuzzleOperation.Double) }
            )
            OperationButton(
                label = stringResource(R.string.puzzle_op_half),
                enabled = hasValue && uiState.runningValue?.rem(2) == 0,
                onClick = { onApply(PuzzleOperation.Half) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleResultsScreen(
    uiState: PuzzleUiState,
    onShare: (String) -> Unit,
    onPlayAgain: () -> Unit,
    onBack: () -> Unit,
    shareText: String
) {
    val result = uiState.lastResult
    val statusText = if (result?.win == true) {
        stringResource(R.string.puzzle_results_win)
    } else {
        stringResource(R.string.puzzle_results_lose)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(stringResource(R.string.puzzle_results_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.puzzle_results_target, uiState.target),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.puzzle_results_value,
                            result?.value ?: 0,
                            result?.moves ?: 0
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.puzzle_results_tries_left, uiState.triesLeft),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            PuzzleMiniGrid(
                result = result,
                grid = uiState.grid
            )

            Button(
                onClick = { onShare(shareText) },
                modifier = Modifier.fillMaxWidth(),
                enabled = shareText.isNotBlank()
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.puzzle_results_share))
            }

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.triesLeft > 0 && !uiState.solved
            ) {
                Text(stringResource(R.string.puzzle_results_play_again))
            }
        }
    }
}

@Composable
private fun PuzzleGrid(
    grid: List<Int>,
    usedCells: Set<Int>,
    selectedCell: Int?,
    onCellClick: (Int, Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        contentPadding = PaddingValues(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(grid) { index, value ->
            val isUsed = usedCells.contains(index)
            val isSelected = selectedCell == index
            val background = when {
                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                isUsed -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
            val textColor = if (isUsed) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(background)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = !isUsed) { onCellClick(index, value) },
                color = Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = value.toString(),
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun PuzzleMiniGrid(
    result: PuzzleResult?,
    grid: List<Int>
) {
    val usedCells = result?.usedCells?.toSet().orEmpty()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(grid) { index, value ->
                val isUsed = usedCells.contains(index)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.toString(),
                        fontSize = 12.sp,
                        color = if (isUsed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OperationButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}

enum class PuzzleOperation {
    AddTwo,
    MinusThree,
    Double,
    Half
}

fun sharePuzzleResults(context: android.content.Context, shareText: String) {
    if (shareText.isBlank()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.puzzle_share_title)))
}
