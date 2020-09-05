package com.nex3z.flowers.modelmaker.classifier

import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp

data class Model(
    val fileName: String,
    val imageProcessor: ImageProcessor,
    val inputTensorIndex: Int = 0,
    val outputTensorIndex: Int = 0,
    val version: String = ""
)

val MOBILE_NET_V2_FLOAT16_MODEL: Model = Model(
    fileName = "mobile_net_v2_float_16.tflite",
    imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
        .add(NormalizeOp(127.5f, 127.5f))
        .build(),
    inputTensorIndex = 0,
    outputTensorIndex = 0,
    version = "mobile_net_v2_1_4_float16"
)

val MOBILE_NET_V2_INT8_MODEL: Model = Model(
    fileName = "mobile_net_v2_int_8.tflite",
    imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(127.5f, 127.5f))
            .build(),
    inputTensorIndex = 0,
    outputTensorIndex = 0,
    version = "mobile_net_v2_1_4_int_8"
)
