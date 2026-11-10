package com.anchor.app.platform.accessibility

/**
 * Known social media and entertainment short-form video packages prone to continuous doomscrolling.
 */
object DoomscrollTargets {
    val DEFAULT_TARGETS: Set<String> = setOf(
        "com.zhiliaoapp.musically",          // TikTok
        "com.ss.android.ugc.trill",          // TikTok Lite / Global
        "com.zhiliaoapp.musically.go",       // TikTok Lite Go
        "com.instagram.android",             // Instagram
        "com.instagram.barcelona",           // Meta Threads
        "com.facebook.katana",               // Facebook
        "com.facebook.lite",                 // Facebook Lite
        "com.twitter.android",               // X / Twitter
        "com.x.android",                     // X
        "com.reddit.frontpage",              // Reddit
        "com.google.android.youtube",        // YouTube
        "com.google.android.apps.youtube.music", // YouTube Music
        "com.snapchat.android",              // Snapchat
        "com.pinterest",                     // Pinterest
        "tv.twitch.android.app",             // Twitch
        "com.kwai.video",                    // SnackVideo / Kwai
        "com.kuaishou.nebula",               // SnackVideo Lite
        "xyz.blueskyweb.app",                // Bluesky
        "com.discord",                       // Discord
        "com.tumblr",                        // Tumblr
        "com.bereal.ft",                     // BeReal
    )

    fun isTarget(packageName: String, blockedApps: Set<String> = emptySet()): Boolean =
        packageName in DEFAULT_TARGETS || packageName in blockedApps
}
