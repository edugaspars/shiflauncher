package io.github.edugaspars.shiflauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.edugaspars.shiflauncher.R
import io.github.edugaspars.shiflauncher.data.AppEntry
import io.github.edugaspars.shiflauncher.data.LauncherSettings
import io.github.edugaspars.shiflauncher.data.indexLetter
import io.github.edugaspars.shiflauncher.data.normalized
import io.github.edugaspars.shiflauncher.notifications.AppNotifications
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    hiddenApps: List<AppEntry>,
    notificationsFor: (AppEntry) -> AppNotifications?,
    settings: LauncherSettings,
    focusSearch: Boolean,
    actions: AppActions,
    onRename: (AppEntry) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var query by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var bubble by remember { mutableStateOf<Char?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    LaunchedEffect(focusSearch) { if (focusSearch) runCatching { focus.requestFocus() } }

    val q = query.trim().normalized()
    val shown = remember(apps, hiddenApps, q) {
        if (q.isEmpty()) apps
        else (apps + hiddenApps)
            .filter { it.label.normalized().contains(q) }
            .sortedBy { if (it.label.normalized().startsWith(q)) 0 else 1 }
    }
    val letters = remember(apps) { apps.map { it.label.indexLetter() }.distinct() }

    fun launchAndClose(app: AppEntry) {
        keyboard?.hide()
        actions.launch(app)
    }

    // Puxar a lista para baixo quando ela já está no topo fecha a gaveta.
    val close by rememberUpdatedState(onClose)
    val closeThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    val pullToClose = remember {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0) {
                    pulled += available.y
                    if (pulled > closeThreshold) { pulled = 0f; close() }
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }

    val background = if (settings.blackBackground) Color.Black else Color.Black.copy(alpha = 0.82f)
    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.White.copy(alpha = 0.12f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { shown.firstOrNull()?.let(::launchAndClose) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .focusRequester(focus),
            )
            Row(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 28.dp, end = 8.dp, bottom = 24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .nestedScroll(pullToClose),
                ) {
                    if (shown.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_results),
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                    items(shown, key = { it.key }) { app ->
                        AppRow(
                            app = app,
                            notifications = if (settings.showNotifications) notificationsFor(app) else null,
                            showIcon = settings.showIcons,
                            large = false,
                            shadow = false,
                            menuOpen = menuFor == app.key,
                            onClick = { launchAndClose(app) },
                            onLongClick = { menuFor = app.key },
                            menu = {
                                AppMenu(
                                    app = app,
                                    isFavorite = app.key in settings.favorites,
                                    isHidden = app.key in settings.hidden,
                                    actions = actions,
                                    onRename = { onRename(app) },
                                    onDismiss = { menuFor = null },
                                )
                            },
                        )
                    }
                }
                if (q.isEmpty() && letters.size > 1) {
                    AlphabetScroller(
                        letters = letters,
                        onLetter = { letter ->
                            bubble = letter
                            val index = shown.indexOfFirst { it.label.indexLetter() == letter }
                            if (index >= 0) scope.launch { listState.scrollToItem(index) }
                        },
                        onRelease = { bubble = null },
                        modifier = Modifier
                            .width(36.dp)
                            .fillMaxHeight()
                            .padding(vertical = 16.dp),
                    )
                }
            }
        }
        bubble?.let {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            ) {
                Text(
                    it.toString(),
                    fontSize = 44.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/**
 * Índice alfabético lateral com efeito "onda": as letras próximas do dedo
 * crescem e se afastam da borda, como no Niagara.
 */
@Composable
fun AlphabetScroller(
    letters: List<Char>,
    onLetter: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val waveShift = with(density) { 56.dp.toPx() }
    var height by remember { mutableIntStateOf(1) }
    var touchY by remember { mutableFloatStateOf(-1f) }
    var current by remember { mutableStateOf<Char?>(null) }
    val pick by rememberUpdatedState(onLetter)
    val release by rememberUpdatedState(onRelease)

    fun select(y: Float) {
        touchY = y
        val index = ((y / height) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        val letter = letters[index]
        if (letter != current) {
            current = letter
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pick(letter)
        }
    }

    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .onSizeChanged { height = it.height.coerceAtLeast(1) }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    select(down.position.y)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        select(change.position.y)
                    }
                    touchY = -1f
                    current = null
                    release()
                }
            },
    ) {
        letters.forEachIndexed { i, letter ->
            val center = (i + 0.5f) / letters.size * height
            val influence = if (touchY < 0) 0f
            else (1f - abs(touchY - center) / (height * 0.12f)).coerceIn(0f, 1f)
            Text(
                text = letter.toString(),
                fontSize = 12.sp,
                fontWeight = if (letter == current) FontWeight.Bold else FontWeight.Normal,
                color = if (letter == current) MaterialTheme.colorScheme.primary
                else Color.White.copy(alpha = 0.55f + 0.45f * influence),
                modifier = Modifier.graphicsLayer {
                    translationX = -waveShift * influence
                    scaleX = 1f + influence
                    scaleY = 1f + influence
                },
            )
        }
    }
}
