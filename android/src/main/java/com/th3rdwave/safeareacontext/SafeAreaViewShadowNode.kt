package com.th3rdwave.safeareacontext

import com.facebook.react.uimanager.*
import com.facebook.react.uimanager.annotations.ReactPropGroup
import com.facebook.react.bridge.Dynamic

class SafeAreaViewShadowNode : LayoutShadowNode() {
    // Standard padding/margin handling - no special logic needed
    // The JS layer will calculate and provide the final values
    
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
        ]
    )
    override fun setPaddings(index: Int, padding: Dynamic) {
        super.setPaddings(index, padding)
        markUpdated()
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
        ]
    )
    override fun setMargins(index: Int, margin: Dynamic) {
        super.setMargins(index, margin)
        markUpdated()
    }
}
