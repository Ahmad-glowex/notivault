package com.notivault.app.ui.screens.media

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notivault.app.ui.screens.media.components.MediaGridItem
import com.notivault.app.ui.screens.media.components.MediaViewerModal
import com.notivault.app.ui.theme.DarkBackground
import com.notivault.app.ui.theme.DarkSurface
import com.notivault.app.ui.theme.TealSecondary
import com.notivault.app.ui.theme.TextPrimaryDark
import com.notivault.app.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(
    onNavigateBack: () -> Unit,
    viewModel: MediaViewModel = viewModel()
) {
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val mediaList by viewModel.mediaList.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Saved Media Vault",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${mediaList.size} cached attachments",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    if (mediaList.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllMedia() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Media",
                                tint = TextSecondaryDark
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter categories
            val filters = listOf(
                Pair("ALL", "All Media"),
                Pair("IMAGE", "Photos"),
                Pair("VIDEO", "Videos"),
                Pair("AUDIO", "Audio / Voice")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { (type, label) ->
                    val isSelected = selectedFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectFilter(type) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealSecondary,
                            selectedLabelColor = Color.Black,
                            containerColor = DarkSurface,
                            labelColor = Color.White
                        )
                    )
                }
            }

            if (mediaList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PermMedia,
                            contentDescription = null,
                            tint = TealSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Cached Media",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "When media attachments are downloaded by WhatsApp, Telegram, or Messenger, NotiVault's Media Observer caches a copy here before they can be revoked.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = mediaList,
                        key = { it.id }
                    ) { mediaItem ->
                        MediaGridItem(
                            media = mediaItem,
                            onClick = { viewModel.openMediaModal(mediaItem) }
                        )
                    }
                }
            }

            // Modal viewer
            selectedMedia?.let { mediaItem ->
                MediaViewerModal(
                    media = mediaItem,
                    onDismiss = { viewModel.closeMediaModal() },
                    onDelete = { id -> viewModel.deleteMedia(id) }
                )
            }
        }
    }
}
