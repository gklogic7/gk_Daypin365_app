package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.local.OverlayAnimationStyle
import com.example.data.local.OverlayPanelStyle

private val PopOvershootEasing = CubicBezierEasing(0.22f, 1.28f, 0.36f, 1f)
private val ZoomBounceEasing = CubicBezierEasing(0.18f, 1.42f, 0.38f, 1f)

const val OVERLAY_EXIT_DURATION_MS = 280L

fun overlayEnterTransition(style: OverlayAnimationStyle): EnterTransition {
    return when (style) {
        OverlayAnimationStyle.FADE ->
            fadeIn(animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing))

        OverlayAnimationStyle.POPUP ->
            fadeIn(animationSpec = tween(240)) +
                scaleIn(
                    initialScale = 0.72f,
                    animationSpec = tween(durationMillis = 300, easing = PopOvershootEasing)
                )

        OverlayAnimationStyle.SLIDE_TOP ->
            fadeIn(animationSpec = tween(220)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                ) { fullHeight -> -fullHeight / 2 }

        OverlayAnimationStyle.SLIDE_BOTTOM ->
            fadeIn(animationSpec = tween(220)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                ) { fullHeight -> fullHeight / 2 }

        OverlayAnimationStyle.SLIDE_SIDE ->
            fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                ) { fullWidth -> -fullWidth / 2 }

        OverlayAnimationStyle.EXPAND_UNFOLD ->
            fadeIn(animationSpec = tween(220)) +
                expandVertically(
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top
                )

        OverlayAnimationStyle.ZOOM_BOUNCE ->
            fadeIn(animationSpec = tween(200)) +
                scaleIn(
                    initialScale = 0.55f,
                    animationSpec = tween(durationMillis = 360, easing = ZoomBounceEasing)
                )
    }
}

