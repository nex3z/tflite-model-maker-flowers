package com.nex3z.flowers.modelmaker.util

import android.content.Context
import androidx.preference.PreferenceManager
import com.nex3z.flowers.modelmaker.R
import com.nex3z.flowers.modelmaker.classifier.Device


fun getBooleanValue(context: Context, key: String, default: Boolean = false): Boolean {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    return sharedPref.getBoolean(key, default)
}

fun getStringValue(context: Context, key: String, default: String?): String? {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    return sharedPref.getString(key, default)
}

fun getIntValue(context: Context, key: String, default: Int = 0): Int {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
    return sharedPref.getInt(key, default)
}

fun isMultiCropEnabled(context: Context): Boolean =
    getBooleanValue(context, context.getString(R.string.key_settings_multi_crop_enabled), false)

fun getDetectThreshold(context: Context): Float =
    getIntValue(context, context.getString(R.string.key_settings_confidence_threshold), 30) / 100.0f

fun getDevice(context: Context): Device =
    when (getStringValue(context, context.getString(R.string.key_settings_device), "cpu")) {
        "cpu" -> Device.CPU
        "gpu" -> Device.GPU
        "nnapi" -> Device.NNAPI
        else -> Device.CPU
    }
