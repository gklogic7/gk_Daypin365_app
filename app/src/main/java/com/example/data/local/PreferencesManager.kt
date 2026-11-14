package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daypin_prefs")

enum class OverlayDisplayMode(val label: String) {
    TRANSPARENT("Transparent Overlay"),
    FULL("Full Overlay")
}

enum class OverlayPanelStyle(val label: String, val shortTag: String, val subtitle: String) {
    CLASSIC_CARD("Classic Paper", "Classic", "Compact rounded card with crisp hairline border"),
    GLASS_PILL("Apple Glass", "Glass", "Translucent frosted capsule with subtle border & compact typography"),
    TICKER_STRIP("Top Ticker", "Ticker", "Slim horizontal top bar with accent stripe"),
    ACTION_SPOTLIGHT("Spotlight", "Spotlight", "Contrasting header band and focused content block"),
    CORNER_TAB("Corner Tab", "Corner", "Asymmetric bookmark card with bold left accent"),
    SPLIT_MEDIA("Split Media", "Split", "Left side GIF/photo/video/art with right side text & player"),
    FULL_MEDIA_GRADIENT("Full Media Gradient", "Media", "Full-bleed media with bottom black gradient & crisp white text"),
    PLAIN_MEDIA("Plain Media", "Plain", "Pure image, GIF, or video with minimal bottom buttons only"),
    MEDIA_EDITORIAL("Media Editorial", "Editorial", "Top buttons over left media and right header & paragraph (no clock)"),
    DYNAMIC_PILL("Dynamic Pill", "Pill", "Ultra-compact capsule overlay with mini media & tight single block"),
    FROSTED_MINI("Frosted Mini", "Mini", "Compact translucent frosted card with minimal footprint"),
    TAPE_NOTE("Tape Note", "Tape", "Small memo card topped with a subtle translucent tape tab"),
    MEDIA_STRIP("Media Strip", "Strip", "Slim horizontal mini strip pairing thumbnail media and text"),
    POCKET_TILE("Pocket Tile", "Pocket", "Compact widget-style mini tile with soft gradient & tight spacing");

    val isSmallVariant: Boolean
        get() = this == DYNAMIC_PILL ||
            this == FROSTED_MINI ||
            this == TAPE_NOTE ||
            this == MEDIA_STRIP ||
            this == POCKET_TILE
}

enum class OverlayAnimationStyle(val label: String, val shortLabel: String) {
    FADE("Fade In & Out", "Fade"),
    POPUP("Pop-Up Scale", "Pop-Up"),
    SLIDE_TOP("Slide Top", "Top"),
    SLIDE_BOTTOM("Slide Bottom", "Bottom"),
    SLIDE_SIDE("Slide Side", "Side"),
    EXPAND_UNFOLD("Expand Unfold", "Unfold"),
    ZOOM_BOUNCE("Zoom Bounce", "Bounce")
}

enum class DeskLaunchMode(val label: String, val subtitle: String) {
    MANUAL("Manual", "Open Desk Standby Board on tap"),
    AUTO_ON_CHARGING("Auto on Charging", "Open automatically when power/dock connects"),
    AUTO_ON_APP_OPEN("Auto on App Open", "Start directly in Desk Standby Board")
}

enum class QuoteScheduleMode(val label: String, val subtitle: String) {
    RANDOM_PER_DAY("Random Times/Day", "Spreads random inspiration across daytime hours (08:00–21:00)"),
    CUSTOM_INTERVAL("Custom Interval", "Sends a peaceful quote every N minutes/hours (e.g. 10 min)")
}

