# Nemo — project-specific R8/ProGuard rules (release build).
#
# Most frameworks used here ship consumer rules that are applied automatically:
#   - Room        (keeps @Entity/@Dao/@Database, generated impls)
#   - Hilt/Dagger (keeps generated components; @HiltWorker factories)
#   - WorkManager (androidx.work consumer rules)
#   - Compose     (androidx.compose consumer rules)
#
# The rules below cover the few app-specific reflection/entry edges that R8 cannot
# see by itself. Keep this list minimal and intentional.

# ---------------------------------------------------------------------------
# Kotlin metadata + coroutines
# ---------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,InnerClasses,Signature,SourceFile,LineNumberTable

# kotlinx.coroutines: keep internal service-loading hooks (safe, official advice).
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepclassmembers class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-dontwarn kotlinx.coroutines.**

# ---------------------------------------------------------------------------
# Room: generated *_Impl databases/DAOs are reached reflectively by name.
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Hilt / Dagger: entry points and generated components.
# ---------------------------------------------------------------------------
-keep,allowobfuscation @interface dagger.hilt.android.HiltAndroidApp
-keep,allowobfuscation @interface dagger.hilt.android.AndroidEntryPoint
-keep,allowobfuscation @interface dagger.hilt.android.lifecycle.HiltViewModel
-keep,allowobfuscation @interface androidx.hilt.work.HiltWorker
-keep class * extends androidx.work.ListenableWorker { *; }

# ---------------------------------------------------------------------------
# WorkManager: @HiltWorker classes are instantiated via the injected factory.
# ---------------------------------------------------------------------------
-keep class * implements androidx.work.WorkerFactory { *; }

# ---------------------------------------------------------------------------
# App entry points referenced from AndroidManifest.xml.
# ---------------------------------------------------------------------------
-keep class com.anchor.app.NemoApp { *; }
-keep class com.anchor.app.MainActivity { *; }
-keep class com.anchor.app.platform.notification.InterventionActionReceiver { *; }
-keep class com.anchor.app.platform.service.FocusService { *; }
