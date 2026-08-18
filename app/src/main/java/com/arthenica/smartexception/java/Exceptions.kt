package com.arthenica.smartexception.java

import android.util.Log

/**
 * Compatibility shim for ffmpeg-kit builds expecting smart-exception-java.
 */
class Exceptions private constructor() {
    companion object {
        @Volatile
        private var rootPackage: String = ""

        @JvmStatic
        fun getStackTraceString(throwable: Throwable?): String {
            return if (throwable == null) "" else Log.getStackTraceString(throwable)
        }

        @JvmStatic
        fun registerRootPackage(value: String?) {
            rootPackage = value ?: ""
        }

        @JvmStatic
        fun getRootPackage(): String {
            return rootPackage
        }
    }
}
