package fail.failure.cursor.onboarding

import android.content.Context

/** Tracks whether the one-time "what is this app" intro has been shown. Nothing sensitive here. */
class OnboardingPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    fun hasSeenIntro(): Boolean = prefs.getBoolean(KEY_SEEN_INTRO, false)

    fun markIntroSeen() {
        prefs.edit().putBoolean(KEY_SEEN_INTRO, true).apply()
    }

    private companion object {
        const val KEY_SEEN_INTRO = "seen_intro"
    }
}
