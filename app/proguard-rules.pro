# androidx.security.crypto pulls in Tink, which references errorprone's build-time-only
# annotations; they're absent at runtime by design, so R8 just needs to stop warning about them.
-dontwarn com.google.errorprone.annotations.**

# Keep kotlinx.serialization models
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class fail.failure.grok.**$$serializer { *; }
-keepclassmembers class fail.failure.grok.** {
    *** Companion;
}
-keepclasseswithmembers class fail.failure.grok.** {
    kotlinx.serialization.KSerializer serializer(...);
}
