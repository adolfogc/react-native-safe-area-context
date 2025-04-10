package com.th3rdwave.safeareacontext

import android.util.Log
import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.uimanager.ViewManager

// Extending BaseReactPackage for Fabric compatibility
class SafeAreaContextPackage : BaseReactPackage() {

  companion object {
    const val TAG = "SafeAreaContextModule"
  }

  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return when (name) {
      SafeAreaContextModule.NAME -> SafeAreaContextModule(reactContext)
      else -> {
        Log.d(TAG, "No module found for name: $name")
        null
      }
    }
  }

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
    val moduleList: Array<Class<out NativeModule>> = arrayOf(SafeAreaContextModule::class.java)
    val reactModuleInfoMap = mutableMapOf<String, ReactModuleInfo>()

    for (moduleClass in moduleList) {
      val reactModule = moduleClass.getAnnotation(ReactModule::class.java)
      if (reactModule != null) {
        reactModuleInfoMap[reactModule.name] = ReactModuleInfo(
          reactModule.name,
          moduleClass.name,
          true, // canOverrideExistingModule
          reactModule.needsEagerInit,
          reactModule.isCxxModule,
          BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
        )
      } else {
        Log.w(TAG, "Module ${moduleClass.name} is missing the @ReactModule annotation")
      }
    }

    return ReactModuleInfoProvider { reactModuleInfoMap }
  }

  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
    return listOf(
      SafeAreaProviderManager(),
      SafeAreaViewManager()
    )
  }
}
