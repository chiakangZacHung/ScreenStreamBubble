package com.torrydo.testfloatingbubble.preview

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.WindowManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ScreenPreviewClient(
    private val context: Context,
    private val onFrameReceived: (Bitmap) -> Unit
) {
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isCapturing = false
    private val frameDirectory: File
    
    init {
        // Get all possible storage locations
        val externalFilesDir = context.getExternalFilesDir(null)
        val externalCacheDir = context.getExternalCacheDir()
        val filesDir = context.filesDir
        val cacheDir = context.cacheDir
        
        Log.d("ScreenPreview", """
            Storage locations:
            External files dir: ${externalFilesDir?.absolutePath}
            External cache dir: ${externalCacheDir?.absolutePath}
            Internal files dir: ${filesDir.absolutePath}
            Internal cache dir: ${cacheDir.absolutePath}
        """.trimIndent())
        
        // Use external files dir if available, otherwise fall back to internal
        frameDirectory = if (externalFilesDir != null) {
            File(externalFilesDir, "frames")
        } else {
            File(filesDir, "frames")
        }
        
        Log.d("ScreenPreview", "Using frame directory: ${frameDirectory.absolutePath}")
        Log.d("ScreenPreview", "Frame directory exists before: ${frameDirectory.exists()}")
        
        if (!frameDirectory.exists()) {
            val created = frameDirectory.mkdirs()
            Log.d("ScreenPreview", "Created frame directory: $created")
            Log.d("ScreenPreview", "Frame directory exists after: ${frameDirectory.exists()}")
            Log.d("ScreenPreview", "Frame directory can write: ${frameDirectory.canWrite()}")
            Log.d("ScreenPreview", "Frame directory can read: ${frameDirectory.canRead()}")
        }
        
        // List contents of parent directory
        frameDirectory.parentFile?.listFiles()?.forEach { 
            Log.d("ScreenPreview", "Parent dir contains: ${it.absolutePath}")
        }
        
        // Clean old frames
        val oldFrames = frameDirectory.listFiles()
        Log.d("ScreenPreview", "Found ${oldFrames?.size ?: 0} old frames")
        oldFrames?.forEach { 
            Log.d("ScreenPreview", "Deleting old frame: ${it.absolutePath}")
            val deleted = it.delete()
            Log.d("ScreenPreview", "Deleted: $deleted")
        }
    }

    fun setMediaProjection(projection: MediaProjection) {
        Log.d("ScreenPreview", "Setting media projection")
        mediaProjection = projection
        setupVirtualDisplay()
    }

    private fun setupVirtualDisplay() {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val width = display.width
        val height = display.height
        
        Log.d("ScreenPreview", "Setting up virtual display: ${width}x${height}")
        
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, context.resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
        
        Log.d("ScreenPreview", "Virtual display created: ${virtualDisplay != null}")
    }

    fun connect() {
        Log.d("ScreenPreview", "Starting frame capture...")
        isCapturing = true
        startFrameCapture()
    }

    private fun startFrameCapture() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isCapturing) {
                    Log.d("ScreenPreview", "Capturing frame...")
                    captureFrame()
                    handler.postDelayed(this, 10000) // Capture every 10 seconds for testing
                }
            }
        }, 0)
    }

    private fun captureFrame() {
        try {
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                Log.d("ScreenPreview", "Got image: ${image.width}x${image.height}")
                val width = image.width
                val height = image.height
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                
                // Save frame to file
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(frameDirectory, "frame_$timestamp.jpg")
                
                Log.d("ScreenPreview", "Saving frame to: ${file.absolutePath}")
                Log.d("ScreenPreview", "File exists before save: ${file.exists()}")
                
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                
                Log.d("ScreenPreview", "Frame saved successfully")
                Log.d("ScreenPreview", "File exists after save: ${file.exists()}")
                Log.d("ScreenPreview", "File size: ${file.length()} bytes")
                Log.d("ScreenPreview", "File can read: ${file.canRead()}")
                
                onFrameReceived(bitmap)
                image.close()
            } else {
                Log.e("ScreenPreview", "Failed to acquire image")
            }
        } catch (e: Exception) {
            Log.e("ScreenPreview", "Error capturing frame", e)
        }
    }

    fun disconnect() {
        Log.d("ScreenPreview", "Stopping frame capture...")
        isCapturing = false
        handler.removeCallbacksAndMessages(null)
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }
}
