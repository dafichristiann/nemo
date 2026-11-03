package com.anchor.app.platform.accessibility

/**
 * Known social media and entertainment short-form video packages prone to continuous doomscrolling.
 */
object DoomscrollTargets {
    val DEFAULT_TARGETS: Set<String> = setOf(
        "com.zhiliaoapp.musically",       // TikTok
        "com.ss.android.ugc.trill",       // TikTok Lite / Global
        "com.instagram.android",          // Instagram
        "com.facebook.katana",            // Facebook
        "com.facebook.lite",              // Facebook Lite
        "com.twitter.android",            // X / Twitter
        "com.x.android",                  // X
        "com.reddit.frontpage",           // Reddit
        "com.google.android.youtube",     // YouTube
        "com.snapchat.android",           // Snapchat
        "com.pinterest",                  // Pinterest
        "tv.twitch.android.app",          // Twitch
    )
}
