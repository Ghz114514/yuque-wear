package com.yuquewatch

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.compose.foundation.background
import com.yuquewatch.data.source.ApiQuota
import com.yuquewatch.ui.AboutScreen
import com.yuquewatch.ui.ApiTestScreen
import com.yuquewatch.ui.BgSpec
import com.yuquewatch.ui.CacheSettingsScreen
import com.yuquewatch.ui.LocalBgSpec
import com.yuquewatch.ui.LocalBleed
import com.yuquewatch.ui.ReadingSettingsScreen
import com.yuquewatch.ui.DebugScreen
import com.yuquewatch.ui.FavoritesScreen
import com.yuquewatch.ui.ImageViewerScreen
import com.yuquewatch.ui.DocsScreen
import com.yuquewatch.ui.EditNoteScreen
import com.yuquewatch.ui.HomeScreen
import com.yuquewatch.ui.NoteDetailScreen
import com.yuquewatch.ui.NotesViewModel
import com.yuquewatch.ui.OnboardingScreen
import com.yuquewatch.ui.SearchScreen
import com.yuquewatch.ui.ReaderTestScreen
import com.yuquewatch.ui.SettingsMenuScreen
import com.yuquewatch.ui.AppearanceSettingsScreen
import com.yuquewatch.ui.DataSettingsScreen
import com.yuquewatch.ui.DebugSettingsScreen
import com.yuquewatch.ui.theme.YuqueTheme
import com.yuquewatch.ui.theme.baseBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Swap the splash window-background for the app theme once we start drawing.
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContent { App() }
    }

    override fun onStop() {
        super.onStop()
        // "退出即清" cache policy.
        runCatching {
            val cfg = com.yuquewatch.data.SettingsStore(this).config.value
            if (cfg.autoClear == com.yuquewatch.data.AutoClear.EXIT) {
                java.io.File(filesDir, "doc_cache").deleteRecursively()
                coil.Coil.imageLoader(this).let { it.diskCache?.clear(); it.memoryCache?.clear() }
            }
        }
    }
}

private const val HOME = "home"
private const val DOCS = "docs"
private const val DETAIL = "detail?ns={ns}&slug={slug}&mini={mini}"
private const val IMAGE = "image?u={u}"
private const val EDIT = "edit"

private fun quickInserts(config: com.yuquewatch.data.AppConfig): List<String> =
    config.quickInserts.split("\n").filter { it.isNotBlank() }
        .ifEmpty { com.yuquewatch.ui.DEFAULT_INSERTS }

private fun detailRoute(ns: String?, slug: String?, mini: String?): String {
    val e = { s: String? -> android.net.Uri.encode(s ?: "") }
    return "detail?ns=${e(ns)}&slug=${e(slug)}&mini=${e(mini)}"
}
private const val SETTINGS = "settings"
private const val SET_APPEARANCE = "set_appearance"
private const val SET_READING = "set_reading"
private const val SET_DATA = "set_data"
private const val SET_CACHE = "set_cache"
private const val SET_DEBUG = "set_debug"
private const val DEBUG = "debug"
private const val ABOUT = "about"
private const val FAVORITES = "favorites"
private const val RECENTS = "recents"
private const val SEARCH = "search"
private const val API_TEST = "apitest"
private const val READER_TEST = "readertest"