fun overlayExitTransition(style: OverlayAnimationStyle): ExitTransition {
    return when (style) {
        OverlayAnimationStyle.FADE ->
            fadeOut(animationSpec = tween(durationMillis = 270, easing = FastOutSlowInEasing))

        OverlayAnimationStyle.POPUP ->
            fadeOut(animationSpec = tween(230)) +
                scaleOut(
                    targetScale = 0.76f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )

        OverlayAnimationStyle.SLIDE_TOP ->
            fadeOut(animationSpec = tween(220)) +
                slideOutVertically(
                    animationSpec = tween(durationMillis = 270, easing = FastOutSlowInEasing)
                ) { fullHeight -> -fullHeight / 2 }

        OverlayAnimationStyle.SLIDE_BOTTOM ->
            fadeOut(animationSpec = tween(220)) +
                slideOutVertically(
                    animationSpec = tween(durationMillis = 270, easing = FastOutSlowInEasing)
                ) { fullHeight -> fullHeight / 2 }

        OverlayAnimationStyle.SLIDE_SIDE ->
            fadeOut(animationSpec = tween(220)) +
                slideOutHorizontally(
                    animationSpec = tween(durationMillis = 270, easing = FastOutSlowInEasing)
                ) { fullWidth -> fullWidth / 2 }

        OverlayAnimationStyle.EXPAND_UNFOLD ->
            fadeOut(animationSpec = tween(220)) +
                shrinkVertically(
                    animationSpec = tween(durationMillis = 270, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top
                )

        OverlayAnimationStyle.ZOOM_BOUNCE ->
            fadeOut(animationSpec = tween(210)) +
                scaleOut(
                    targetScale = 0.58f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                )
    }
}

private fun TextStyle.scaledBy(scale: Float): TextStyle {
    if (kotlin.math.abs(scale - 1f) < 0.01f) return this
    val nextSize = if (fontSize != TextUnit.Unspecified) fontSize * scale else fontSize
    val nextLine = if (lineHeight != TextUnit.Unspecified) lineHeight * scale else lineHeight
    return copy(fontSize = nextSize, lineHeight = nextLine)
}

fun scaleOverlayTypography(base: Typography, textScale: Float): Typography {
    val safeScale = textScale.coerceIn(0.80f, 1.35f)
    if (kotlin.math.abs(safeScale - 1f) < 0.01f) return base
    return base.copy(
        headlineLarge = base.headlineLarge.scaledBy(safeScale),
        headlineMedium = base.headlineMedium.scaledBy(safeScale),
        titleLarge = base.titleLarge.scaledBy(safeScale),
        titleMedium = base.titleMedium.scaledBy(safeScale),
        titleSmall = base.titleSmall.scaledBy(safeScale),
        bodyLarge = base.bodyLarge.scaledBy(safeScale),
        bodyMedium = base.bodyMedium.scaledBy(safeScale),
        bodySmall = base.bodySmall.scaledBy(safeScale),
        labelLarge = base.labelLarge.scaledBy(safeScale),
        labelMedium = base.labelMedium.scaledBy(safeScale),
        labelSmall = base.labelSmall.scaledBy(safeScale)
    )
}

@Composable
fun OverlayPanelShell(
    panelStyle: OverlayPanelStyle,
    baseSurfaceColor: Color,
    accentBorderColor: Color,
    isFullMode: Boolean,
    minWidth: Dp = 200.dp,
    maxWidth: Dp = 300.dp,
    targetWidth: Dp? = null,
    maxMediaHeightDp: Dp? = null,
    panelScale: Float = 1.0f,
    mediaHeightScale: Float = 1.0f,
    textScale: Float = 1.0f,
    spotlightHeaderLabel: String = "ACTIVE PANEL",
    mediaImageUri: String? = null,
    mediaVideoUri: String? = null,
    hasAudioMedia: Boolean = false,
    useAlarmAudio: Boolean = false,
    stretchMediaToFill: Boolean = false,
    onMediaClick: (() -> Unit)? = null,
    cornerRadiusOverrideDp: Int? = null,
    surfaceAlphaOverride: Float? = null,
    topBarContent: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val safePanelScale = panelScale.coerceIn(0.50f, 2.20f)
    val safeMediaHeightScale = mediaHeightScale.coerceIn(0.50f, 2.40f)
    val basePad = when {
        panelStyle.isSmallVariant -> if (isFullMode) 11.dp else 8.dp
        isFullMode -> 15.dp
        else -> 10.dp
    }
    val contentPad = (basePad.value * safePanelScale.coerceIn(0.76f, 1.32f)).dp
    val vSpacing = ((if (isFullMode) 8f else 5f) * safeMediaHeightScale.coerceIn(0.75f, 1.45f)).dp

    val effectiveMinWidth = if (panelStyle.isSmallVariant) {
        minOf(minWidth, 148.dp)
    } else {
        minWidth
    }
    val scaledMinWidth = (effectiveMinWidth.value * safePanelScale.coerceIn(0.50f, 1.0f)).dp
    val scaledMaxWidth = when (panelStyle) {
        OverlayPanelStyle.TICKER_STRIP -> maxOf(maxWidth, 440.dp)
        OverlayPanelStyle.SPLIT_MEDIA,
        OverlayPanelStyle.MEDIA_EDITORIAL -> maxOf(maxWidth, if (isFullMode) 560.dp else 360.dp)
        OverlayPanelStyle.DYNAMIC_PILL,
        OverlayPanelStyle.MEDIA_STRIP -> maxOf(maxWidth, if (isFullMode) 390.dp else 290.dp)
        OverlayPanelStyle.FROSTED_MINI,
        OverlayPanelStyle.TAPE_NOTE,
        OverlayPanelStyle.POCKET_TILE -> maxOf(maxWidth, if (isFullMode) 350.dp else 260.dp)
        else -> maxWidth
    }.let { (it.value * safePanelScale).dp }

    fun clampMediaH(rawDp: Float, minFloorDp: Dp = 76.dp): Dp {
        val candidate = rawDp.dp
        val capped = if (maxMediaHeightDp != null) minOf(candidate, maxMediaHeightDp) else candidate
        return capped.coerceAtLeast(minFloorDp)
    }

    val cardMinHeight = if (safeMediaHeightScale > 1.04f) {
        clampMediaH(
            rawDp = (if (panelStyle.isSmallVariant) 74f else if (isFullMode) 136f else 102f) * safeMediaHeightScale,
            minFloorDp = 56.dp
        )
    } else {
        0.dp
    }

    fun widthBoundModifier(minBound: Dp = scaledMinWidth): Modifier {
        val wMod = if (targetWidth != null) {
            modifier.width(targetWidth.coerceAtLeast(140.dp))
        } else {
            modifier.widthIn(min = minBound, max = maxOf(minBound, scaledMaxWidth))
        }
        return if (cardMinHeight > 0.dp) wMod.heightIn(min = cardMinHeight) else wMod
    }

    fun r(defaultDp: Int): Dp = (cornerRadiusOverrideDp ?: defaultDp).coerceIn(8, 36).dp
    fun a(defaultAlpha: Float): Float = surfaceAlphaOverride?.coerceIn(0.65f, 1.0f) ?: defaultAlpha

    val hairline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
    val scaledTypography = scaleOverlayTypography(MaterialTheme.typography, textScale.coerceIn(0.80f, 1.45f))

    MaterialTheme(typography = scaledTypography) {
        when (panelStyle) {
            OverlayPanelStyle.CLASSIC_CARD -> {
                val shape = RoundedCornerShape(r(if (isFullMode) 24 else 20))
                Surface(
                    color = baseSurfaceColor.copy(alpha = a(if (isFullMode) 1f else 0.88f)),
                    shape = shape,
                    shadowElevation = if (isFullMode) 12.dp else 5.dp,
                    modifier = widthBoundModifier()
                        .border(
                            width = 1.dp,
                            color = accentBorderColor.copy(alpha = if (isFullMode) 0.85f else 0.65f),
                            shape = shape
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(contentPad),
                        verticalArrangement = Arrangement.spacedBy(vSpacing),
                        content = content
                    )
                }
            }

            OverlayPanelStyle.GLASS_PILL -> {
                val shape = RoundedCornerShape(r(30))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = a(if (isFullMode) 0.82f else 0.74f)),
                    shape = shape,
                    shadowElevation = 10.dp,
                    modifier = widthBoundModifier()
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.42f),
                            shape = shape
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.14f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(horizontal = contentPad + 4.dp, vertical = contentPad),
                        verticalArrangement = Arrangement.spacedBy(vSpacing),
                        content = content
                    )
                }
            }

            OverlayPanelStyle.TICKER_STRIP -> {
                val shape = RoundedCornerShape(r(16))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = a(if (isFullMode) 0.95f else 0.88f)),
                    shape = shape,
                    shadowElevation = 6.dp,
                    modifier = widthBoundModifier(minBound = maxOf(scaledMinWidth, 210.dp))
                        .border(0.8.dp, hairline, shape)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier.padding(horizontal = contentPad, vertical = (contentPad.value * 0.82f).dp),
                            verticalArrangement = Arrangement.spacedBy(vSpacing),
                            content = content
                        )
                    }
                }
            }

            OverlayPanelStyle.ACTION_SPOTLIGHT -> {
                val shape = RoundedCornerShape(r(22))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = a(if (isFullMode) 0.96f else 0.88f)),
                    shape = shape,
                    shadowElevation = 10.dp,
                    modifier = widthBoundModifier()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.50f), shape)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f))
                                .padding(horizontal = contentPad, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = spotlightHeaderLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "SPOTLIGHT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(
                            modifier = Modifier.padding(contentPad),
                            verticalArrangement = Arrangement.spacedBy(vSpacing),
                            content = content
                        )
                    }
                }
            }

            OverlayPanelStyle.CORNER_TAB -> {
                val shape = RoundedCornerShape(
                    topStart = 6.dp,
                    topEnd = r(22),
                    bottomEnd = r(22),
                    bottomStart = r(18)
                )
                Surface(
                    color = baseSurfaceColor.copy(alpha = a(if (isFullMode) 1f else 0.88f)),
                    shape = shape,
                    shadowElevation = 8.dp,
                    modifier = widthBoundModifier()
                        .clip(shape)
                        .border(0.8.dp, accentBorderColor.copy(alpha = 0.72f), shape)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(contentPad),
                            verticalArrangement = Arrangement.spacedBy(vSpacing),
                            content = content
                        )
                    }
                }
            }

            OverlayPanelStyle.SPLIT_MEDIA -> {
                val shape = RoundedCornerShape(r(if (isFullMode) 24 else 20))
                val splitMediaHeight = clampMediaH(
                    rawDp = (if (isFullMode) 168f else 124f) * safeMediaHeightScale,
                    minFloorDp = 88.dp
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = a(if (isFullMode) 0.96f else 0.88f)),
                    shape = shape,
                    shadowElevation = if (isFullMode) 12.dp else 6.dp,
                    modifier = widthBoundModifier(minBound = maxOf(scaledMinWidth, 232.dp))
                        .border(0.9.dp, accentBorderColor.copy(alpha = 0.45f), shape)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(if (isFullMode) 12.dp else 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isFullMode) 12.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OverlayVisualMediaTile(
                            imageUri = mediaImageUri,
                            videoUri = mediaVideoUri,
                            hasAudioMedia = hasAudioMedia,
                            accentColor = accentBorderColor,
                            useAlarmAudio = useAlarmAudio,
                            stretchToFill = stretchMediaToFill,
                            onMediaClick = onMediaClick,
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                                .heightIn(min = splitMediaHeight)
                                .clip(RoundedCornerShape(if (isFullMode) 18.dp else 14.dp))
                        )

                        Column(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(vSpacing),
                            content = content
                        )
                    }
                }
            }

            OverlayPanelStyle.FULL_MEDIA_GRADIENT -> {
                val shape = RoundedCornerShape(r(if (isFullMode) 26 else 22))
                val gradientCardHeight = clampMediaH(
                    rawDp = (if (isFullMode) 246f else 182f) * safeMediaHeightScale,
                    minFloorDp = 128.dp
                )
                val overlayScheme = MaterialTheme.colorScheme.copy(
                    onSurface = Color.White,
                    onSurfaceVariant = Color.White.copy(alpha = 0.82f),
                    surface = Color.Black.copy(alpha = 0.48f),
                    surfaceContainerHigh = Color.Black.copy(alpha = 0.46f),
                    surfaceContainerLow = Color.Black.copy(alpha = 0.44f),
                    secondaryContainer = Color.White.copy(alpha = 0.18f),
                    onSecondaryContainer = Color.White,
                    outlineVariant = Color.White.copy(alpha = 0.30f)
                )

                Surface(
                    color = Color.Black,
                    shape = shape,
                    shadowElevation = if (isFullMode) 14.dp else 8.dp,
                    modifier = widthBoundModifier()
                        .border(0.9.dp, Color.White.copy(alpha = 0.28f), shape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = gradientCardHeight)
                    ) {
                        OverlayVisualMediaTile(
                            imageUri = mediaImageUri,
                            videoUri = mediaVideoUri,
                            hasAudioMedia = hasAudioMedia,
                            accentColor = accentBorderColor,
                            useAlarmAudio = useAlarmAudio,
                            stretchToFill = stretchMediaToFill,
                            onMediaClick = onMediaClick,
                            modifier = Modifier.matchParentSize()
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to Color.Black.copy(alpha = 0.10f),
                                        0.36f to Color.Black.copy(alpha = 0.44f),
                                        0.70f to Color.Black.copy(alpha = 0.84f),
                                        1.0f to Color.Black.copy(alpha = 0.95f)
                                    )
                                )
                        )

                        MaterialTheme(colorScheme = overlayScheme, typography = scaledTypography) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomStart)
                                    .padding(
                                        start = contentPad,
                                        end = contentPad,
                                        top = (36f * safeMediaHeightScale.coerceIn(0.70f, 1.50f)).dp,
                                        bottom = contentPad
                                    ),
                                verticalArrangement = Arrangement.spacedBy(vSpacing),
                                content = content
                            )
                        }
                    }
                }
            }

            OverlayPanelStyle.PLAIN_MEDIA -> {
                val shape = RoundedCornerShape(r(if (isFullMode) 26 else 22))
                val mediaHeight = clampMediaH(
                    rawDp = (if (isFullMode) 204f else 156f) * safeMediaHeightScale,
                    minFloorDp = 84.dp
                )
                val darkFooterScheme = MaterialTheme.colorScheme.copy(
                    onSurface = Color.White,
                    onSurfaceVariant = Color.White.copy(alpha = 0.72f),
                    surface = Color(0xFF1F2124),
                    surfaceContainerHigh = Color(0xFF2B2E33),
                    surfaceContainerHighest = Color(0xFF33363C),
                    outlineVariant = Color.White.copy(alpha = 0.16f)
                )
                Surface(
                    color = Color(0xFF1F2124).copy(alpha = a(if (isFullMode) 0.98f else 0.94f)),
                    shape = shape,
                    shadowElevation = if (isFullMode) 14.dp else 8.dp,
                    modifier = widthBoundModifier()
                        .border(0.8.dp, Color.White.copy(alpha = 0.24f), shape)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OverlayVisualMediaTile(
                            imageUri = mediaImageUri,
                            videoUri = mediaVideoUri,
                            hasAudioMedia = hasAudioMedia,
                            accentColor = accentBorderColor,
                            useAlarmAudio = useAlarmAudio,
                            stretchToFill = stretchMediaToFill,
                            onMediaClick = onMediaClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(mediaHeight)
                        )
                        MaterialTheme(colorScheme = darkFooterScheme, typography = scaledTypography) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1F2124).copy(alpha = a(0.96f)))
                                    .padding(horizontal = contentPad, vertical = (contentPad.value * 0.78f).dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                content = content
                            )
                        }
                    }
                }
            }

            OverlayPanelStyle.MEDIA_EDITORIAL -> {
                val shape = RoundedCornerShape(r(if (isFullMode) 24 else 20))
                val editorialMediaH = clampMediaH(
                    rawDp = (if (isFullMode) 164f else 120f) * safeMediaHeightScale,
                    minFloorDp = 86.dp
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = a(if (isFullMode) 0.96f else 0.88f)),
                    shape = shape,
                    shadowElevation = if (isFullMode) 12.dp else 6.dp,
                    modifier = widthBoundModifier(minBound = maxOf(scaledMinWidth, 236.dp))
                        .border(0.9.dp, accentBorderColor.copy(alpha = 0.50f), shape)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (topBarContent != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f))
                                    .padding(horizontal = contentPad, vertical = if (isFullMode) 8.dp else 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                content = topBarContent
                            )
                            HorizontalDivider(color = hairline)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .padding(contentPad),
                            horizontalArrangement = Arrangement.spacedBy(if (isFullMode) 12.dp else 9.dp)
                        ) {
                            OverlayVisualMediaTile(
                                imageUri = mediaImageUri,
                                videoUri = mediaVideoUri,
                                hasAudioMedia = hasAudioMedia,
                                accentColor = accentBorderColor,
                                useAlarmAudio = useAlarmAudio,
                                stretchToFill = stretchMediaToFill,
                                onMediaClick = onMediaClick,
                                modifier = Modifier
                                    .weight(0.42f)
                                    .fillMaxHeight()
                                    .heightIn(min = editorialMediaH)
                                    .clip(RoundedCornerShape(if (isFullMode) 16.dp else 13.dp))
                            )
                            Column(
                                modifier = Modifier.weight(0.58f),
                                verticalArrangement = Arrangement.spacedBy(vSpacing),
                                content = content
                            )
                        }
                    }
                }
            }

            OverlayPanelStyle.DYNAMIC_PILL -> {
                val shape = RoundedCornerShape(r(32))
                val darkScheme = MaterialTheme.colorScheme.copy(
                    onSurface = Color.White,
                    onSurfaceVariant = Color.White.copy(alpha = 0.74f),
                    surface = Color(0xFF14161D),
                    surfaceContainerHigh = Color.White.copy(alpha = 0.14f),
                    outlineVariant = Color.White.copy(alpha = 0.22f)
                )
                val thumbSize = (42f * safeMediaHeightScale.coerceIn(0.75f, 1.55f)).dp
                Surface(
                    color = Color(0xFF14161D).copy(alpha = a(if (isFullMode) 0.96f else 0.90f)),
                    shape = shape,
                    shadowElevation = 8.dp,
                    modifier = widthBoundModifier()
                        .border(0.8.dp, Color.White.copy(alpha = 0.24f), shape)
                ) {
                    MaterialTheme(colorScheme = darkScheme, typography = scaledTypography) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = contentPad + 2.dp, vertical = contentPad),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!mediaImageUri.isNullOrBlank() || !mediaVideoUri.isNullOrBlank() || hasAudioMedia) {
                                OverlayVisualMediaTile(
                                    imageUri = mediaImageUri,
                                    videoUri = mediaVideoUri,
                                    hasAudioMedia = hasAudioMedia,
                                    accentColor = accentBorderColor,
                                    useAlarmAudio = useAlarmAudio,
                                    stretchToFill = stretchMediaToFill,
                                    onMediaClick = onMediaClick,
                                    compactMuteButton = true,
                                    modifier = Modifier
                                        .size(thumbSize)
                                        .clip(CircleShape)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(vSpacing),
                                content = content
                            )
                        }
                    }
                }
            }

            OverlayPanelStyle.FROSTED_MINI -> {
                val shape = RoundedCornerShape(r(18))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = a(if (isFullMode) 0.86f else 0.74f)),
                    shape = shape,
                    shadowElevation = 5.dp,
                    modifier = widthBoundModifier()
                        .border(0.8.dp, Color.White.copy(alpha = 0.46f), shape)
                ) {
                    Column(
                        modifier = Modifier.padding(contentPad),
                        verticalArrangement = Arrangement.spacedBy(vSpacing),
                        content = content
                    )
                }
            }

            OverlayPanelStyle.TAPE_NOTE -> {
                val cardShape = RoundedCornerShape(r(14))
                Box(
                    modifier = widthBoundModifier(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        color = baseSurfaceColor.copy(alpha = a(if (isFullMode) 1f else 0.90f)),
                        shape = cardShape,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp)
                            .border(0.8.dp, accentBorderColor.copy(alpha = 0.68f), cardShape)
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                start = contentPad,
                                end = contentPad,
                                top = contentPad + 5.dp,
                                bottom = contentPad
                            ),
                            verticalArrangement = Arrangement.spacedBy(vSpacing),
                            content = content
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(3.dp))
                    )
                }
            }

            OverlayPanelStyle.MEDIA_STRIP -> {
                val shape = RoundedCornerShape(r(18))
                val stripThumbSize = (56f * safeMediaHeightScale.coerceIn(0.70f, 1.65f)).dp
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = a(if (isFullMode) 0.94f else 0.88f)),
                    shape = shape,
                    shadowElevation = 6.dp,
                    modifier = widthBoundModifier()
                        .border(0.8.dp, hairline, shape)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(contentPad),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverlayVisualMediaTile(
                            imageUri = mediaImageUri,
                            videoUri = mediaVideoUri,
                            hasAudioMedia = hasAudioMedia,
                            accentColor = accentBorderColor,
                            useAlarmAudio = useAlarmAudio,
                            stretchToFill = stretchMediaToFill,
                            onMediaClick = onMediaClick,
                            compactMuteButton = true,
                            modifier = Modifier
                                .size(stripThumbSize)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(vSpacing),
                            content = content
                        )
                    }
                }
            }

            OverlayPanelStyle.POCKET_TILE -> {
                val shape = RoundedCornerShape(r(22))
                Surface(
                    color = Color.Transparent,
                    shape = shape,
                    shadowElevation = 6.dp,
                    modifier = widthBoundModifier()
                        .clip(shape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    baseSurfaceColor.copy(alpha = a(if (isFullMode) 0.97f else 0.88f)),
                                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = a(if (isFullMode) 0.96f else 0.86f))
                                )
                            )
                        )
                        .border(0.8.dp, accentBorderColor.copy(alpha = 0.58f), shape)
                ) {
                    Column(
                        modifier = Modifier.padding(contentPad),
                        verticalArrangement = Arrangement.spacedBy(vSpacing)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(32.dp)
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.68f))
                        )
                        content()
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun OverlayVisualMediaTile(
    imageUri: String?,
    videoUri: String?,
    hasAudioMedia: Boolean,
    accentColor: Color,
    useAlarmAudio: Boolean = false,
    showMuteToggle: Boolean = true,
    compactMuteButton: Boolean = false,
    stretchToFill: Boolean = false,
    onMediaClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val parsedImg = remember(imageUri) { imageUri?.let { runCatching { Uri.parse(it) }.getOrNull() } }
    val parsedVid = remember(videoUri) { videoUri?.let { runCatching { Uri.parse(it) }.getOrNull() } }
    val clickableMod = if (onMediaClick != null && (parsedImg != null || parsedVid != null)) {
        modifier
            .clickable(onClick = onMediaClick)
            .testTag("overlay_media_tap_zoom")
    } else {
        modifier
    }

    when {
        parsedImg != null -> {
            Box(modifier = clickableMod) {
                AsyncImage(
                    model = parsedImg,
                    imageLoader = AnimatedImageLoader.get(context),
                    contentDescription = "Overlay attached GIF, WebP or image",
                    contentScale = if (stretchToFill) ContentScale.FillBounds else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (onMediaClick != null && !compactMuteButton) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.52f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInFull,
                            contentDescription = "Tap to zoom media",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        parsedVid != null -> {
            var isMuted by remember(parsedVid, hasAudioMedia) { mutableStateOf(hasAudioMedia) }
            val exoPlayer = remember(parsedVid, useAlarmAudio) {
                ExoPlayer.Builder(context).build().apply {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(if (useAlarmAudio) C.USAGE_ALARM else C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build()
                    setAudioAttributes(attrs, !useAlarmAudio && !hasAudioMedia)
                    setMediaItem(MediaItem.fromUri(parsedVid))
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = if (hasAudioMedia) 0f else 1f
                    prepare()
                    playWhenReady = true
                }
            }
            LaunchedEffect(exoPlayer, isMuted) {
                exoPlayer.volume = if (isMuted) 0f else 1f
            }
            DisposableEffect(exoPlayer) {
                onDispose { exoPlayer.release() }
            }
            val targetResize = if (stretchToFill) {
                AspectRatioFrameLayout.RESIZE_MODE_FILL
            } else {
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
            Box(modifier = clickableMod.background(Color.Black)) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = targetResize
                        }
                    },
                    update = { view ->
                        if (view.resizeMode != targetResize) {
                            view.resizeMode = targetResize
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (onMediaClick != null && !compactMuteButton) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.56f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInFull,
                            contentDescription = "Tap to open full resizable video",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (showMuteToggle) {
                    val badgeSize = if (compactMuteButton) 22.dp else 30.dp
                    val iconSize = if (compactMuteButton) 13.dp else 16.dp
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(if (compactMuteButton) 3.dp else 6.dp)
                            .size(badgeSize)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.62f))
                            .border(0.6.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                            .clickable { isMuted = !isMuted }
                            .testTag("overlay_video_mute_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute overlay video" else "Mute overlay video",
                            tint = Color.White,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        }

        else -> {
            Box(
                modifier = modifier.background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFD9875F),
                            Color(0xFFBC9A94),
                            Color(0xFF6B9E94)
                        )
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasAudioMedia) Icons.Filled.GraphicEq else Icons.Filled.GraphicEq,
                    contentDescription = "Media artwork",
                    tint = Color.White.copy(alpha = 0.94f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun MotivationQuoteBanner(
    quote: String,
    compact: Boolean = false,
    onShuffle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(if (compact) 12.dp else 16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (compact) 8.dp else 12.dp,
                end = if (onShuffle != null) 2.dp else if (compact) 8.dp else 12.dp,
                top = if (compact) 6.dp else 8.dp,
                bottom = if (compact) 6.dp else 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = "Daily motivation",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(if (compact) 14.dp else 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "\"$quote\"",
                    style = (if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium)
                        .copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = if (compact) 2 else 4
                )
            }
            if (onShuffle != null) {
                IconButton(
                    onClick = onShuffle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "New motivation quote",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
