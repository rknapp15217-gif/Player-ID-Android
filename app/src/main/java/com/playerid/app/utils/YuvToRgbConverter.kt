/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.playerid.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import java.nio.ByteBuffer

/**
 * Official Google sample implementation of a YUV to RGB converter using RenderScript.
 * This is a high-performance, GPU-accelerated converter.
 * This version has been modified to be robust to image size changes.
 */
class YuvToRgbConverter(context: Context) {

    private val rs = RenderScript.create(context)
    private val script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    private var yuvToRgbIntrinsicIn: Allocation? = null
    private var yuvToRgbIntrinsicOut: Allocation? = null
    private var lastImageWidth: Int = 0
    private var lastImageHeight: Int = 0

    @Synchronized
    fun yuvToRgb(image: Image, output: Bitmap) {
        val yuvBytes = image.toNv21()

        // If the image's dimensions have changed, we need to re-initialize our buffers.
        if (yuvToRgbIntrinsicIn == null || image.width != lastImageWidth || image.height != lastImageHeight) {
            lastImageWidth = image.width
            lastImageHeight = image.height

            val yuvType = Type.Builder(rs, Element.U8(rs)).setX(yuvBytes.size)
            yuvToRgbIntrinsicIn = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)
            
            val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(image.width).setY(image.height)
            yuvToRgbIntrinsicOut = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT)
        }

        yuvToRgbIntrinsicIn!!.copyFrom(yuvBytes)
        script.setInput(yuvToRgbIntrinsicIn)
        script.forEach(yuvToRgbIntrinsicOut)
        yuvToRgbIntrinsicOut!!.copyTo(output)
    }

    private fun Image.toNv21(): ByteArray {
        val yBuffer = planes[0].buffer.asReadOnlyBuffer()
        val uBuffer = planes[1].buffer.asReadOnlyBuffer()
        val vBuffer = planes[2].buffer.asReadOnlyBuffer()

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }
}