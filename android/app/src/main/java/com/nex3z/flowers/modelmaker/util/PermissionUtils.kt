package com.nex3z.flowers.modelmaker.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

val CAMERA_PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

fun hasCameraPermissions(context: Context) = CAMERA_PERMISSIONS_REQUIRED.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

fun Fragment.requestCameraPermissions(requestCode: Int) {
    requestPermissions(CAMERA_PERMISSIONS_REQUIRED, requestCode)
}
