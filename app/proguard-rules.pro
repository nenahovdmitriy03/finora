# ─── Finora ProGuard / R8 rules ──────────────────────────────────────────────

# Keep annotations
-keepattributes *Annotation*

# ─── Kotlin Serialization ─────────────────────────────────────────────────────
-keepattributes InnerClasses
-keep,includedescriptorclasses class com.finora.**$$serializer { *; }
-keepclassmembers class com.finora.** {
    *** Companion;
}
-keepclasseswithmembers class com.finora.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Keep all @Serializable DTOs in SyncManager
-keep class com.finora.data.remote.SyncManager$* { *; }

# ─── Supabase / Ktor ─────────────────────────────────────────────────────────
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ─── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# ─── OkHttp / misc ───────────────────────────────────────────────────────────
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.slf4j.**
