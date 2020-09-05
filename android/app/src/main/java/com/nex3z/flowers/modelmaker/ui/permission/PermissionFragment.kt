package com.nex3z.flowers.modelmaker.ui.permission

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nex3z.flowers.modelmaker.R
import com.nex3z.flowers.modelmaker.util.hasCameraPermissions
import com.nex3z.flowers.modelmaker.util.requestCameraPermissions
import timber.log.Timber

class PermissionFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasCameraPermissions(requireContext())) {
            navigateToCamera()
        } else {
            requestCameraPermissions(RC_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.v("onRequestPermissionsResult(): Permission granted")
                navigateToCamera()
            } else {
                Toast.makeText(requireContext(), R.string.m_permission_camera_permission_denied,
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToCamera() {
        findNavController().navigate(R.id.action_permission_to_camera)
    }

    companion object {
        private const val RC_PERMISSIONS = 10
    }
}