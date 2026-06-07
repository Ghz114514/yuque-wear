# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.yuquewatch.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.yuquewatch.**$$serializer { *; }
-keep @kotlinx.serialization.Serializable class com.yuquewatch.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep our data models (serialized / reflected over)
-keep class com.yuquewatch.data.** { *; }
-keepclassmembers enum com.yuquewatch.data.** { *; }
