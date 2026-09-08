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

# MetisJson deserialises by reflecting over field names, so renaming a DTO field silently
# produces empty data rather than an error. The DTOs also carry @Keep; this covers the
# library's own reflective access.
-keep class io.lilithtechs.metisJson.** { *; }

# Room entities are records; keep their component accessors, which the generated DAO
# implementations and the type converters reach by name.
-keepclassmembers class * {
    @androidx.room.ColumnInfo <fields>;
}

# Keep line numbers so a crash report from a release build is readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
