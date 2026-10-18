-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# Modele zapisywane w JSON (konfiguracja, pulpit, odpowiedzi API)
-keep class com.opendesktop.launcher.data.** { *; }
