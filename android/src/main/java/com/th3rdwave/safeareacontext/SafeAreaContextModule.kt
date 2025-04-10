package com.th3rdwave.safeareacontext

import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = SafeAreaContextModule.NAME)
class SafeAreaContextModule(reactContext: ReactApplicationContext?) :
    NativeSafeAreaContextSpec(reactContext) {

    companion object {
        const val NAME = "RNCSafeAreaContext"
        private const val TAG = "SafeAreaContextModule"
    }

    override fun getName(): String = NAME

    // Exports the initial window metrics to JavaScript
    public override fun getTypedExportedConstants(): Map<String, Any?> {
        val initialMetrics = getInitialWindowMetrics()
        if (initialMetrics == null) {
            Log.w(TAG, "Initial window metrics are not available.")
        }
        return mapOf("initialWindowMetrics" to initialMetrics)
    }

    /**
     * Computes the initial window metrics if available.
     *
     * Returns a map with "insets" and "frame" if the decor view is laid out; otherwise, returns null.
     */
    private fun getInitialWindowMetrics(): Map<String, Any>? {

        val activity = reactApplicationContext.currentActivity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "Activity is not valid for getting window metrics.")
            return null
        }

        val decorView = activity.window?.decorView as? ViewGroup
        if (decorView == null) {
            Log.w(TAG, "Decor view is not available.")
            return null
        }

        val contentView = decorView.findViewById<View>(android.R.id.content)
        if (contentView == null) {
            Log.w(TAG, "Content view is not available.")
            return null
        }

        // If the view is already laid out, we can immediately compute metrics.
        if (decorView.isLaidOut && decorView.width > 0 && decorView.height > 0) {
            val insets = getSafeAreaInsets(decorView)  // Assumes this function is defined elsewhere.
            val frame = getFrame(decorView, contentView)  // Assumes this function computes frame correctly.

            if (insets != null && frame != null) {
                return mapOf(
                    "insets" to edgeInsetsToJavaMap(insets), // Converts insets to a Java-friendly map.
                    "frame" to rectToJavaMap(frame)            // Converts frame to a Java-friendly map.
                )
            } else {
                Log.w(TAG, "Insets or frame computed as null.")
            }
        } else {
            Log.w(TAG, "Decor view is not laid out or dimensions are invalid.")
        }

        return null
    }
}
