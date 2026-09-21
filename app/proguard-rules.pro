# Add project specific ProGuard rules here.
# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Entities and DAOs
-keep class com.notivault.app.data.local.entity.** { *; }
-keep interface com.notivault.app.data.local.dao.** { *; }
-keep class com.notivault.app.service.parser.** { *; }

# Compose
-keepclassmembers class * extends androidx.compose.runtime.State { *; }
