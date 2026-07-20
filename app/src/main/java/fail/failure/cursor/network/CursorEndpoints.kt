package fail.failure.cursor.network

object CursorEndpoints {
    /** Official, documented REST surface (Background/Cloud Agents, models, repos, API key info). */
    const val API_BASE = "https://api.cursor.com/"

    /** Legacy/internal host used by the desktop app's own auth + dashboard calls. */
    const val API2_BASE = "https://api2.cursor.sh/"
}
