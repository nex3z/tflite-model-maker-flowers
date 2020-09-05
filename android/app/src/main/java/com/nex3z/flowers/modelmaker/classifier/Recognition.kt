package com.nex3z.flowers.modelmaker.classifier

import java.util.*
import kotlin.Comparator
import kotlin.math.min

data class Recognition(
    val label: Int,
    val confidence: Float
)

class RecognitionQueue(
    capacity: Int
) : PriorityQueue<Recognition>(
    capacity,
    Comparator { lhs, rhs -> rhs.confidence.compareTo(lhs.confidence) }
)


fun List<Recognition>.max(): Recognition = this.maxBy(Recognition::confidence)
    ?: throw IllegalArgumentException("Cannot find max in empty list")


fun List<Recognition>.argMax(): Int {
    return this.withIndex().maxBy { it.value.confidence }?.index
        ?: throw IllegalArgumentException("Cannot find arg max in empty list")
}


fun List<Recognition>.getTopK(topK: Int): List<Recognition> {
    val pq = RecognitionQueue(topK)
    this.forEach { pq.add(it) }
    return pq.take(min(topK, pq.size))
}
