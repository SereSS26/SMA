package com.example.demo1.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.demo1.R
import com.example.demo1.model.Category
import com.example.demo1.model.TodoItem
import com.example.demo1.ui.theme.Demo1Theme
import com.example.demo1.ui.theme.PriorityHigh
import com.example.demo1.ui.theme.PriorityLow

@Composable
fun StatsScreen(todos: List<TodoItem>, modifier: Modifier = Modifier) {
    val total = todos.size
    val done = todos.count { it.done }
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else done.toFloat() / total,
        animationSpec = tween(800),
        label = "statsProgress"
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.stats_overall), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(20.dp))
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(160.dp),
                        strokeWidth = 14.dp,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CountCard(stringResource(R.string.stats_total), total, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            CountCard(stringResource(R.string.stats_active), total - done, PriorityHigh, Modifier.weight(1f))
            CountCard(stringResource(R.string.stats_done), done, PriorityLow, Modifier.weight(1f))
        }

        StatCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.stats_by_category), style = MaterialTheme.typography.titleMedium)
                Category.entries.forEach { category ->
                    val inCategory = todos.filter { it.category == category }
                    CategoryProgress(category, inCategory.count { it.done }, inCategory.size)
                }
            }
        }
    }
}

@Composable
private fun StatCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) { content() }
}

@Composable
private fun CountCard(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineSmall, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryProgress(category: Category, done: Int, total: Int) {
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else done.toFloat() / total,
        animationSpec = tween(800),
        label = "categoryProgress"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(category.color)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(category.label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.stats_category_progress, done, total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = category.color,
            trackColor = category.color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsScreenPreview() {
    Demo1Theme {
        StatsScreen(listOf(TodoItem(1, "A", done = true), TodoItem(2, "B", category = Category.WORK)))
    }
}
