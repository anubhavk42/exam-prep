# Keep line numbers and source file attributes for readable crash reports in release builds
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Android Components & BroadcastReceivers
-keep class com.anubhav.diprep.receiver.ReminderReceiver { *; }
-keep public class * extends android.content.BroadcastReceiver

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**
-keepclassmembers class * {
    @androidx.room.Database *;
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}

# App Data Models & Room Entities
-keep class com.anubhav.diprep.data.local.db.** { *; }
-keep class com.anubhav.diprep.data.model.** { *; }
-keep class com.anubhav.diprep.data.datastore.** { *; }

# DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn androidx.datastore.**

# Jetpack Compose & Material 3
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# Kotlin Coroutines & StateFlow
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.flow.** { *; }

# JSON Processing (org.json.JSONArray / org.json.JSONObject)
-dontwarn org.json.**
-keep class org.json.** { *; }
