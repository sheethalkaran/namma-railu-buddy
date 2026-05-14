-keepattributes Signature
-keepattributes *Annotation*

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.nammarailu.buddy.data.model.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
