package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrowserSettings
import com.example.data.model.BrowserTab

@Composable
fun NovaToolbar(
    activeTab: BrowserTab?,
    tabCount: Int,
    settings: BrowserSettings,
    isBookmarked: Boolean,
    isRdpActive: Boolean = false,
    activeRdpHost: String? = null,
    omniboxQuery: String,
    isOmniboxFocused: Boolean,
    onOmniboxQueryChange: (String) -> Unit,
    onOmniboxFocusChange: (Boolean) -> Unit,
    onOmniboxSubmit: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onHome: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenTabsGrid: () -> Unit,
    onOpenSecurityInfo: () -> Unit,
    onOpenRdpManager: () -> Unit = {},
    onOpenMariaAi: () -> Unit = {},
    onMenuAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                // Back button
                IconButton(
                    onClick = onBack,
                    enabled = activeTab?.canGoBack == true,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (activeTab?.canGoBack == true)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // Forward button
                IconButton(
                    onClick = onForward,
                    enabled = activeTab?.canGoForward == true,
                    modifier = Modifier.testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (activeTab?.canGoForward == true)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // Reload or Stop
                IconButton(
                    onClick = {
                        if (activeTab?.isLoading == true) onStop() else onReload()
                    },
                    modifier = Modifier.testTag("nav_reload_button")
                ) {
                    Icon(
                        imageVector = if (activeTab?.isLoading == true) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = if (activeTab?.isLoading == true) "Stop loading" else "Reload page",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Optional Home button
                if (settings.showHomeButton) {
                    IconButton(
                        onClick = onHome,
                        modifier = Modifier.testTag("nav_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Omnibox
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = if (isOmniboxFocused) 1.5.dp else 0.5.dp,
                            color = if (isOmniboxFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(21.dp)
                        )
                        .clickable {
                            onOmniboxFocusChange(true)
                            focusRequester.requestFocus()
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isSecure = activeTab?.isSecure == true
                        val isNewTab = activeTab?.isNewTab == true

                        Box(
                            modifier = Modifier
                                .clickable { onOpenSecurityInfo() }
                                .padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isNewTab -> Icons.Default.Public
                                    isSecure -> Icons.Default.Lock
                                    else -> Icons.Outlined.Security
                                },
                                contentDescription = if (isSecure) "Secure connection" else "Site security",
                                tint = when {
                                    isNewTab -> MaterialTheme.colorScheme.primary
                                    isSecure -> Color(0xFF10B981)
                                    else -> Color(0xFFF59E0B)
                                },
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        if (isOmniboxFocused) {
                            BasicTextField(
                                value = omniboxQuery,
                                onValueChange = onOmniboxQueryChange,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        onOmniboxSubmit(omniboxQuery)
                                        focusManager.clearFocus()
                                    }
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { state ->
                                        if (state.isFocused != isOmniboxFocused) {
                                            onOmniboxFocusChange(state.isFocused)
                                        }
                                    }
                                    .testTag("omnibox_input")
                            )

                            if (omniboxQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onOmniboxQueryChange("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear address bar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = if (activeTab?.isNewTab == true)
                                    "Search or enter URL"
                                else
                                    activeTab?.displayHost ?: "Search or enter URL",
                                color = if (activeTab?.isNewTab == true)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!isOmniboxFocused && activeTab?.isNewTab == false) {
                            IconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("bookmark_star_button")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark this tab",
                                    tint = if (isBookmarked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Maria AI Assistant Button
                IconButton(
                    onClick = onOpenMariaAi,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .testTag("maria_ai_toolbar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Maria AI Assistant",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // RDP / Remote Internet Indicator Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRdpActive) Color(0xFF10B981).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            1.dp,
                            if (isRdpActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onOpenRdpManager() }
                        .padding(horizontal = 7.dp)
                        .testTag("rdp_toolbar_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = "RDP Remote Internet",
                            tint = if (isRdpActive) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        if (isRdpActive) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "🇺🇸 USA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Tabs pill / count badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { onOpenTabsGrid() }
                        .testTag("tabs_overview_button")
                ) {
                    Text(
                        text = if (tabCount > 99) ":D" else tabCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 3-dots Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("main_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.width(220.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("New tab") },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("new_tab")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New incognito tab") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("new_incognito_tab")
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Ask Maria AI") },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("maria_ai")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("RDP & Cloud Internet") },
                            leadingIcon = { Icon(Icons.Default.Computer, null, tint = Color(0xFF10B981)) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("rdp_manager")
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Bookmarks") },
                            leadingIcon = { Icon(Icons.Default.Bookmark, null) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("bookmarks")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("History") },
                            leadingIcon = { Icon(Icons.Default.History, null) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("history")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Downloads") },
                            leadingIcon = { Icon(Icons.Default.Download, null) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("downloads")
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(if (activeTab?.isDesktopMode == true) "Mobile site" else "Desktop site") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("toggle_desktop")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Find in page") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("find_in_page")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Zoom") },
                            leadingIcon = { Icon(Icons.Default.ZoomIn, null) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("zoom")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share...") },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("share")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Print...") },
                            leadingIcon = { Icon(Icons.Default.Print, null) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("print")
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = {
                                menuExpanded = false
                                onMenuAction("settings")
                            }
                        )
                    }
                }
            }

            // Page loading progress bar
            if (activeTab?.isLoading == true) {
                LinearProgressIndicator(
                    progress = { (activeTab.progress / 100f).coerceIn(0.05f, 1f) },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                )
            }
        }
    }
}
