package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ProBlue
import com.example.ui.theme.StatusRed

@Composable
fun MultiSelectionBottomBar(
    selectedCount: Int,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onLockToVault: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowProperties: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Subtle top divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Share
                    SelectionActionButton(
                        icon = Icons.Default.Share,
                        label = "Share",
                        tint = ProBlue,
                        onClick = onShare,
                        tag = "selection_action_share"
                    )

                    // 2. Move
                    SelectionActionButton(
                        icon = Icons.Default.DriveFileMove,
                        label = "Move",
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = onMove,
                        tag = "selection_action_move"
                    )

                    // 3. Copy
                    SelectionActionButton(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy",
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = onCopy,
                        tag = "selection_action_copy"
                    )

                    // 4. Locker (Vault)
                    SelectionActionButton(
                        icon = Icons.Default.Lock,
                        label = "Locker",
                        tint = Color(0xFFF59E0B),
                        onClick = onLockToVault,
                        tag = "selection_action_vault"
                    )

                    // 5. Delete
                    SelectionActionButton(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        tint = StatusRed,
                        onClick = onDelete,
                        tag = "selection_action_delete"
                    )

                    // 6. More Options
                    Box {
                        SelectionActionButton(
                            icon = Icons.Default.MoreVert,
                            label = "More",
                            tint = MaterialTheme.colorScheme.onSurface,
                            onClick = { showMoreMenu = true },
                            tag = "selection_action_more"
                        )

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = StatusRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                text = { Text("Favorite All", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMoreMenu = false
                                    onToggleFavorite()
                                }
                            )

                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = ProBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                text = { Text("Selected Properties", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMoreMenu = false
                                    onShowProperties()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
