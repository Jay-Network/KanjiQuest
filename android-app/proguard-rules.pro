# KanjiQuest ProGuard Rules

# Keep Kuromoji dictionary data
-keep class com.atilika.kuromoji.** { *; }

# Keep SQLDelight generated code
-keep class com.jworks.kanjiquest.db.** { *; }

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Suppress SLF4J warnings (used by Kuromoji)
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.jworks.kanjiquest.**$$serializer { *; }
-keepclassmembers class com.jworks.kanjiquest.** {
    *** Companion;
}
-keepclasseswithmembers class com.jworks.kanjiquest.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor (HTTP client used by Supabase SDK)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep class io.ktor.client.engine.okhttp.** { *; }

# Supabase SDK
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**
-keep class io.github.jan.supabase.gotrue.** { *; }
-keep class io.github.jan.supabase.postgrest.** { *; }
-keep class io.github.jan.supabase.functions.** { *; }

# OkHttp (used by Ktor engine)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Supabase auth request/response models
-keep class * implements kotlinx.serialization.KSerializer { *; }