@Composable
private fun App() {
    val vm: NotesViewModel = viewModel()
    val config by vm.config.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val debugOn by rememberUpdatedState(config.debugQuota)
    LaunchedEffect(Unit) {
        ApiQuota.events.collect { msg ->
            if (debugOn) Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    YuqueTheme(config) {
        val d = LocalDensity.current
        val spec = BgSpec(
            style = config.backgroundStyle,
            dynamic = config.backgroundDynamic,
            base = baseBackground(config),
            accent = MaterialTheme.colors.primary,
            density = config.backgroundDensity,
            intensity = config.backgroundIntensity,
        )
        CompositionLocalProvider(
            LocalBgSpec provides spec,
            LocalBleed provides config.bleedVertical,
            LocalDensity provides Density(d.density * config.uiScale, d.fontScale),
        ) {
            // Opaque backstop (stays black), with content fading in from the splash.
            var shown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            val fade by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(450),
                label = "fade",
            )
            Box(Modifier.fillMaxSize().background(baseBackground(config))) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = fade }) {
                    if (!config.onboardingDone) {
                        OnboardingScreen(initial = config, onFinish = { vm.saveSettings(it) })
                    } else {
                        MainNav(vm, config)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainNav(vm: NotesViewModel, config: com.yuquewatch.data.AppConfig) {
    val nav = rememberSwipeDismissableNavController()
    LaunchedEffect(Unit) { vm.start() }

    SwipeDismissableNavHost(navController = nav, startDestination = HOME) {
        composable(HOME) {
            val greeting by vm.greeting.collectAsStateWithLifecycle()
            val hitokoto by vm.hitokoto.collectAsStateWithLifecycle()
            val hitokotoLoading by vm.hitokotoLoading.collectAsStateWithLifecycle()
            val repos by vm.repos.collectAsStateWithLifecycle()
            val mini by vm.miniNotes.collectAsStateWithLifecycle()
            val quick by vm.quickNotes.collectAsStateWithLifecycle()
            val miniHasMore by vm.miniHasMore.collectAsStateWithLifecycle()
            val favs by vm.favorites.collectAsStateWithLifecycle()

            val tab by vm.homeTab.collectAsStateWithLifecycle()
            LaunchedEffect(tab) {
                when (tab) {
                    com.yuquewatch.data.HomeTab.QUICK -> vm.loadQuick()
                    com.yuquewatch.data.HomeTab.MINI -> vm.ensureMini()
                    com.yuquewatch.data.HomeTab.MINE -> vm.ensureRepos()
                }
            }
            HomeScreen(
                greeting = greeting,
                greetingFontSp = config.greetingFontSize,
                homeBleed = config.homeBleed,
                tabsIconOnly = config.tabsIconOnly,
                hitokoto = hitokoto,
                hitokotoLoading = hitokotoLoading,
                hitokotoCopy = config.hitokotoCopy,
                noteMode = config.noteMode,
                selectedTab = tab,
                onSelectTab = { vm.setHomeTab(it) },
                avatarUrl = config.avatarUrl,
                showAvatar = config.showAvatar,
                quick = quick,
                onOpenQuick = { n -> nav.navigate(detailRoute(config.quickRepoNamespace, n.id, null)) },
                onNewQuick = { vm.prepareCreate(config.quickRepoNamespace); nav.navigate(EDIT) },
                mini = mini,
                miniHasMore = miniHasMore,
                onOpenMini = { n -> nav.navigate(detailRoute(null, null, n.id)) },
                onNewMini = { vm.prepareCreate(null); nav.navigate(EDIT) },
                onLoadMore = { vm.loadMoreMini() },
                repos = repos,
                onOpenRepo = { r -> vm.openRepo(r); nav.navigate(DOCS) },
                favCount = favs.size,
                onFavorites = { nav.navigate(FAVORITES) },
                onRecents = { nav.navigate(RECENTS) },
                onSearch = { nav.navigate(SEARCH) },
                onSettings = { nav.navigate(SETTINGS) },
                onRetry = {
                    when (tab) {
                        com.yuquewatch.data.HomeTab.QUICK -> vm.loadQuick()
                        com.yuquewatch.data.HomeTab.MINI -> vm.ensureMini(true)
                        com.yuquewatch.data.HomeTab.MINE -> vm.ensureRepos(true)
                    }
                },
            )
        }
        composable(DOCS) {
            val tree by vm.docs.collectAsStateWithLifecycle()
            val repo by vm.currentRepo.collectAsStateWithLifecycle()
            DocsScreen(
                repoName = repo?.name ?: "文档",
                tree = tree,
                onOpen = { slug -> repo?.let { nav.navigate(detailRoute(it.namespace, slug, null)) } },
                onNew = { repo?.let { vm.prepareCreate(it.namespace) }; nav.navigate(EDIT) },
                onRetry = { vm.reloadRepo() },
            )
        }
        composable(
            DETAIL,
            arguments = listOf(
                navArgument("ns") { type = NavType.StringType; defaultValue = "" },
                navArgument("slug") { type = NavType.StringType; defaultValue = "" },
                navArgument("mini") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val ns = entry.arguments?.getString("ns").orEmpty()
            val slug = entry.arguments?.getString("slug").orEmpty()
            val mini = entry.arguments?.getString("mini").orEmpty()
            val nsArg = ns.ifEmpty { null }
            // Per-entry state: navigating back shows the correct doc (not the last opened).
            val state by androidx.compose.runtime.produceState<com.yuquewatch.ui.Resource<com.yuquewatch.data.Note>>(
                com.yuquewatch.ui.Resource.Loading, ns, slug, mini,
            ) {
                value = try {
                    com.yuquewatch.ui.Resource.Content(
                        vm.fetchNote(nsArg, if (mini.isNotEmpty()) mini else slug)
                    )
                } catch (e: Exception) {
                    com.yuquewatch.ui.Resource.Error(e.message ?: "读取失败")
                }
            }
            NoteDetailScreen(
                state = state,
                readingFontSp = config.readingFontSize,
                imageMode = config.imageMode,
                hideTitle = nsArg == null,
                showCopy = config.showCopyButton,
                nativeSelect = config.nativeTextSelection,
                canFav = nsArg != null,
                initialFavorited = nsArg != null && vm.isFavorite(ns, slug),
                onToggleFav = {
                    val title = (state as? com.yuquewatch.ui.Resource.Content)?.data?.title ?: slug
                    vm.toggleFavorite(com.yuquewatch.data.DocRef(ns, slug, title))
                },
                canEdit = nsArg != null,
                onEdit = { nav.navigate("editdoc?ns=${android.net.Uri.encode(ns)}&slug=${android.net.Uri.encode(slug)}") },
                onOpenRef = { ref -> nav.navigate(detailRoute(ref.namespace, ref.slug, null)) },
                onZoom = { url -> nav.navigate("image?u=${android.net.Uri.encode(url)}") },
                onDelete = { id -> vm.deleteEntry(nsArg, id) { nav.popBackStack() } },
            )
        }
        composable(
            IMAGE,
            arguments = listOf(navArgument("u") { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            ImageViewerScreen(
                url = entry.arguments?.getString("u").orEmpty(),
                onExit = { nav.popBackStack() },
            )
        }
        composable(EDIT) {
            EditNoteScreen(
                inserts = quickInserts(config),
                onSave = { text, cb -> vm.create(text, cb) },
                onDone = { nav.popBackStack() },
            )
        }
        composable(
            "editdoc?ns={ns}&slug={slug}",
            arguments = listOf(
                navArgument("ns") { type = NavType.StringType; defaultValue = "" },
                navArgument("slug") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val ns = entry.arguments?.getString("ns").orEmpty()
            val slug = entry.arguments?.getString("slug").orEmpty()
            val body by androidx.compose.runtime.produceState<String?>(null, ns, slug) {
                value = runCatching { vm.docBody(ns, slug) }.getOrElse { "" }
            }
            val b = body
            if (b == null) {
                androidx.wear.compose.material.Scaffold { com.yuquewatch.ui.EditorLoading() }
            } else {
                EditNoteScreen(
                    title = "编辑",
                    initialText = b,
                    inserts = quickInserts(config),
                    onSave = { text, cb -> vm.updateDoc(ns, slug, text, cb) },
                    onDone = { nav.popBackStack() },
                )
            }
        }
        composable(SETTINGS) {
            SettingsMenuScreen(
                onAppearance = { nav.navigate(SET_APPEARANCE) },
                onReading = { nav.navigate(SET_READING) },
                onData = { nav.navigate(SET_DATA) },
                onCache = { nav.navigate(SET_CACHE) },
                onDebug = { nav.navigate(SET_DEBUG) },
                onAbout = { nav.navigate(ABOUT) },
            )
        }
        composable(SET_CACHE) {
            CacheSettingsScreen(
                initial = config,
                onSave = { vm.saveSettings(it) },
                onClearRecents = { vm.clearRecents() },
            )
        }
        composable(SET_APPEARANCE) {
            AppearanceSettingsScreen(initial = config, onSave = { vm.saveSettings(it) })
        }
        composable(SET_READING) {
            ReadingSettingsScreen(initial = config, onSave = { vm.saveSettings(it) })
        }
        composable(SET_DATA) {
            DataSettingsScreen(initial = config, onSave = { vm.saveSettings(it) })
        }
        composable(SET_DEBUG) {
            DebugSettingsScreen(
                initial = config,
                onSave = { vm.saveSettings(it) },
                onApiTest = { nav.navigate(API_TEST) },
                onReaderTest = { nav.navigate(READER_TEST) },
                onOpenRecords = { nav.navigate(DEBUG) },
            )
        }
        composable(DEBUG) {
            val entries by com.yuquewatch.data.source.ResponseLog.entries.collectAsStateWithLifecycle()
            DebugScreen(
                entries = entries,
                recording = config.recordResponses,
                onClear = { com.yuquewatch.data.source.ResponseLog.clear() },
            )
        }
        composable(API_TEST) {
            val r by vm.apiTest.collectAsStateWithLifecycle()
            ApiTestScreen(state = r, onRun = { vm.testApi() })
        }
        composable(READER_TEST) { ReaderTestScreen(readingFontSp = config.readingFontSize) }
        composable(ABOUT) { AboutScreen() }
        composable(FAVORITES) {
            val favs by vm.favorites.collectAsStateWithLifecycle()
            FavoritesScreen(
                favorites = favs,
                onOpen = { ref -> nav.navigate(detailRoute(ref.namespace, ref.slug, null)) },
            )
        }
        composable(RECENTS) {
            val recents by vm.recents.collectAsStateWithLifecycle()
            FavoritesScreen(
                favorites = recents,
                onOpen = { ref -> nav.navigate(detailRoute(ref.namespace, ref.slug, null)) },
                title = "最近",
                emptyHint = "还没有最近打开的文档",
            )
        }
        composable(SEARCH) {
            val results by vm.searchResults.collectAsStateWithLifecycle()
            SearchScreen(
                state = results,
                onSearch = { vm.search(it) },
                onOpen = { ref -> nav.navigate(detailRoute(ref.namespace, ref.slug, null)) },
            )
        }
    }
}
