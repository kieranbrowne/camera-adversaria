package com.kieranbrowne.cameraadversaria

import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.android.synthetic.main.activity_main.*
import android.graphics.Bitmap
import android.view.Surface
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCaptureSession
import java.io.File
import java.io.FileOutputStream
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.Manifest.permission
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.media.ImageReader
import android.graphics.ImageFormat
import android.Manifest
import android.view.TextureView
import android.graphics.SurfaceTexture
import android.view.SurfaceHolder
import java.nio.ByteBuffer
import android.widget.Toast
import android.content.ContentResolver;


class MainActivity : Activity(), TextureView.SurfaceTextureListener {


    private var textureView: TextureView? = null
    lateinit var previewSurface: Surface




    val surfaceReadyCallback = object: SurfaceHolder.Callback {
        override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) { }
        override fun surfaceDestroyed(p0: SurfaceHolder?) { }

        override fun surfaceCreated(p0: SurfaceHolder?) {
            openCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView.holder.addCallback(surfaceReadyCallback)




        //setTextureView() // setup texture surfaces



    }

    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                123)
            return false
        } else {
            return true
        }

    }

    private fun openCamera() {
        if(checkPermission()) {

            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val firstCamera = cameraManager.cameraIdList[0]

            val imageReader = ImageReader.newInstance(1080, 1920,
                ImageFormat.YUV_420_888, 1)

            val recordingSurface = imageReader.surface

            //surfaceView.holder.setFixedSize(800, 800);
            previewSurface = surfaceView.holder.surface;

            imageReader.setOnImageAvailableListener({
                // do something

                val image = imageReader.acquireLatestImage()


                if(image != null) {

                    val timeStamp: String = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())

                    val file = File(filesDir, "JPEG_${timeStamp}.jpg")


                    //val uri = android.net.Uri.fromFile(file)

                    val uri = FileProvider.getUriForFile(
                        this, "com.kieranbrowne.cameraadversaria.provider",
                        file
                    )



                    Log.d("FILENAME", file.toString());
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())

                    var output: FileOutputStream? = null

                    try {

                        output = FileOutputStream(file).apply {
                            this.write(bytes)
                        }
                        Toast.makeText(this, "Writing!", Toast.LENGTH_LONG).show()

                    } catch (e: java.io.IOException) {
                        Log.e("ERROR", e.toString())
                    } finally {
                        Log.d("HOWMANYBYTES", bytes.size.toString())
                        val bm = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        MediaStore.Images.Media.insertImage(getContentResolver(),bm,"pic.jpg", null);
                        bm.recycle()

                        val files = filesDir.listFiles();
                        Log.d("TEST", "Size: "+ files.size.toString());
                        image.close()
                        output?.let {
                            try {
                                it.close()

                            } catch (e: java.io.IOException) {
                                Log.e("ERROR", e.toString())
                            }
                        }
                    }
                }

            }, null)



            cameraManager.openCamera(firstCamera, object : CameraDevice.StateCallback() {
                override fun onDisconnected(cameraDevice: CameraDevice) {
                    cameraDevice.close()
                }
                override fun onError(cameraDevice: CameraDevice, error: Int) {
                    cameraDevice.close()
                }

                override fun onOpened(cameraDevice: CameraDevice) {
                    // use the camera
                    val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraDevice.id)


                    cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.let { streamConfigurationMap ->
                        streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888)?.let { yuvSizes ->
                            val previewSize = yuvSizes.last()

                            // cont.
                            val displayRotation = windowManager.defaultDisplay.rotation
                            val swappedDimensions = areDimensionsSwapped(displayRotation, cameraCharacteristics)// swap width and height if needed
                            val rotatedPreviewWidth = if (swappedDimensions) previewSize.height else previewSize.width
                            val rotatedPreviewHeight = if (swappedDimensions) previewSize.width else previewSize.height
                            //textureView!!.layoutParams = android.widget.FrameLayout.LayoutParams(previewSize.width, previewSize.height, android.view.Gravity.CENTER)
                            surfaceView.holder.setFixedSize(rotatedPreviewWidth, rotatedPreviewHeight)
                        }
                    }

                    // capture
                    val stillRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        .apply {
                            addTarget(previewSurface)
                            addTarget(recordingSurface)
                        }

                    /// preview
                    val captureCallback = object : CameraCaptureSession.StateCallback() {
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                        }

                        override fun onConfigured(session: CameraCaptureSession) {
                            // session configured
                            val previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                .apply {
                                    addTarget(previewSurface)
                                    //addTarget(recordingSurface)
                                }
                            /*session.setRepeatingRequest(
                                previewRequestBuilder.build(),
                                object : CameraCaptureSession.CaptureCallback() {},
                                null
                            )*/
                            session?.capture(
                                previewRequestBuilder?.build(),
                                object : CameraCaptureSession.CaptureCallback() {},
                                null
                            )


                            start_cam.setOnClickListener {

                                session?.capture(
                                    stillRequestBuilder?.build(),
                                    object : CameraCaptureSession.CaptureCallback() {},
                                    null
                                )

                                Toast.makeText(this@MainActivity,"Shoot!", Toast.LENGTH_LONG).show()
                            }
                        }
                    }



                    cameraDevice.createCaptureSession(mutableListOf(previewSurface, recordingSurface), captureCallback, null)
                }
            }, null)

            //val previewSize = mCamera!!.parameters.previewSize
            //textureView!!.layoutParams = FrameLayout.LayoutParams(previewSize.width, previewSize.height, Gravity.CENTER)

        }

    }

    private fun captureStill() {


    }

    private fun areDimensionsSwapped(displayRotation: Int, cameraCharacteristics: CameraCharacteristics): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 90 || cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 270) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 0 || cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 180) {
                    swappedDimensions = true
                }
            }
            else -> {
                // invalid display rotation
            }
        }
        return swappedDimensions
    }

    private fun setTextureView() {
        textureView = TextureView(this)
        textureView!!.surfaceTextureListener = this
        setContentView(textureView)
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {

        previewSurface = Surface(surfaceTexture)

        openCamera()

    }


    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
        // Ignored, Camera does all the work for us
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        //mCamera!!.stopPreview()
        //mCamera!!.release()
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        // Invoked every time there's a new Camera preview frame.
    }

}

