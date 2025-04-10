package com.th3rdwave.safeareacontext

import android.util.Log
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.ReactStylesDiffMap
import com.facebook.react.uimanager.StateWrapper
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.viewmanagers.RNCSafeAreaViewManagerInterface
import com.facebook.react.views.view.ReactViewGroup
import com.facebook.react.views.view.ReactViewManager

@ReactModule(name = SafeAreaViewManager.REACT_CLASS)
class SafeAreaViewManager : ReactViewManager(), RNCSafeAreaViewManagerInterface<SafeAreaView> {

  override fun getName() = REACT_CLASS

  override fun createViewInstance(context: ThemedReactContext) = SafeAreaView(context)

  override fun createShadowNodeInstance() = SafeAreaViewShadowNode()

  override fun getShadowNodeClass() = SafeAreaViewShadowNode::class.java

  /**
   * Sets the "mode" property on the native view.
   * Accepts "padding" or "margin"; logs a warning for unrecognized values.
   */
  @ReactProp(name = "mode")
  override fun setMode(view: SafeAreaView, mode: String?) {
    when (mode?.lowercase()) {
      "padding" -> view.setMode(SafeAreaViewMode.PADDING)
      "margin" -> view.setMode(SafeAreaViewMode.MARGIN)
      else -> Log.w(REACT_CLASS, "Received unrecognized mode: $mode")
    }
  }

  /**
   * Sets the "edges" property on the native view.
   * Converts edge strings to corresponding enum values, defaulting to OFF when not provided.
   */
  @ReactProp(name = "edges")
  override fun setEdges(view: SafeAreaView, edgesMap: ReadableMap?) {
    if (edgesMap != null) {
      view.setEdges(
          SafeAreaViewEdges(
              top = edgesMap.getString("top")?.let { SafeAreaViewEdgeModes.valueOf(it.uppercase()) }
                      ?: SafeAreaViewEdgeModes.OFF,
              right = edgesMap.getString("right")?.let { SafeAreaViewEdgeModes.valueOf(it.uppercase()) }
                      ?: SafeAreaViewEdgeModes.OFF,
              bottom = edgesMap.getString("bottom")?.let { SafeAreaViewEdgeModes.valueOf(it.uppercase()) }
                      ?: SafeAreaViewEdgeModes.OFF,
              left = edgesMap.getString("left")?.let { SafeAreaViewEdgeModes.valueOf(it.uppercase()) }
                      ?: SafeAreaViewEdgeModes.OFF))
    }
  }

  /**
   * Updates the state of the native view by assigning the provided state wrapper.
   */
  override fun updateState(
      view: ReactViewGroup,
      props: ReactStylesDiffMap?,
      stateWrapper: StateWrapper?
  ): Any? {
    (view as SafeAreaView).setStateWrapper(stateWrapper)
    return null
  }

  companion object {
    const val REACT_CLASS = "RNCSafeAreaView"
  }
}
