# Consumer ProGuard rules for rtmp-sdk

# Keep public API
-keep public class com.example.rtmp.sdk.** { *; }

# Keep RootEncoder
-keep class com.pedro.** { *; }

# Keep ExoPlayer
-keep class androidx.media3.** { *; }
