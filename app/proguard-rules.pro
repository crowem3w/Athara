# Needed only if you enable minifyEnabled for release/staging. Add to the :app module's proguard-rules.pro.

# kotlinx.serialization (DTOs in :core)
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-dontnote kotlinx.serialization.**
-keepclassmembers class org.example.core.net.** { *** Companion; }
-keepclasseswithmembers class org.example.core.net.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class org.example.core.net.**$$serializer { *; }

# Retrofit (suspend functions + generic Response<T>)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface org.example.core.net.BackendApi { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
