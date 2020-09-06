package com.nex3z.flowers.modelmaker.ui.camera

import android.app.Application
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nex3z.flowers.modelmaker.classifier.*
import com.nex3z.flowers.modelmaker.exception.BaseException
import com.nex3z.flowers.modelmaker.exception.ClassifierInitFailedException
import com.nex3z.flowers.modelmaker.exception.ClassifierRunFailedException
import com.nex3z.flowers.modelmaker.util.convertYuv420ToArgb8888Bitmap
import com.nex3z.flowers.modelmaker.util.getDetectThreshold
import com.nex3z.flowers.modelmaker.util.getDevice
import com.nex3z.flowers.modelmaker.util.isMultiCropEnabled
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ClassifierViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _recognition: MutableLiveData<Result?> = MutableLiveData()
    val recognition: LiveData<Result?> = _recognition

    private val _error: MutableLiveData<BaseException> = MutableLiveData()
    val error: LiveData<BaseException> = _error

//    val model: Model = MOBILE_NET_V2_INT8_MODEL
    val model: Model = MOBILE_NET_V2_FLOAT16_MODEL
    private var classifier: Classifier? = null
    private var lastAnalyzedTimestamp = 0L
    private var confidentThreshold: Float = getDetectThreshold(getApplication())
    private var scales: FloatArray = SINGLE_SCALE

    val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }

    @Synchronized
    fun initClassifier() {
        confidentThreshold = getDetectThreshold(getApplication())
        val multiCrop = isMultiCropEnabled(getApplication())
        scales = if (multiCrop) MULTI_SCALES else SINGLE_SCALE
        val device = getDevice(getApplication())
        Timber.v("initClassifier(): device = $device, confidentThreshold = $confidentThreshold, multiCrop = $multiCrop")

        classifier?.close()
        classifier = null

        executor.submit {
            try {
                classifier = Classifier(
                        context = getApplication(),
//                        model = model,
                        device = device,
                        topK = 1
                )
                Timber.v("initClassifier(): classifier initialized")
            } catch (e: Exception) {
                Timber.e(e, "initClassifier(): Failed to create classifier")
                _error.postValue(ClassifierInitFailedException(e))
            }
        }
        Timber.v("initClassifier(): complete")
    }

    fun classifyAsync(image: Bitmap) {
        executor.submit { classify(image) }
    }

    @Synchronized
    fun classify(image: Bitmap) {
        classifier?.let {
            try {
                val recognition = it.classify(image).first()
                Timber.v("classify(): recognition = $recognition")
                if (recognition.confidence >= confidentThreshold) {
                    _recognition.postValue(Result(image, recognition))
                } else {
                    _recognition.postValue(null)
                }
            } catch (e: Exception) {
                Timber.e(e, "classify():")
                _error.postValue(ClassifierRunFailedException(e))
            }
        }
    }

    @Synchronized
    fun classify(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp <= ANALYSIS_INTERVAL) {
            return
        }
        val bitmap = convertYuv420ToArgb8888Bitmap(imageProxy)
        Timber.v("classify(): Image size = ${bitmap.width} x ${bitmap.height}")

        classifier?.let {
            val recognitions = mutableListOf<Recognition>()
            val images = mutableListOf<Bitmap>()
            for (scale in scales) {
                val cropSize = (bitmap.width.coerceAtMost(bitmap.height) * scale).toInt()
                val processor = ImageProcessor.Builder()
                    .add(ResizeWithCropOrPadOp(cropSize, cropSize))
                    .build()
                val image = processor.process(TensorImage.fromBitmap(bitmap))
                images.add(image.bitmap)

                try {
                    val recognition = it.classify(image).first()
                    Timber.v("classify(): scale = $scale, recognition = $recognition")
                    recognitions.add(recognition)
                } catch (e: Exception) {
                    Timber.e(e, "classify():")
                    _error.postValue(ClassifierRunFailedException(e))
                }
            }
            val idx = recognitions.argMax()
            Timber.v("classify(): recognitions[idx] = ${recognitions[idx]}")
            if (recognitions[idx].confidence >= confidentThreshold) {
                Timber.v("classify(): recognition = ${recognitions[idx]}")
                _recognition.postValue(Result(images[idx], recognitions[idx]))
            }
        }

        Timber.v("classify(): Complete")
        lastAnalyzedTimestamp = currentTimestamp
    }

    companion object {
        private val ANALYSIS_INTERVAL: Long = java.util.concurrent.TimeUnit.SECONDS.toMillis(1)
        private val SINGLE_SCALE: FloatArray = floatArrayOf(1.0f)
        private val MULTI_SCALES: FloatArray = floatArrayOf(1.0f, 0.75f, 0.5f)
    }
}