# Keep Health Services callback implementations referenced reflectively by the platform.
-keep class * extends androidx.health.services.client.ExerciseUpdateCallback { *; }

# Hilt generated components are handled by the Hilt Gradle plugin's consumer rules.

# Keep enum names used for DataStore persistence (we store enum names as strings).
-keepclassmembers enum com.heartbeats.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
