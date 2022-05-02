package com.bond.bondbuddy.models

import androidx.annotation.Keep

@Keep
sealed class Response<T>(val data: T? = null, val message: String? = null){
    class Success<T>(data: T?) : Response<T>(data)
    class Failure<T>(message: String, data: T? = null): Response<T>(data, message)
    class Loading<T> : Response<T>()
}