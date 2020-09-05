package com.nex3z.flowers.modelmaker.exception


const val CODE_CLASSIFIER_INIT_FAILED: Int = 10001
const val CODE_CLASSIFIER_RUN_FAILED: Int = 10002


abstract class BaseException(
    val code: Int,
    message: String,
    cause: Throwable
) : RuntimeException(message, cause)
