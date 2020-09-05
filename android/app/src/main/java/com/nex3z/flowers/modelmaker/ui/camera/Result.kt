package com.nex3z.flowers.modelmaker.ui.camera

import android.graphics.Bitmap
import com.nex3z.flowers.modelmaker.classifier.Recognition

data class Result(
    val image: Bitmap,
    val recognition: Recognition
)
