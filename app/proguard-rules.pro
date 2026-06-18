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

# Ignore warnings for Java management APIs used by Ktor
-dontwarn java.lang.management.**

# Ignore warnings for SLF4J logging binder
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Keep Google Sign-In and Auth components
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.auth.**

# Keep Firebase Auth (if you use Firebase to bridge the Google Sign-in)
-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

# Keep OAuth credentials
-keep class androidx.credentials.** { *; }

# Keep all Bouncy Castle classes to prevent stripping
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Specifically keep the Ed25519 implementation
-keep class org.bouncycastle.crypto.signers.Ed25519Signer { *; }
-keep class org.bouncycastle.jcajce.provider.asymmetric.edec.** { *; }