# ==============================================================================
# ProGuard / R8 Rules for VYBE Music App
# ==============================================================================

# Preserve line numbers and source file attribute for actionable crash traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve standard annotations and signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# Kotlin Coroutines & Flow
# ------------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ------------------------------------------------------------------------------
# Moshi & JSON Serialization
# ------------------------------------------------------------------------------
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keep class com.example.data.model.** { *; }
-keep class com.example.data.drive.** { *; }
-keep class com.example.data.update.** { *; }
-keep class com.example.domain.model.** { *; }
-dontwarn com.squareup.moshi.**

# ------------------------------------------------------------------------------
# Retrofit & OkHttp
# ------------------------------------------------------------------------------
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ------------------------------------------------------------------------------
# Room Database
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.local.** { *; }
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# AndroidX Media3 & ExoPlayer
# ------------------------------------------------------------------------------
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class com.example.player.** { *; }
-dontwarn androidx.media3.**

# ------------------------------------------------------------------------------
# Google Play Services & Auth
# ------------------------------------------------------------------------------
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# ------------------------------------------------------------------------------
# Coil Image Loading
# ------------------------------------------------------------------------------
-dontwarn coil.**
