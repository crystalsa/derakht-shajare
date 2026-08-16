# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Room entities (used with Room's generated code + reflection-free
# KSP/annotation processing, but keep entity fields safe regardless)
-keep class com.example.data.** { *; }

# Kotlin coroutines / Compose runtime metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Coil (image loading)
-keep class coil.** { *; }

# Keep line numbers for readable crash stack traces in production:
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
