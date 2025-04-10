package com.th3rdwave.safeareacontext

import android.util.Log
import com.facebook.react.bridge.ReactContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.viewmanagers.RNCSafeAreaProviderManagerDelegate
import com.facebook.react.viewmanagers.RNCSafeAreaProviderManagerInterface

private const val TAG = "SafeAreaProviderManager"

@ReactModule(name = SafeAreaProviderManager.REACT_CLASS)
class SafeAreaProviderManager :
    ViewGroupManager<SafeAreaProvider>(), RNCSafeAreaProviderManagerInterface<SafeAreaProvider> {

  private val delegate = RNCSafeAreaProviderManagerDelegate(this)

  override fun getDelegate() = delegate

  override fun getName() = REACT_CLASS

  public override fun createViewInstance(context: ThemedReactContext) = SafeAreaProvider(context)

  override fun getExportedCustomDirectEventTypeConstants() =
      mapOf(
          InsetsChangeEvent.EVENT_NAME to mapOf("registrationName" to "onInsetsChange")
      )

  override fun addEventEmitters(reactContext: ThemedReactContext, view: SafeAreaProvider) {
    super.addEventEmitters(reactContext, view)
    view.setOnInsetsChangeHandler(::handleOnInsetsChange)
  }

  companion object {
    const val REACT_CLASS = "RNCSafeAreaProvider"
  }
}

private fun handleOnInsetsChange(view: SafeAreaProvider, insets: EdgeInsets, frame: Rect) {
  // Safe-cast view.context to ReactContext and log a warning if it fails.
  val reactContext = view.context as? ReactContext
  if (reactContext == null) {
    Log.w(TAG, "Expected ReactContext but found ${view.context}")
    return
  }

  val reactTag = view.id
  if (reactTag <= 0) return

  val surfaceId = UIManagerHelper.getSurfaceId(reactContext)
  UIManagerHelper.getEventDispatcherForReactTag(reactContext, reactTag)
      ?.dispatchEvent(InsetsChangeEvent(surfaceId, reactTag, insets, frame))
}
