# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# RootEncoder
-keep class com.pedro.** { *; }
-dontwarn com.pedro.**

# ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
