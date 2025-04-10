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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "SafeAreaView"
private const val MAX_WAIT_TIME_NANO = 500_000_000L // 500ms

class SafeAreaView(context: Context?) : ReactViewGroup(context), ViewTreeObserver.OnPreDrawListener {

  private var mode = SafeAreaViewMode.PADDING
  private var insets: EdgeInsets? = null
  private var edges: SafeAreaViewEdges? = null
  private var providerView: View? = null
  private var stateWrapper: StateWrapper? = null

  fun getStateWrapper(): StateWrapper? = stateWrapper

  fun setStateWrapper(stateWrapper: StateWrapper?) {
    this.stateWrapper = stateWrapper
  }

  /**
   * Updates the view's safe area insets.
   */
  private fun updateInsets() {
    insets?.let { currentInsets ->
      // Provide default edge modes if not set.
      val currentEdges = edges ?: SafeAreaViewEdges(
        SafeAreaViewEdgeModes.ADDITIVE,
        SafeAreaViewEdgeModes.ADDITIVE,
        SafeAreaViewEdgeModes.ADDITIVE,
        SafeAreaViewEdgeModes.ADDITIVE
      )

      // Update state using StateWrapper if available.
      stateWrapper?.let { wrapper ->
        val map = Arguments.createMap()
        map.putMap("insets", edgeInsetsToJsMap(currentInsets))
        wrapper.updateState(map)
        return
      } ?: run {
        // Otherwise use the Fabric-compatible update.
        val reactContext = context as? ReactContext
        if (reactContext == null) {
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
            // Request layout to ensure UI updates.
            requestLayout()
            // Wait for the native modules queue thread to process this update.
            waitForReactLayout(reactContext)
          } else {
            Log.e(TAG, "Failed to retrieve FabricUIManager")
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error updating safe area insets", e)
        }
      }
    }
  }

  /**
   * Blocks the main thread until the native module thread is done processing,
   * or until MAX_WAIT_TIME_NANO has elapsed.
   */
  private fun waitForReactLayout(reactContext: ReactContext) {
    var done = false
    val lock = ReentrantLock()
    val condition = lock.newCondition()
    val startTime = System.nanoTime()
    var waitTime = 0L

    // Enqueue a task on the native modules queue thread.
    reactContext.runOnNativeModulesQueueThread {
      lock.withLock {
        if (!done) {
          done = true
          condition.signal()
        }
      }
    }
    // Wait for the condition to be signaled or until timeout.
    lock.withLock {
      while (!done && waitTime < MAX_WAIT_TIME_NANO) {
        try {
          condition.awaitNanos(MAX_WAIT_TIME_NANO)
        } catch (ex: InterruptedException) {
          // If interrupted, give up waiting.
          done = true
        }
        waitTime = System.nanoTime() - startTime
      }
    }
    if (waitTime >= MAX_WAIT_TIME_NANO) {
      Log.w(TAG, "Timed out waiting for layout.")
    }
  }

  fun setMode(mode: SafeAreaViewMode) {
    this.mode = mode
    updateInsets()
  }

  fun setEdges(edges: SafeAreaViewEdges) {
    this.edges = edges
    updateInsets()
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
   */
  private fun findProvider(): View {
    var current = parent
    while (current != null) {
      if (current is SafeAreaProvider) {
        return current
      }
      current = current.parent
    }
    return this
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
    if (didUpdate) {
      requestLayout()
    }
    // Returning false cancels the current draw pass.
    return !didUpdate
  }
}
