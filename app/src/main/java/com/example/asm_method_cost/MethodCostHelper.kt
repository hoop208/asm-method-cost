package com.example.asm_method_cost

import android.util.Log
import androidx.annotation.Keep

@Keep
object MethodCostHelper {

    private const val TAG = "MethodCostHelper"

    @Keep
    @JvmStatic
    fun log(
        className: String,
        methodName: String,
        cost: Long,
        inputParams: List<Any?>?,
        result: Any? = null,
    ) {
        val inputParamsDesc =
            if (inputParams.isNullOrEmpty()) "[void]" else inputParams.joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]"
            )
        val message = "class:$className\nmethod:$methodName\ncost:${cost}ms\ninput:${inputParamsDesc}\nreturn:${result}"
        Log.i(TAG, message)
    }

}