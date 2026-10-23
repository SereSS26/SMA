package com.example.demo1.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.demo1.R
import com.example.demo1.TodoViewModel
import com.example.demo1.model.Category
import com.example.demo1.ui.components.AddTaskSheet
import com.example.demo1.ui.screens.SettingsScreen
import com.example.demo1.ui.screens.StatsScreen
import com.example.demo1.ui.screens.TasksScreen
import com.example.demo1.ui.theme.CategoryAll
import com.example.demo1.ui.theme.GradientEnd
import com.example.demo1.ui.theme.GradientStart
import kotlinx.coroutines.launch

enum class Screen(@StringRes val label: Int, val icon: ImageVector) {
    TASKS(R.string.nav_tasks, Icons.Rounded.Checklist),
    STATS(R.string.nav_stats, Icons.Rounded.BarChart),
    SETTINGS(R.string.nav_settings, Icons.Rounded.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(viewModel: TodoViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.TASKS) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val deletedMessage = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.undo)

    fun deleteWithUndo(id: Long) {
        val (index, item) = viewModel.delete(id) ?: return
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(deletedMessage, actionLabel = undoLabel, withDismissAction = true)
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(index, item)
        }
    }

    // Back closes the drawer first, then returns to the Tasks tab before leaving the app.
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    BackHandler(enabled = drawerState.isClosed && screen != Screen.TASKS) { screen = Screen.TASKS }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                viewModel = viewModel,
                currentScreen = screen,
                onSelectCategory = { category ->
                    viewModel.selectedCategory = category
                    screen = Screen.TASKS
                    scope.launch { drawerState.close() }
                },
                onOpenSettings = {
                    screen = Screen.SETTINGS
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        val title = when {
                            screen != Screen.TASKS -> stringResource(screen.label)
                            viewModel.selectedCategory != null -> stringResource(viewModel.selectedCategory!!.label)
                            else -> stringResource(R.string.app_title)
                        }
                        Text(title)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.open_menu))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Screen.entries.forEach { item ->
                        NavigationBarItem(
                            selected = screen == item,
                            onClick = { screen = item },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) }
                        )
                    }
                }
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = screen == Screen.TASKS,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showAddSheet = true },
                        expanded = fabExpanded,
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.new_task)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally { w -> if (forward) w / 4 else -w / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { w -> if (forward) -w / 4 else w / 4 } + fadeOut())
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                label = "screen"
            ) { target ->
                when (target) {
                    Screen.TASKS -> {
                        val inCategory = viewModel.todos.filter {
                            viewModel.selectedCategory == null || it.category == viewModel.selectedCategory
                        }
                        TasksScreen(
                            todos = viewModel.visibleTodos,
                            doneCount = inCategory.count { it.done },
                            totalCount = inCategory.size,
                            filter = viewModel.filter,
                            onFilterChange = { viewModel.filter = it },
                            onToggle = viewModel::toggle,
                            onDelete = ::deleteWithUndo,
                            listState = listState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Screen.STATS -> StatsScreen(viewModel.todos, Modifier.fillMaxSize())

                    Screen.SETTINGS -> SettingsScreen(
                        themeMode = viewModel.themeMode,
                        onThemeModeChange = { viewModel.themeMode = it },
                        hideCompleted = viewModel.hideCompleted,
                        onHideCompletedChange = { viewModel.hideCompleted = it },
                        completedCount = viewModel.todos.count { it.done },
                        onClearCompleted = viewModel::clearCompleted,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddTaskSheet(
            initialCategory = viewModel.selectedCategory ?: Category.PERSONAL,
            onDismiss = { showAddSheet = false },
            onSave = { text, category, priority -> viewModel.add(text, category, priority) }
        )
    }
}

@Composable
private fun AppDrawer(
    viewModel: TodoViewModel,
    currentScreen: Screen,
    onSelectCategory: (Category?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    ModalDrawerSheet {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(MaterialTheme.shapes.large)
                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                .padding(20.dp)
        ) {
            Column {
                Text(stringResource(R.string.app_title), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text(
                    stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Text(
            stringResource(R.string.drawer_lists),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 8.dp)
        )

        val onTasks = currentScreen == Screen.TASKS
        DrawerCategoryItem(
            label = stringResource(R.string.category_all),
            color = CategoryAll,
            count = viewModel.activeCount(null),
            selected = onTasks && viewModel.selectedCategory == null,
            onClick = { onSelectCategory(null) }
        )
        Category.entries.forEach { category ->
            DrawerCategoryItem(
                label = stringResource(category.label),
                color = category.color,
                count = viewModel.activeCount(category),
                selected = onTasks && viewModel.selectedCategory == category,
                onClick = { onSelectCategory(category) }
            )
        }

        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 12.dp))

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nav_settings)) },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
            selected = currentScreen == Screen.SETTINGS,
            onClick = onOpenSettings,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

@Composable
private fun DrawerCategoryItem(label: String, color: Color, count: Int, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        },
        badge = { if (count > 0) Text("$count", style = MaterialTheme.typography.labelLarge) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
