package com.nex3z.flowers.modelmaker.classifier

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.nex3z.flowers.modelmaker.ml.Model
import org.tensorflow.lite.Delegate
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.image.TensorImage
import timber.log.Timber


class Classifier(
    context: Context,
    device: Device = Device.CPU,
    numThreads: Int = 4,
    private val topK: Int = 5
) {

    private val delegate: Delegate? = when(device) {
        Device.CPU -> null
        Device.NNAPI -> NnApiDelegate()
        Device.GPU -> GpuDelegate()
    }

    private val model = Model.newInstance(context)

    fun classify(bitmap: Bitmap): List<Recognition> {
        val image = TensorImage.fromBitmap(bitmap)
        return classify(image)
    }

    fun classify(image: TensorImage): List<Recognition> {
        val start = SystemClock.uptimeMillis()
        val outputs = model.process(image)
        val end = SystemClock.uptimeMillis()
        val timeCost = end - start

        val probs = outputs.probabilityAsCategoryList
        Timber.v("classify(): timeCost = $timeCost, probs = $probs")
        val recognitions: List<Recognition> = probs.map { Recognition(it.label, it.score) }
        return recognitions.getTopK(topK)
    }

    fun close() {
        model.close()
    }
}
