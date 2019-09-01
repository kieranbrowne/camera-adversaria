package com.kieranbrowne.cameraadversaria

import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.android.synthetic.main.activity_main.*
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
import android.Manifest
import android.view.TextureView
import android.view.SurfaceHolder
import java.nio.ByteBuffer
import android.widget.Toast
import android.content.ContentResolver;
import android.graphics.*
import android.media.Image
import android.net.Uri
import android.os.Environment
import android.util.DisplayMetrics
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter


class MainActivity : Activity(), TextureView.SurfaceTextureListener {


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

    }

    private fun processImage(image: Image) {
        if(image != null) {

            val timeStamp: String = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())

            val file = File(filesDir, "JPEG_${timeStamp}.jpg")


            Log.d("FILENAME", file.toString());
            Log.d("NUMPLANES", image.planes.size.toString());
            val buffer = image.planes[0].buffer
            buffer.rewind();
            val bytes = ByteArray(buffer.remaining())
            image.planes[0].buffer.get(bytes);

            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size);

            Log.d("SIZE", image.width.toString() + "x" + image.height.toString());


            var output: FileOutputStream? = null

            try {

                output = FileOutputStream(file)
                //output.write(bytes)
                bmp.compress(Bitmap.CompressFormat.PNG, 100, output);

                Toast.makeText(this, "Writing!", Toast.LENGTH_LONG).show()

            } catch (e: java.io.IOException) {
                Log.e("ERROR", e.toString())
            } finally {
                Log.d("HOWMANYBYTES", bytes.size.toString())
                //val bm = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                //MediaStore.Images.Media.insertImage(getContentResolver(),bm,"pic.jpg", null);
                //bm.recycle()

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



            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            Log.d("DIR", File(path.toURI()).listFiles().size.toString())


            val gpuImage = GPUImage(this)
            gpuImage.setFilter(GPUImageSepiaToneFilter())
            gpuImage.setImage(file)

            val newBmp = gpuImage.getBitmapWithFilterApplied(bmp)

            Log.d("WIDTH",newBmp.width.toString())

            if(gpuImage != null) {

                val filtered = File(filesDir, "filtered_${timeStamp}.jpg")


                output = FileOutputStream(filtered)
                //output.write(bytes)
                newBmp?.let {
                    it.compress(Bitmap.CompressFormat.PNG, 100, output)
                }

                output.close()
                //gpuImage.saveToPictures("GPUImage", "ImageWithFilter.jpg", null)

            } else {
                Log.e("ERROR", "IT was null")
            }




            //image.close();
        } else {
            Log.e("WOAH", "image was null")
        }

    }

    private fun openCamera() {
        if(checkPermission()) {

            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val firstCamera = cameraManager.cameraIdList[0]




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

                    var imageReader: ImageReader? = null








                    cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.let { streamConfigurationMap ->
                        streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)?.let { yuvSizes ->
                            val previewSize = yuvSizes.last()

                            // cont.
                            val displayRotation = windowManager.defaultDisplay.rotation
                            val swappedDimensions = areDimensionsSwapped(displayRotation, cameraCharacteristics)// swap width and height if needed
                            val rotatedPreviewWidth = if (swappedDimensions) previewSize.height else previewSize.width
                            val rotatedPreviewHeight = if (swappedDimensions) previewSize.width else previewSize.height
                            //textureView!!.layoutParams = android.widget.FrameLayout.LayoutParams(previewSize.width, previewSize.height, android.view.Gravity.CENTER)
                            surfaceView.holder.setFixedSize(rotatedPreviewWidth, rotatedPreviewHeight)

                            imageReader = ImageReader.newInstance(rotatedPreviewWidth*12, rotatedPreviewHeight*12,
                                ImageFormat.JPEG, 1)
                        }
                    }


                    val recordingSurface = imageReader?.surface

                    //surfaceView.holder.setFixedSize(800, 800);
                    previewSurface = surfaceView.holder.surface;
                    //previewSurface.setFixedSize(800,800);

                    imageReader?.setOnImageAvailableListener({
                        // do something

                        val image = imageReader?.acquireLatestImage()

                        image?.let {
                            processImage(it)
                        }

                        openGallery()

                    }, null)

                    // capture
                    val stillRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        .apply {
                            //addTarget(previewSurface)
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
                            session.setRepeatingRequest(
                                previewRequestBuilder.build(),
                                object : CameraCaptureSession.CaptureCallback() {},
                                null
                            )
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

        }

    }

    private fun openGallery() {

        val intent = Intent(this@MainActivity, GalleryActivity::class.java)

        startActivity(intent)

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

}

