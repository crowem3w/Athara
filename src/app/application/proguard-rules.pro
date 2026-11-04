# Needed only if you enable minifyEnabled for release/staging. Add to the :app module's proguard-rules.pro.

# kotlinx.serialization (DTOs in :network and :authentication, and the token model in :secure-storage)
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-dontnote kotlinx.serialization.**
-keepclassmembers class org.example.core.network.** { *** Companion; }
-keepclasseswithmembers class org.example.core.network.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class org.example.core.network.**$$serializer { *; }
-keepclassmembers class org.example.shared.authentication.data.** { *** Companion; }
-keepclasseswithmembers class org.example.shared.authentication.data.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class org.example.shared.authentication.data.**$$serializer { *; }
-keepclassmembers class org.example.core.securestorage.** { *** Companion; }
-keepclasseswithmembers class org.example.core.securestorage.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class org.example.core.securestorage.**$$serializer { *; }

# Retrofit (suspend functions + generic Response<T>)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface org.example.core.network.SyncApi { *; }
-keep interface org.example.core.network.TokenRefreshApi { *; }
-keep interface org.example.shared.authentication.data.AuthApi { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
