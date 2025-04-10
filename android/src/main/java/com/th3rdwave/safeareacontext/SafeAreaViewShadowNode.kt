package com.th3rdwave.safeareacontext

import com.facebook.react.bridge.Dynamic
import com.facebook.react.bridge.ReadableType
import com.facebook.react.uimanager.*
import com.facebook.react.uimanager.annotations.ReactPropGroup
import kotlin.math.max

class SafeAreaViewShadowNode : LayoutShadowNode() {
  private var localData: SafeAreaViewLocalData? = null
  private val paddings: FloatArray = FloatArray(ViewProps.PADDING_MARGIN_SPACING_TYPES.size)
  private val margins: FloatArray = FloatArray(ViewProps.PADDING_MARGIN_SPACING_TYPES.size)
  private var needsUpdate = false

  init {
    // Initialize all spacing values to NaN
    for (i in ViewProps.PADDING_MARGIN_SPACING_TYPES.indices) {
      paddings[i] = Float.NaN
      margins[i] = Float.NaN
    }
  }

  private fun updateInsets() {
    val localData = localData ?: return

    // Default edge values
    var top = 0f
    var right = 0f
    var bottom = 0f
    var left = 0f

    // Determine metadata based on the current mode
    val meta = if (localData.mode == SafeAreaViewMode.PADDING) paddings else margins

    // Helper function that applies an override if the candidate is defined.
    fun applyOverride(current: Float, candidate: Float): Float =
      if (!candidate.isNaN()) candidate else current

    // Priority order: all edges > vertical/horizontal > individual sides.
    val allEdges = meta[Spacing.ALL]
    if (!allEdges.isNaN()) {
      top = allEdges
      right = allEdges
      bottom = allEdges
      left = allEdges
    }
    top = applyOverride(top, meta[Spacing.VERTICAL])
    bottom = applyOverride(bottom, meta[Spacing.VERTICAL])
    right = applyOverride(right, meta[Spacing.HORIZONTAL])
    left = applyOverride(left, meta[Spacing.HORIZONTAL])
    top = applyOverride(top, meta[Spacing.TOP])
    right = applyOverride(right, meta[Spacing.RIGHT])
    bottom = applyOverride(bottom, meta[Spacing.BOTTOM])
    left = applyOverride(left, meta[Spacing.LEFT])

    // Convert DIP values to pixels
    top = PixelUtil.toPixelFromDIP(top)
    right = PixelUtil.toPixelFromDIP(right)
    bottom = PixelUtil.toPixelFromDIP(bottom)
    left = PixelUtil.toPixelFromDIP(left)

    val edges = localData.edges
    val insets = localData.insets

    if (localData.mode == SafeAreaViewMode.PADDING) {
      super.setPadding(Spacing.TOP, getEdgeValue(edges.top, insets.top, top))
      super.setPadding(Spacing.RIGHT, getEdgeValue(edges.right, insets.right, right))
      super.setPadding(Spacing.BOTTOM, getEdgeValue(edges.bottom, insets.bottom, bottom))
      super.setPadding(Spacing.LEFT, getEdgeValue(edges.left, insets.left, left))
    } else {
      super.setMargin(Spacing.TOP, getEdgeValue(edges.top, insets.top, top))
      super.setMargin(Spacing.RIGHT, getEdgeValue(edges.right, insets.right, right))
      super.setMargin(Spacing.BOTTOM, getEdgeValue(edges.bottom, insets.bottom, bottom))
      super.setMargin(Spacing.LEFT, getEdgeValue(edges.left, insets.left, left))
    }
    
    // Mark the node as dirty for Fabric after updating insets
    dirty()
  }

  private fun getEdgeValue(
      edgeMode: SafeAreaViewEdgeModes,
      insetValue: Float,
      edgeValue: Float
  ): Float = when (edgeMode) {
      SafeAreaViewEdgeModes.OFF -> edgeValue
      SafeAreaViewEdgeModes.MAXIMUM -> max(insetValue, edgeValue)
      else -> insetValue + edgeValue
  }

  private fun resetInsets(mode: SafeAreaViewMode) {
    if (mode == SafeAreaViewMode.PADDING) {
      super.setPadding(Spacing.TOP, paddings[Spacing.TOP])
      super.setPadding(Spacing.RIGHT, paddings[Spacing.RIGHT])
      super.setPadding(Spacing.BOTTOM, paddings[Spacing.BOTTOM])
      super.setPadding(Spacing.LEFT, paddings[Spacing.LEFT])
    } else {
      super.setMargin(Spacing.TOP, margins[Spacing.TOP])
      super.setMargin(Spacing.RIGHT, margins[Spacing.RIGHT])
      super.setMargin(Spacing.BOTTOM, margins[Spacing.BOTTOM])
      super.setMargin(Spacing.LEFT, margins[Spacing.LEFT])
    }
    
    // Mark node as dirty for Fabric compatibility.
    dirty()
  }

  // Fabric-compatible approach to handle updates before layout.
  override fun onBeforeLayout(nativeViewHierarchyOptimizer: NativeViewHierarchyOptimizer) {
    if (needsUpdate) {
      needsUpdate = false
      updateInsets()
    }
    super.onBeforeLayout(nativeViewHierarchyOptimizer)
  }

  // Use markUpdated() to indicate the node is dirty in Fabric.
  override fun dirty() {
    markUpdated()
  }

  override fun setLocalData(data: Any) {
    if (data !is SafeAreaViewLocalData) return
    val oldLocalData = localData
    if (oldLocalData != null && oldLocalData.mode != data.mode) {
      resetInsets(oldLocalData.mode)
    }
    localData = data
    needsUpdate = false
    updateInsets()
  }

  @ReactPropGroup(
      names = [
          ViewProps.PADDING,
          ViewProps.PADDING_VERTICAL,
          ViewProps.PADDING_HORIZONTAL,
          ViewProps.PADDING_START,
          ViewProps.PADDING_END,
          ViewProps.PADDING_TOP,
          ViewProps.PADDING_BOTTOM,
          ViewProps.PADDING_LEFT,
          ViewProps.PADDING_RIGHT
      ])
  override fun setPaddings(index: Int, padding: Dynamic) {
    val spacingType = ViewProps.PADDING_MARGIN_SPACING_TYPES[index]
    paddings[spacingType] =
        if (padding.type == ReadableType.Number) padding.asDouble().toFloat() else Float.NaN
    super.setPaddings(index, padding)
    needsUpdate = true
  }

  @ReactPropGroup(
      names = [
          ViewProps.MARGIN,
          ViewProps.MARGIN_VERTICAL,
          ViewProps.MARGIN_HORIZONTAL,
          ViewProps.MARGIN_START,
          ViewProps.MARGIN_END,
          ViewProps.MARGIN_TOP,
          ViewProps.MARGIN_BOTTOM,
          ViewProps.MARGIN_LEFT,
          ViewProps.MARGIN_RIGHT
      ])
  override fun setMargins(index: Int, margin: Dynamic) {
    val spacingType = ViewProps.PADDING_MARGIN_SPACING_TYPES[index]
    margins[spacingType] =
        if (margin.type == ReadableType.Number) margin.asDouble().toFloat() else Float.NaN
    super.setMargins(index, margin)
    needsUpdate = true
  }
}
