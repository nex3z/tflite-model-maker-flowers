package com.nex3z.flowers.modelmaker.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.nex3z.flowers.modelmaker.R
import com.nex3z.flowers.modelmaker.ui.camera.Result
import kotlinx.android.synthetic.main.view_result.view.*

class ResultView(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val labels: Array<String> = context.resources.getStringArray(R.array.labels)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_result, this, true)
    }

    fun render(result: Result) {
        iv_vr_image.setImageBitmap(result.image)
        tv_vr_label.text = result.recognition.label
        tv_vr_conf.text = String.format("%.4f", result.recognition.confidence)
    }
}
