# Proguard rules for Maria Browser
-dontwarn android.webkit.**
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
