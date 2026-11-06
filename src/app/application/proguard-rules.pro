# Needed only if you enable minifyEnabled for release/staging. Add to the :app module's proguard-rules.pro.

# kotlinx.serialization (DTOs in :network and :authentication, and the token model in :secure-storage)
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-dontnote kotlinx.serialization.**
-keepclassmembers class soc.athara.app.core.network.** { *** Companion; }
-keepclasseswithmembers class soc.athara.app.core.network.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class soc.athara.app.core.network.**$$serializer { *; }
-keepclassmembers class soc.athara.app.shared.authentication.data.** { *** Companion; }
-keepclasseswithmembers class soc.athara.app.shared.authentication.data.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class soc.athara.app.shared.authentication.data.**$$serializer { *; }
-keepclassmembers class soc.athara.app.shared.payments.data.** { *** Companion; }
-keepclasseswithmembers class soc.athara.app.shared.payments.data.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class soc.athara.app.shared.payments.data.**$$serializer { *; }
-keepclassmembers class soc.athara.app.core.securestorage.** { *** Companion; }
-keepclasseswithmembers class soc.athara.app.core.securestorage.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class soc.athara.app.core.securestorage.**$$serializer { *; }

# Retrofit (suspend functions + generic Response<T>)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface soc.athara.app.core.network.SyncApi { *; }
-keep interface soc.athara.app.core.network.TokenRefreshApi { *; }
-keep interface soc.athara.app.shared.authentication.data.AuthApi { *; }
-keep interface soc.athara.app.shared.payments.data.PaymentsApi { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
