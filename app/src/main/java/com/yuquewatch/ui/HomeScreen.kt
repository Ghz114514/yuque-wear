package com.yuquewatch.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import coil.compose.AsyncImage
import com.yuquewatch.data.HomeTab
import com.yuquewatch.data.Note
import com.yuquewatch.data.NoteMode
import com.yuquewatch.data.Repo

@Composable
fun HomeScreen(
    greeting: String,
    greetingFontSp: Float,
    homeBleed: Int,
    tabsIconOnly: Boolean,
    hitokoto: String?,
    hitokotoLoading: Boolean,
    hitokotoCopy: Boolean,
    noteMode: NoteMode,
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    avatarUrl: String,
    showAvatar: Boolean,
    quick: Resource<List<Note>>,
    onOpenQuick: (Note) -> Unit,
    onNewQuick: () -> Unit,
    mini: Resource<List<Note>>,
    miniHasMore: Boolean,
    onOpenMini: (Note) -> Unit,
    onNewMini: () -> Unit,
    onLoadMore: () -> Unit,
    repos: Resource<List<Repo>>,
    onOpenRepo: (Repo) -> Unit,
    favCount: Int,
    onFavorites: () -> Unit,
    onRecents: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    val showQuick = noteMode != NoteMode.MINI
    val showMini = noteMode != NoteMode.QUICK

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScreenBg()
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 10.dp, vertical = homeBleed.coerceAtLeast(4).dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showAvatar && avatarUrl.isNotBlank()) {
                item {
                    AsyncImage(
                        model = avatarUrl, contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape).padding(top = 2.dp),
                    )
                }
            }
            item {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.title3.copy(
                        fontSize = greetingFontSp.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    ),
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
            if (!hitokoto.isNullOrBlank()) {
                item {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    val mod = Modifier.fillMaxWidth().padding(horizontal = 8.dp).let {
                        if (hitokotoCopy) it.then(Modifier.clickable {
                            val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("hitokoto", hitokoto))
                            android.widget.Toast.makeText(ctx, "已复制", android.widget.Toast.LENGTH_SHORT).show()
                        }) else it
                    }
                    Text("「$hitokoto」", style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center, modifier = mod)
                }
            } else if (hitokotoLoading) {
                item { LoadingRow("一言加载中…") }
            }

            // Tab bar (order: 快记 | 小记 | 我的); only enabled tabs shown.
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (showQuick) Tab("快记", AppIcons.tabQuick, tabsIconOnly, selectedTab == HomeTab.QUICK, Modifier.weight(1f)) { onSelectTab(HomeTab.QUICK) }
                    if (showMini) Tab("小记", AppIcons.tabMini, tabsIconOnly, selectedTab == HomeTab.MINI, Modifier.weight(1f)) { onSelectTab(HomeTab.MINI) }
                    Tab("我的", AppIcons.tabMine, tabsIconOnly, selectedTab == HomeTab.MINE, Modifier.weight(1f)) { onSelectTab(HomeTab.MINE) }
                }
            }

            when (selectedTab) {
                HomeTab.QUICK -> {
                    item { NewButton("＋ 新建快记", onNewQuick) }
                    notesSection(quick, onOpenQuick, onRetry, emptyHint = "快记知识库暂无内容")
                }
                HomeTab.MINI -> {
                    item { NewButton("＋ 新建小记", onNewMini) }
                    notesSection(mini, onOpenMini, onRetry, emptyHint = "还没有小记")
                    if (mini is Resource.Content && miniHasMore) {
                        item {
                            CompactChip(onClick = onLoadMore, label = { Text("加载更多") },
                                colors = ChipDefaults.secondaryChipColors(), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                HomeTab.MINE -> mineSection(repos, favCount, onOpenRepo, onFavorites, onRecents, onSearch, onSettings, onRetry)
            }
        }
    }
}

private fun ScalingLazyListScope.notesSection(
    state: Resource<List<Note>>,
    onOpen: (Note) -> Unit,
    onRetry: () -> Unit,
    emptyHint: String,
) {
    when (state) {
        is Resource.Loading -> item { LoadingRow() }
        is Resource.Error -> item { ErrorChip(state.message, onRetry) }
        is Resource.Content -> {
            if (state.data.isEmpty()) item { CenterHint(emptyHint) }
            else items(state.data, key = { it.id }) { n ->
                Chip(
                    onClick = { onOpen(n) },
                    label = { Text(n.title, maxLines = 2) },
                    secondaryLabel = if (n.updatedAt.isNotBlank()) { { Text(n.updatedAt) } } else null,
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun ScalingLazyListScope.mineSection(
    repos: Resource<List<Repo>>,
    favCount: Int,
    onOpenRepo: (Repo) -> Unit,
    onFavorites: () -> Unit,
    onRecents: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    item {
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            RoundIconButton(AppIcons.favorites, "收藏", onFavorites)
            RoundIconButton(AppIcons.recent, "最近", onRecents)
            RoundIconButton(AppIcons.search, "搜索", onSearch)
            RoundIconButton(AppIcons.settings, "设置", onSettings)
        }
    }
    when (repos) {
        is Resource.Loading -> item { LoadingRow("加载知识库…") }
        is Resource.Error -> item { ErrorChip(repos.message, onRetry) }
        is Resource.Content -> {
            if (repos.data.isEmpty()) item { CenterHint("没有知识库") }
            else items(repos.data, key = { it.id }) { r ->
                Chip(
                    onClick = { onOpenRepo(r) },
                    label = { Text(r.name, maxLines = 2) },
                    secondaryLabel = { Text(r.namespace) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.secondaryButtonColors(),
        modifier = Modifier.size(40.dp),
    ) { Icon(icon, contentDescription = desc, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun NewButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.primaryButtonColors(),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.title3.copy(
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    }
}

@Composable
private fun Tab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconOnly: Boolean,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = if (selected) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors()
    CompactChip(
        onClick = onClick,
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(icon, null, Modifier.size(16.dp))
                if (!iconOnly) {
                    androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                    Text(label, style = MaterialTheme.typography.caption2)
                }
            }
        },
        colors = colors, modifier = modifier,
    )
}
