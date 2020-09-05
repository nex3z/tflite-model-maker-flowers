package com.nex3z.flowers.modelmaker.ui.camera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.behavior.SwipeDismissBehavior
import com.nex3z.flowers.modelmaker.R
import com.nex3z.flowers.modelmaker.util.hasCameraPermissions
import kotlinx.android.synthetic.main.camera_fragment.*
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CameraFragment : Fragment() {

    private lateinit var viewModel: ClassifierViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.camera_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(ClassifierViewModel::class.java)
        init()
    }

    override fun onResume() {
        super.onResume()
        if (!hasCameraPermissions(requireContext())) {
            findNavController().navigate(R.id.action_camera_to_permission)
        } else {
            bindCamera()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Timber.v("onActivityResult(): requestCode = $requestCode, data = $data")
            if (requestCode == RC_PICK_IMAGE) {
                data?.data?.let { uri ->
                    val image = if (Build.VERSION.SDK_INT < 28) {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                    } else {
                        val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    }.copy(Bitmap.Config.ARGB_8888, true)
                    viewModel.classifyAsync(image)
                }
            }
        }
    }

    private fun init() {
        initView()
        initClassifier()
        bindData()
    }

    private fun initView() {
        with (rv_cf_result.layoutParams as CoordinatorLayout.LayoutParams) {
            val swipeDismissBehavior = SwipeDismissBehavior<CardView>().apply {
                setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_START_TO_END)
                setListener(object: SwipeDismissBehavior.OnDismissListener {
                    override fun onDismiss(view: View?) {
                        cl_cf_result_container.visibility = View.GONE
                        fab_cf_pick_image.show()
                    }
                    override fun onDragStateChanged(state: Int) {}
                })
            }
            behavior = swipeDismissBehavior
        }

        btn_cf_settings.setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_settings)
        }

        fab_cf_pick_image.setOnClickListener {
            val intent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ).apply {
                type = "image/*"
            }
            val chooserIntent = Intent.createChooser(intent, getString(R.string.m_camera_pick_image))
            startActivityForResult(chooserIntent, RC_PICK_IMAGE)
        }
    }

    private fun initClassifier() {
        viewModel.initClassifier()
    }

    private fun bindData() {
        viewModel.recognition.observe(viewLifecycleOwner, Observer {
            renderResult(it)
        })
        viewModel.error.observe(viewLifecycleOwner, Observer {
            Toast.makeText(requireContext(), "[${it.code}]: ${it.message}", Toast.LENGTH_SHORT)
                .show()
        })
    }

    private fun bindCamera() = pv_cf_view_finder.post {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val metrics = DisplayMetrics().also { pv_cf_view_finder.display.getRealMetrics(it) }
            val ratio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            val rotation = pv_cf_view_finder.display.rotation
            Timber.d("bindCamera(): metrics = $metrics, ratio = $ratio, rotation = $rotation")

            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            val preview = Preview.Builder()
                .setTargetAspectRatio(ratio)
                .setTargetRotation(rotation)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(viewModel.executor, ImageAnalysis.Analyzer { image ->
                        viewModel.classify(image)
                        image.close()
                    })
                }

            cameraProvider.unbindAll()

            try {
                val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
                Timber.i("bindCamera(): sensorRotationDegrees = ${camera.cameraInfo.sensorRotationDegrees}")
                preview.setSurfaceProvider(pv_cf_view_finder.createSurfaceProvider())
            } catch (e: Exception) {
                Timber.e(e, "bindCamera(): Failed to bind use cases")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun renderResult(result: Result?) {
        if (result != null) {
            cl_cf_result_container.visibility = View.VISIBLE
            fab_cf_pick_image.hide()
            rv_cf_result.alpha = 1.0f
            rv_cf_result.render(result)
        } else {
            Toast.makeText(requireContext(), R.string.m_camera_failed_to_recognize, Toast.LENGTH_SHORT)
                .show()
        }
    }

    companion object {
        const val RC_PICK_IMAGE: Int = 20
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        fun aspectRatio(width: Int, height: Int): Int {
            val previewRatio = max(width, height).toDouble() / min(width, height)
            if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
                return AspectRatio.RATIO_4_3
            }
            return AspectRatio.RATIO_16_9
        }
    }
}