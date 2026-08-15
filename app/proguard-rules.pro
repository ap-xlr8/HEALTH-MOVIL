-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-keep class com.healthos.data.remote.** { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# AndroidX Security / Google Tink
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.meta.**

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# SQLCipher
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**

