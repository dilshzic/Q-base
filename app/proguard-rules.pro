# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn com.gemalto.jp2.JP2Decoder

-keep class com.algorithmx.q_base.core_ai.brain.** { *; }
-dontwarn com.algorithmx.q_base.core_ai.brain.**

# Keep data models used for GSON/Serialization
-keep class com.algorithmx.q_base.**.data.** { *; }
-keep class com.algorithmx.q_base.core.data.** { *; }
-keepclassmembers class com.algorithmx.q_base.**.data.** { <fields>; }
-keepclassmembers class com.algorithmx.q_base.core.data.** { <fields>; }

# GSON specific rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.internal.LinkedTreeMap { *; }
