package com.example.demo1.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.demo1.R
import com.example.demo1.model.TaskFilter
import com.example.demo1.model.TodoItem
import com.example.demo1.ui.components.SwipeableTaskCard
import com.example.demo1.ui.theme.Demo1Theme
import com.example.demo1.ui.theme.GradientEnd
import com.example.demo1.ui.theme.GradientStart

@Composable
fun TasksScreen(
    todos: List<TodoItem>,
    doneCount: Int,
    totalCount: Int,
    filter: TaskFilter,
    onFilterChange: (TaskFilter) -> Unit,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        // Extra bottom padding keeps the last card clear of the FAB.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "header") {
            ProgressHeader(doneCount = doneCount, totalCount = totalCount)
        }
        item(key = "filters") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                TaskFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { onFilterChange(option) },
                        label = { Text(stringResource(option.label)) },
                        shape = CircleShape
                    )
                }
            }
        }
        if (todos.isEmpty()) {
            item(key = "empty") { EmptyState(Modifier.animateItem()) }
        } else {
            items(todos, key = { it.id }) { item ->
                SwipeableTaskCard(
                    item = item,
                    onToggle = { onToggle(item.id) },
                    onDelete = { onDelete(item.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun ProgressHeader(doneCount: Int, totalCount: Int) {
    val progress by animateFloatAsState(
        targetValue = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount,
        label = "headerProgress"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.greeting),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Text(
                stringResource(R.string.progress_summary, doneCount, totalCount),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(8.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {}
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.TaskAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        }
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TasksScreenPreview() {
    Demo1Theme {
        TasksScreen(
            todos = listOf(TodoItem(1, "Trimite raportul"), TodoItem(2, "Mergi la sală", done = true)),
            doneCount = 1,
            totalCount = 2,
            filter = TaskFilter.ALL,
            onFilterChange = {},
            onToggle = {},
            onDelete = {}
        )
    }
}
