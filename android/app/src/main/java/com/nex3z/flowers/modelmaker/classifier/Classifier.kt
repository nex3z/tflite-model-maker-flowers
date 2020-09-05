package com.nex3z.flowers.modelmaker.classifier

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Delegate
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.DequantizeOp
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.QuantizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber
import java.io.Closeable


class Classifier(
    context: Context,
    private val model: Model,
    device: Device = Device.CPU,
    numThreads: Int = 4,
    private val topK: Int = 5
) {

    private val delegate: Delegate? = when(device) {
        Device.CPU -> null
        Device.NNAPI -> NnApiDelegate()
        Device.GPU -> GpuDelegate()
    }

    private val interpreter: Interpreter = Interpreter(
        FileUtil.loadMappedFile(context, model.fileName),
        Interpreter.Options().apply {
            setNumThreads(numThreads)
            delegate?.let { addDelegate(it) }
        }
    )

    private val inputBuffer: TensorImage =
        with(interpreter.getInputTensor(model.inputTensorIndex)) {
            Timber.v("[input] shape = ${shape()?.contentToString()} dataType = ${dataType()}")
            TensorImage(dataType())
        }

    private val outputBuffer: TensorBuffer =
        with(interpreter.getOutputTensor(model.outputTensorIndex)) {
            Timber.v("[outout] shape = ${shape()?.contentToString()}, dataType = ${dataType()}")
            TensorBuffer.createFixedSize(shape(), dataType())
        }

    private val quantPreProcess: ImageProcessor? =
            with(interpreter.getInputTensor(model.inputTensorIndex).quantizationParams()) {
                Timber.v("[input] scale = ${scale}, zeroPoint = $zeroPoint")
                if (scale > 0) {
                    Timber.v("Adding quant pre-processor")
                    ImageProcessor.Builder()
                            .add(QuantizeOp(zeroPoint.toFloat(), scale))
                            .add(CastOp(DataType.UINT8))
                            .build()
                } else {
                    null
                }
            }

    private val quantPostProcessor: TensorProcessor? =
            with(interpreter.getOutputTensor(model.inputTensorIndex).quantizationParams()) {
                Timber.v("[output] scale = ${scale}, zeroPoint = $zeroPoint")
                if (scale > 0) {
                   Timber.v("Adding quant post-processor")
                   TensorProcessor.Builder()
                            .add(DequantizeOp(zeroPoint.toFloat(), scale))
                            .build()
                } else {
                    null
                }
            }

    fun classify(bitmap: Bitmap): List<Recognition> {
        inputBuffer.load(bitmap)
        return classify(inputBuffer)
    }

    fun classify(image: TensorImage): List<Recognition> {
        var input = model.imageProcessor.process(image)
        quantPreProcess?.let {
            input = it.process(input)
        }

        val start = SystemClock.uptimeMillis()
        interpreter.run(input.buffer, outputBuffer.buffer.rewind())
        val end = SystemClock.uptimeMillis()
        val timeCost = end - start

        val output = quantPostProcessor?.run { process(outputBuffer) } ?: outputBuffer
        val probs = output.floatArray
        Timber.v("classify(): timeCost = $timeCost, probs = ${probs.contentToString()}")
        val recognitions: List<Recognition> = probs.mapIndexed(::Recognition)
        return recognitions.getTopK(topK)
    }

    fun close() {
        interpreter.close()
        if (delegate is Closeable) {
            delegate.close()
        }
    }
}
