package com.algorithmx.q_base.ui.explore

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.*
import com.algorithmx.q_base.ui.components.ProfileIconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.data.collections.StudyCollectionWithCount
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.QuestionSet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedExploreScreen(
    viewModel: ExploreViewModel,
    onCollectionClick: (String) -> Unit,
    onSetClick: (String, String) -> Unit,
    onNavigateToUnifiedCreation: () -> Unit,
    onProfileClick: () -> Unit,
    onBack: () -> Unit
) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedSetIds.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var searchQuery by remember { mutableStateOf("") }

    val filteredCollections = remember(collections, searchQuery) {
        if (searchQuery.isBlank()) collections
        else collections.filter { it.collection.name.contains(searchQuery, ignoreCase = true) }
    }

    val filteredSets = remember(sets, searchQuery) {
        if (searchQuery.isBlank()) sets
        else sets.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    androidx.activity.compose.BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            if (isSelectionMode) "${selectedIds.size} Selected" else "Explorer Hub",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (!isSelectionMode) {
                            Text(
                                "Your complete question library",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.deleteSelectedSets() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                        ProfileIconButton(
                            user = currentUser,
                            onClick = onProfileClick
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToUnifiedCreation,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Create New") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search collections or sets...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )
            }

            // Master Collections Section (Horizontal)
            if (filteredCollections.isNotEmpty() && !isSelectionMode) {
                item {
                    SectionHeader(
                        title = "Master Collections",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        itemsIndexed(filteredCollections) { index, item ->
                            MasterCollectionCard(
                                item = item,
                                onClick = { onCollectionClick(item.collection.collectionId) }
                            )
                        }
                    }
                }
            }

            // My Library Section
            item {
                SectionHeader(
                    title = "My Library",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            if (filteredSets.isEmpty()) {
                item {
                    EmptyLibraryView(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp))
                }
            } else {
                itemsIndexed(filteredSets) { index, set ->
                    val isSelected = selectedIds.contains(set.setId)
                    PersonalSetItem(
                        set = set,
                        isSelected = isSelected,
                        selectionMode = isSelectionMode,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.toggleSetSelection(set.setId)
                            } else {
                                onSetClick(set.setId, set.title)
                            }
                        },
                        onLongClick = {
                            viewModel.toggleSetSelection(set.setId)
                        },
                        onDelete = { viewModel.deleteCollectionSet(set.setId) }
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun MasterCollectionCard(
    item: StudyCollectionWithCount,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(120.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp).align(Alignment.CenterStart)
            ) {
                Icon(
                    Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = item.collection.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${item.questionCount} Qs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PersonalSetItem(
    set: QuestionSet,
    isSelected: Boolean,
    selectionMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(set.createdTimestamp) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(set.createdTimestamp))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.CollectionsBookmark,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = set.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Created $dateString",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Layers,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Library is empty",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