data class AppPreferences(
    val hasCompletedPermissionOnboarding: Boolean = false,
    val defaultSnoozeMinutes: Int = 10,
    val vibrationEnabled: Boolean = true,
    val fadeInAudio: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val overlayDisplayMode: OverlayDisplayMode = OverlayDisplayMode.TRANSPARENT,
    val overlayPanelStyle: OverlayPanelStyle = OverlayPanelStyle.CLASSIC_CARD,
    val overlayAnimationStyle: OverlayAnimationStyle = OverlayAnimationStyle.POPUP,
    val overlayNotificationEnabled: Boolean = true,
    val overlayPanelScale: Float = 1.0f,
    val overlayMediaHeightScale: Float = 1.0f,
    val overlayTextScale: Float = 1.0f,
    val alarmActivePanelStyle: OverlayPanelStyle = OverlayPanelStyle.PLAIN_MEDIA,
    val alarmActivePanelScale: Float = 1.0f,
    val alarmActiveMediaHeightScale: Float = 1.0f,
    val alarmActiveTextScale: Float = 1.0f,
    val alarmActiveCornerRadius: Int = 22,
    val alarmActiveOpacity: Float = 0.94f,
    val alarmActiveShowQuote: Boolean = true,
    val deskLaunchMode: DeskLaunchMode = DeskLaunchMode.MANUAL,
    val deskKeepScreenOn: Boolean = true,
    val deskDimMode: Boolean = false,
    val quoteNotificationsEnabled: Boolean = false,
    val quoteScheduleMode: QuoteScheduleMode = QuoteScheduleMode.RANDOM_PER_DAY,
    val quoteRandomPerDay: Int = 3,
    val quoteIntervalMinutes: Int = 10,
    val quoteCategory: com.example.data.repository.QuoteCategory = com.example.data.repository.QuoteCategory.ALL
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val PERMISSION_ONBOARDING_DONE = booleanPreferencesKey("permission_onboarding_done")
        val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val FADE_IN_AUDIO = booleanPreferencesKey("fade_in_audio")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val OVERLAY_DISPLAY_MODE = stringPreferencesKey("overlay_display_mode")
        val OVERLAY_PANEL_STYLE = stringPreferencesKey("overlay_panel_style")
        val OVERLAY_ANIMATION_STYLE = stringPreferencesKey("overlay_animation_style")
        val OVERLAY_NOTIFICATION_ENABLED = booleanPreferencesKey("overlay_notification_enabled")
        val OVERLAY_PANEL_SCALE = floatPreferencesKey("overlay_panel_scale")
        val OVERLAY_MEDIA_HEIGHT_SCALE = floatPreferencesKey("overlay_media_height_scale")
        val OVERLAY_TEXT_SCALE = floatPreferencesKey("overlay_text_scale")
        val ALARM_ACTIVE_PANEL_STYLE = stringPreferencesKey("alarm_active_panel_style")
        val ALARM_ACTIVE_PANEL_SCALE = floatPreferencesKey("alarm_active_panel_scale")
        val ALARM_ACTIVE_MEDIA_HEIGHT_SCALE = floatPreferencesKey("alarm_active_media_height_scale")
        val ALARM_ACTIVE_TEXT_SCALE = floatPreferencesKey("alarm_active_text_scale")
        val ALARM_ACTIVE_CORNER_RADIUS = intPreferencesKey("alarm_active_corner_radius")
        val ALARM_ACTIVE_OPACITY = floatPreferencesKey("alarm_active_opacity")
        val ALARM_ACTIVE_SHOW_QUOTE = booleanPreferencesKey("alarm_active_show_quote")
        val DESK_LAUNCH_MODE = stringPreferencesKey("desk_launch_mode")
        val DESK_KEEP_SCREEN_ON = booleanPreferencesKey("desk_keep_screen_on")
        val DESK_DIM_MODE = booleanPreferencesKey("desk_dim_mode")
        val QUOTE_NOTIFS_ENABLED = booleanPreferencesKey("quote_notifications_enabled")
        val QUOTE_SCHEDULE_MODE = stringPreferencesKey("quote_schedule_mode")
        val QUOTE_RANDOM_PER_DAY = intPreferencesKey("quote_random_per_day")
        val QUOTE_INTERVAL_MINUTES = intPreferencesKey("quote_interval_minutes")
        val QUOTE_CATEGORY = stringPreferencesKey("quote_category")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            hasCompletedPermissionOnboarding = prefs[Keys.PERMISSION_ONBOARDING_DONE] ?: false,
            defaultSnoozeMinutes = prefs[Keys.DEFAULT_SNOOZE_MINUTES] ?: 10,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            fadeInAudio = prefs[Keys.FADE_IN_AUDIO] ?: true,
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM,
            overlayDisplayMode = prefs[Keys.OVERLAY_DISPLAY_MODE]?.let {
                runCatching { OverlayDisplayMode.valueOf(it) }.getOrDefault(OverlayDisplayMode.TRANSPARENT)
            } ?: OverlayDisplayMode.TRANSPARENT,
            overlayPanelStyle = prefs[Keys.OVERLAY_PANEL_STYLE]?.let {
                runCatching { OverlayPanelStyle.valueOf(it) }.getOrDefault(OverlayPanelStyle.CLASSIC_CARD)
            } ?: OverlayPanelStyle.CLASSIC_CARD,
            overlayAnimationStyle = prefs[Keys.OVERLAY_ANIMATION_STYLE]?.let {
                runCatching { OverlayAnimationStyle.valueOf(it) }.getOrDefault(OverlayAnimationStyle.POPUP)
            } ?: OverlayAnimationStyle.POPUP,
            overlayNotificationEnabled = prefs[Keys.OVERLAY_NOTIFICATION_ENABLED] ?: true,
            overlayPanelScale = (prefs[Keys.OVERLAY_PANEL_SCALE] ?: 1.0f).coerceIn(0.50f, 2.20f),
            overlayMediaHeightScale = (prefs[Keys.OVERLAY_MEDIA_HEIGHT_SCALE] ?: 1.0f).coerceIn(0.50f, 2.40f),
            overlayTextScale = (prefs[Keys.OVERLAY_TEXT_SCALE] ?: 1.0f).coerceIn(0.80f, 1.45f),
            alarmActivePanelStyle = prefs[Keys.ALARM_ACTIVE_PANEL_STYLE]?.let {
                runCatching { OverlayPanelStyle.valueOf(it) }.getOrDefault(OverlayPanelStyle.PLAIN_MEDIA)
            } ?: OverlayPanelStyle.PLAIN_MEDIA,
            alarmActivePanelScale = (prefs[Keys.ALARM_ACTIVE_PANEL_SCALE] ?: 1.0f).coerceIn(0.50f, 2.20f),
            alarmActiveMediaHeightScale = (prefs[Keys.ALARM_ACTIVE_MEDIA_HEIGHT_SCALE] ?: 1.0f).coerceIn(0.50f, 2.40f),
            alarmActiveTextScale = (prefs[Keys.ALARM_ACTIVE_TEXT_SCALE] ?: 1.0f).coerceIn(0.80f, 1.45f),
            alarmActiveCornerRadius = (prefs[Keys.ALARM_ACTIVE_CORNER_RADIUS] ?: 22).coerceIn(12, 32),
            alarmActiveOpacity = (prefs[Keys.ALARM_ACTIVE_OPACITY] ?: 0.94f).coerceIn(0.70f, 1.00f),
            alarmActiveShowQuote = prefs[Keys.ALARM_ACTIVE_SHOW_QUOTE] ?: true,
            deskLaunchMode = prefs[Keys.DESK_LAUNCH_MODE]?.let {
                runCatching { DeskLaunchMode.valueOf(it) }.getOrDefault(DeskLaunchMode.MANUAL)
            } ?: DeskLaunchMode.MANUAL,
            deskKeepScreenOn = prefs[Keys.DESK_KEEP_SCREEN_ON] ?: true,
            deskDimMode = prefs[Keys.DESK_DIM_MODE] ?: false,
            quoteNotificationsEnabled = prefs[Keys.QUOTE_NOTIFS_ENABLED] ?: false,
            quoteScheduleMode = prefs[Keys.QUOTE_SCHEDULE_MODE]?.let {
                runCatching { QuoteScheduleMode.valueOf(it) }.getOrDefault(QuoteScheduleMode.RANDOM_PER_DAY)
            } ?: QuoteScheduleMode.RANDOM_PER_DAY,
            quoteRandomPerDay = (prefs[Keys.QUOTE_RANDOM_PER_DAY] ?: 3).coerceIn(1, 20),
            quoteIntervalMinutes = (prefs[Keys.QUOTE_INTERVAL_MINUTES] ?: 10).coerceIn(1, 720),
            quoteCategory = prefs[Keys.QUOTE_CATEGORY]?.let {
                runCatching { com.example.data.repository.QuoteCategory.valueOf(it) }
                    .getOrDefault(com.example.data.repository.QuoteCategory.ALL)
            } ?: com.example.data.repository.QuoteCategory.ALL
        )
    }

    suspend fun markPermissionOnboardingCompleted() {
        context.dataStore.edit { it[Keys.PERMISSION_ONBOARDING_DONE] = true }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_SNOOZE_MINUTES] = minutes.coerceIn(1, 60) }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setFadeInAudio(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FADE_IN_AUDIO] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setOverlayDisplayMode(mode: OverlayDisplayMode) {
        context.dataStore.edit { it[Keys.OVERLAY_DISPLAY_MODE] = mode.name }
    }

    suspend fun setOverlayPanelStyle(style: OverlayPanelStyle) {
        context.dataStore.edit { it[Keys.OVERLAY_PANEL_STYLE] = style.name }
    }

    suspend fun setOverlayAnimationStyle(style: OverlayAnimationStyle) {
        context.dataStore.edit { it[Keys.OVERLAY_ANIMATION_STYLE] = style.name }
    }

    suspend fun setOverlayNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OVERLAY_NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setOverlayPanelScale(scale: Float) {
        context.dataStore.edit { it[Keys.OVERLAY_PANEL_SCALE] = scale.coerceIn(0.50f, 2.20f) }
    }

    suspend fun setOverlayMediaHeightScale(scale: Float) {
        context.dataStore.edit { it[Keys.OVERLAY_MEDIA_HEIGHT_SCALE] = scale.coerceIn(0.50f, 2.40f) }
    }

    suspend fun setOverlayTextScale(scale: Float) {
        context.dataStore.edit { it[Keys.OVERLAY_TEXT_SCALE] = scale.coerceIn(0.80f, 1.45f) }
    }

    suspend fun setAlarmActivePanelStyle(style: OverlayPanelStyle) {
        context.dataStore.edit { it[Keys.ALARM_ACTIVE_PANEL_STYLE] = style.name }
    }

    suspend fun setAlarmActivePanelScale(scale: Float) {
        context.dataStore.edit { it[Keys.ALARM_ACTIVE_PANEL_SCALE] = scale.coerceIn(0.50f, 2.20f) }
    }

    suspend fun setAlarmActiveMediaHeightScale(scale: Float) {
        context.dataStore.edit { it[Keys.ALARM_ACTIVE_MEDIA_HEIGHT_SCALE] = scale.coerceIn(0.50f, 2.40f) }
    }

    suspend fun setAlarmActiveTextScale(scale: Float) {
        context.dataStore.edit { it[Keys.ALARM_ACTIVE_TEXT_SCALE] = scale.coerceIn(0.80f, 1.45f) }
    }

    suspend fun setAlarmActiveCornerRadius(radiusDp: Int) {
        context.dataStore.edit { it[Keys.ALARM_ACTIVE_CORNER_RADIUS] = radiusDp.coerceIn(12, 32) }
    }

    suspend fun setAlarmActiveOpacity(opacity: Float) {
        context.dataStore.edit { it[Keys.ALARM_ACTIVE_OPACITY] = opacity.coerceIn(0.70f, 1.00f) }
    }

    suspend fun setAlarmActiveShowQuote(show: Boolean) {
        context.dataStore.edit { it[Keys.ALARM_ACTIVE_SHOW_QUOTE] = show }
    }

    suspend fun setDeskLaunchMode(mode: DeskLaunchMode) {
        context.dataStore.edit { it[Keys.DESK_LAUNCH_MODE] = mode.name }
    }

    suspend fun setDeskKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DESK_KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setDeskDimMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DESK_DIM_MODE] = enabled }
    }

    suspend fun setQuoteNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.QUOTE_NOTIFS_ENABLED] = enabled }
    }

    suspend fun setQuoteScheduleMode(mode: QuoteScheduleMode) {
        context.dataStore.edit { it[Keys.QUOTE_SCHEDULE_MODE] = mode.name }
    }

    suspend fun setQuoteRandomPerDay(count: Int) {
        context.dataStore.edit { it[Keys.QUOTE_RANDOM_PER_DAY] = count.coerceIn(1, 20) }
    }

    suspend fun setQuoteIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.QUOTE_INTERVAL_MINUTES] = minutes.coerceIn(1, 720) }
    }

    suspend fun setQuoteCategory(category: com.example.data.repository.QuoteCategory) {
        context.dataStore.edit { it[Keys.QUOTE_CATEGORY] = category.name }
    }
}
