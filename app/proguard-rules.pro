# Keep kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class fail.failure.cursor.**$$serializer { *; }
-keepclassmembers class fail.failure.cursor.** {
    *** Companion;
}
-keepclasseswithmembers class fail.failure.cursor.** {
    kotlinx.serialization.KSerializer serializer(...);
}
