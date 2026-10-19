package com.example.collectalogger2.util

import androidx.annotation.StringRes
import com.example.collectalogger2.R

object PlayStatus {
    const val Unplayed = "Unplayed"
    const val Played = "Played"
    const val Planned = "Plan to Play"
    const val Completed = "Completed"
    const val Beaten = "Beaten"
    const val Abandoned = "Abandoned"
    const val Playing = "Playing"

    // The list of all statuses, in display order
    val all = listOf(Unplayed, Playing, Played, Planned, Completed, Beaten, Abandoned)

    /**
     * Maps a status constant (as stored in the database) to its display string
     * resource, or null for unknown statuses. The constants themselves stay
     * locale-neutral — only the UI resolves them through this.
     */
    @StringRes
    fun displayResOf(status: String): Int? = when (status) {
        Unplayed -> R.string.status_unplayed
        Playing -> R.string.status_playing
        Played -> R.string.status_played
        Planned -> R.string.status_plan_to_play
        Completed -> R.string.status_completed
        Beaten -> R.string.status_beaten
        Abandoned -> R.string.status_abandoned
        else -> null
    }
}
