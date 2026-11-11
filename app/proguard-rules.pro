# Add project specific ProGuard rules here.
-keep class ${.PACKAGE_NAME.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn android.support.**
