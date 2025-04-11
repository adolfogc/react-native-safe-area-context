package com.th3rdwave.safeareacontext

import com.th3rdwave.safeareacontext.Rect
import android.util.Log
import android.view.View
import com.facebook.react.bridge.ReactContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.viewmanagers.RNCSafeAreaProviderManagerDelegate
import com.facebook.react.viewmanagers.RNCSafeAreaProviderManagerInterface

private const val TAG = "SafeAreaProviderManager"

/**
 * ViewGroupManager for SafeAreaProvider component.
 * Handles creation, management, and event dispatching for safe area insets.
 */
@ReactModule(name = SafeAreaProviderManager.REACT_CLASS)
class SafeAreaProviderManager :
    ViewGroupManager<SafeAreaProvider>(),
    RNCSafeAreaProviderManagerInterface<SafeAreaProvider> {

    private val delegate = RNCSafeAreaProviderManagerDelegate(this)
    
    override fun getDelegate() = delegate
    
    override fun getName(): String = REACT_CLASS
    
    override fun createViewInstance(context: ThemedReactContext): SafeAreaProvider =
        SafeAreaProvider(context)

    /**
     * Registers the onInsetsChange event for React Native
     */
    override fun getExportedCustomDirectEventTypeConstants(): Map<String, Any> =
        mapOf(
            InsetsChangeEvent.EVENT_NAME to mapOf("registrationName" to "onInsetsChange")
        )

    /**
     * Sets up the event emitter for insets change events
     */
    override fun addEventEmitters(reactContext: ThemedReactContext, view: SafeAreaProvider) {
        super.addEventEmitters(reactContext, view)
        view.setOnInsetsChangeHandler(::handleOnInsetsChange)
    }

    companion object {
        const val REACT_CLASS = "RNCSafeAreaProvider"
    }
}

/**
 * Handles insets changes and dispatches events to React Native
 *
 * @param view The SafeAreaProvider that triggered the change
 * @param insets The new edge insets
 * @param frame The frame rectangle using the library's Rect type
 */
private fun handleOnInsetsChange(view: SafeAreaProvider, insets: EdgeInsets, frame: Rect) {
    // Get ReactContext, returning early if not found
    val reactContext = view.context as? ReactContext ?: run {
        Log.w(TAG, "Expected ReactContext but found ${view.context::class.java.simpleName}")
        return
    }

    val reactTag = view.id
    if (reactTag == View.NO_ID) {
        Log.w(TAG, "View has an invalid tag: $reactTag")
        return
    }

    try {
        val surfaceId = UIManagerHelper.getSurfaceId(reactContext)
        UIManagerHelper.getEventDispatcherForReactTag(reactContext, reactTag)?.let { dispatcher ->
            dispatcher.dispatchEvent(InsetsChangeEvent(surfaceId, reactTag, insets, frame))
        } ?: run {
            Log.w(TAG, "Could not get event dispatcher for reactTag: $reactTag")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to dispatch InsetsChangeEvent for tag $reactTag", e)
    }
}
