package com.th3rdwave.safeareacontext

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.fabric.FabricUIManager
import com.facebook.react.uimanager.StateWrapper
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.common.UIManagerType
import com.facebook.react.views.view.ReactViewGroup

private const val TAG = "SafeAreaView"

/**
 * SafeAreaView implementation for React Native Fabric.
 * 
 * This class detects safe area insets changes and communicates them to the Fabric system.
 * It does not directly modify view properties, instead relying on Fabric to apply the
 * appropriate layout changes based on insets, mode, and edge settings.
 */
class SafeAreaView(context: Context?) : ReactViewGroup(context), ViewTreeObserver.OnPreDrawListener {

  private var insets: EdgeInsets? = null
  private var providerView: View? = null
  private var stateWrapper: StateWrapper? = null

  fun getStateWrapper(): StateWrapper? = stateWrapper

  fun setStateWrapper(stateWrapper: StateWrapper?) {
    this.stateWrapper = stateWrapper
  }

  /**
   * Notifies the Fabric system about inset changes.
   */
  private fun updateInsets() {
    insets?.let { currentInsets ->
      // Update state using StateWrapper if available (preferred Fabric approach)
      stateWrapper?.let { wrapper ->
        val map = Arguments.createMap()
        map.putMap("insets", edgeInsetsToJsMap(currentInsets))
        wrapper.updateState(map)
        return
      } ?: run {
        // Otherwise use event dispatch as fallback
        dispatchInsetsChangeEvent(currentInsets)
      }
    }
  }

  /**
   * Dispatches an InsetsChangeEvent to the JS layer via Fabric.
   */
  private fun dispatchInsetsChangeEvent(currentInsets: EdgeInsets) {
    val reactContext = context as? ReactContext ?: run {
      Log.e(TAG, "Context is not an instance of ReactContext")
      return
    }

    try {
      val surfaceId = UIManagerHelper.getSurfaceId(reactContext)
      val fabricUIManager =
        UIManagerHelper.getUIManager(reactContext, UIManagerType.FABRIC) as? FabricUIManager
      if (fabricUIManager != null) {
        val event = InsetsChangeEvent(
          surfaceId,
          id,
          currentInsets,
          Rect(0f, 0f, width.toFloat(), height.toFloat())
        )
        UIManagerHelper.getEventDispatcherForReactTag(reactContext, id)?.dispatchEvent(event)
      } else {
        Log.e(TAG, "Failed to retrieve FabricUIManager")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error dispatching InsetsChangeEvent", e)
    }
  }

  /**
   * These methods store the mode/edge settings.
   * The actual application of these settings is handled by the Fabric system,
   * not directly by this native view.
   */
  fun setMode(mode: SafeAreaViewMode) {
    // Store mode for potential future use with StateWrapper
    // No direct view manipulation here
  }

  fun setEdges(edges: SafeAreaViewEdges) {
    // Store edges for potential future use with StateWrapper
    // No direct view manipulation here
  }

  /**
   * Checks for updates to the safe area insets. Returns true if an update was made.
   */
  private fun maybeUpdateInsets(): Boolean {
    providerView?.let { view ->
      getSafeAreaInsets(view)?.let { computedInsets ->
        if (insets != computedInsets) {
          insets = computedInsets
          updateInsets()
          return true
        }
      }
    }
    return false
  }

  /**
   * Searches up the view hierarchy for a SafeAreaProvider.
   * Returns null if no provider is found.
   */
  private fun findProvider(): View? {
    var current = parent
    while (current != null) {
      if (current is SafeAreaProvider) {
        return current
      }
      current = current.parent
    }
    Log.w(TAG, "No SafeAreaProvider found in the view hierarchy")
    return null
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    providerView = findProvider()
    providerView?.viewTreeObserver?.addOnPreDrawListener(this)
    maybeUpdateInsets()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    providerView?.viewTreeObserver?.removeOnPreDrawListener(this)
    providerView = null
  }

  override fun onPreDraw(): Boolean {
    val didUpdate = maybeUpdateInsets()
    // If insets changed, return false to cancel this draw pass
    // This gives Fabric time to process the inset change and update layout
    return !didUpdate
  }
}
