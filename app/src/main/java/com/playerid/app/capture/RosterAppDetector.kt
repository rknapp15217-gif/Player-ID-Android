package com.playerid.app.capture

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class RosterApp(
    val name: String,
    val packageName: String,
    val isInstalled: Boolean = false,
    val icon: Drawable? = null
)

object RosterAppDetector {
    private val knownRosterApps = listOf(
        RosterApp("TeamSnap", "com.teamsnap.teamsnap"),
        RosterApp("TeamSnap", "com.teamsnap.mobile"),
        RosterApp("TeamSnap", "com.teamsnap.android"),
        RosterApp("TeamSnap", "com.teamsnap"),
        RosterApp("SportsEngine", "com.sportsengine.mobile"),
        RosterApp("MaxPreps", "com.maxpreps.android"),
        RosterApp("GameChanger", "com.gc.android.team"),
        RosterApp("TeamLinkt", "com.teamlinkt.app"),
        RosterApp("Jersey Watch", "com.jerseywatch"),
        RosterApp("SportsYou", "com.bluelabelapps.sportsyou"),
        RosterApp("LeagueApps", "com.leagueapps.mobile"),
        RosterApp("Stack Team", "com.stack.team"),
        RosterApp("Hudl", "com.hudl.android"),
        RosterApp("TeamReach", "com.teamreach.app"),
        RosterApp("RosterBot", "com.rosterbot.app"),
        RosterApp("Crossover", "com.crossover.app"),
        RosterApp("LeagueLineup", "com.leaguelineup.mobile")
    )

    fun getInstalledRosterApps(context: Context): List<RosterApp> {
        val packageManager = context.packageManager
        val found = knownRosterApps.mapNotNull { app ->
            try {
                val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
                val icon = packageManager.getApplicationIcon(appInfo)
                app.copy(isInstalled = true, icon = icon)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
        
        return found.distinctBy { it.name }
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = when {
                packageName.contains("teamsnap") -> {
                    // Try to deep link to roster page
                    val deepLinkPatterns = listOf(
                        "teamsnap://roster",
                        "teamsnap://teams/roster",
                        "teamsnap://team/roster"
                    )
                    
                    // Try each deep link pattern
                    var successfulIntent: Intent? = null
                    for (pattern in deepLinkPatterns) {
                        try {
                            val deepIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse(pattern)
                                setPackage(packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra("reset_scroll", true)
                            }
                            // Check if this can be handled
                            val resolveInfo = context.packageManager.resolveActivity(deepIntent, 0)
                            if (resolveInfo != null) {
                                successfulIntent = deepIntent
                                break
                            }
                        } catch (e: Exception) {
                            // Continue to next pattern
                        }
                    }
                    
                    // Use deep link if found, otherwise use regular launch
                    successfulIntent ?: context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                else -> {
                    context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }
            
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
