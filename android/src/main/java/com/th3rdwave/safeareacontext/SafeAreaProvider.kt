package com.th3rdwave.safeareacontext

import android.content.Context
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.facebook.react.views.view.ReactViewGroup

/**
 * Type alias representing the handler that is invoked when safe area insets or frame change.
 */
typealias OnInsetsChangeHandler = (view: SafeAreaProvider, insets: EdgeInsets, frame: Rect) -> Unit

/**
 * A React Native view that observes safe area insets and frame changes.
 * It notifies registered listeners when these properties are updated.
 */
class SafeAreaProvider(context: Context?) :
    ReactViewGroup(context), ViewTreeObserver.OnPreDrawListener {

  private var insetsChangeHandler: OnInsetsChangeHandler? = null
  private var lastInsets: EdgeInsets? = null
  private var lastFrame: Rect? = null

  /**
   * Checks the current safe area insets and frame and, if they have changed,
   * triggers the registered change handler.
   */
  private fun maybeUpdateInsets() {
    val handler = insetsChangeHandler ?: return
    val edgeInsets = getSafeAreaInsets(this) ?: return
    // Safely cast rootView to ViewGroup
    val rootGroup = rootView as? ViewGroup ?: return
    val frame = getFrame(rootGroup, this) ?: return

    if (lastInsets != edgeInsets || lastFrame != frame) {
      handler(this, edgeInsets, frame)
      lastInsets = edgeInsets
      lastFrame = frame
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    viewTreeObserver.addOnPreDrawListener(this)
    maybeUpdateInsets()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    viewTreeObserver.removeOnPreDrawListener(this)
  }

  override fun onPreDraw(): Boolean {
    maybeUpdateInsets()
    return true
  }

  /**
   * Registers a new handler for insets changes.
   * Invokes [maybeUpdateInsets] immediately after setting the handler.
   */
  fun setOnInsetsChangeHandler(handler: OnInsetsChangeHandler?) {
    insetsChangeHandler = handler
    maybeUpdateInsets()
  }
}
