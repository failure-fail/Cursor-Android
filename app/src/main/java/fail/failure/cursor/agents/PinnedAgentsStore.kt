package fail.failure.cursor.agents

import android.content.Context

/** Which agent IDs the user has pinned to the top of their list - purely local, nothing sensitive,
 * so a plain (non-encrypted) preferences file is fine, same as onboarding state. */
class PinnedAgentsStore(context: Context) {
    private val prefs = context.getSharedPreferences("pinned_agents", Context.MODE_PRIVATE)

    fun pinnedIds(): Set<String> = prefs.getStringSet(KEY_PINNED, emptySet()).orEmpty()

    fun setPinned(agentId: String, pinned: Boolean) {
        val current = pinnedIds().toMutableSet()
        if (pinned) current.add(agentId) else current.remove(agentId)
        prefs.edit().putStringSet(KEY_PINNED, current).apply()
    }

    private companion object {
        const val KEY_PINNED = "pinned_ids"
    }
}
