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
import android.media.MediaScannerConnection
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Environment
import android.util.DisplayMetrics
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter
import com.kieranbrowne.cameraadversaria.AdversarialFilter
import java.lang.Exception


class MainActivity : Activity(), TextureView.SurfaceTextureListener {


    var isRearCam : Boolean = true;

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

        try {
            displayLatestImageThumb();
        } catch (e : Exception) {
        }

    }

    private fun reopenCamera() {
        if(checkPermission()) {
            if (previewSurface.isValid) {
                openCamera()
            }
        }

    }

    private fun openCamera() {
        if(checkPermission()) {

            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

            if(cameraManager.cameraIdList.size < 1) // if no cameras present
                return;

            var currentCamera = cameraManager.cameraIdList[0]

            if(!isRearCam) {
                if(cameraManager.cameraIdList.size > 1)
                    currentCamera = cameraManager.cameraIdList[1]
            }

            cameraManager.openCamera(currentCamera, object : CameraDevice.StateCallback() {
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

                            imageReader = ImageReader.newInstance(previewSize.width*12, previewSize.height*12,
                                ImageFormat.JPEG, 3)
                        }
                    }


                    val recordingSurface = imageReader?.surface

                    //surfaceView.holder.setFixedSize(800, 800);
                    previewSurface = surfaceView.holder.surface;

                    imageReader?.setOnImageAvailableListener({

                        val image = imageReader?.acquireLatestImage()

                        image?.let {
                            val buffer : ByteBuffer = it.getPlanes()[0].getBuffer()
                            val bytes = ByteArray(buffer.capacity())
                            buffer.get(bytes)
                            val bitmapImage : Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null);
                            val thumbImage : Bitmap = ThumbnailUtils.extractThumbnail(bitmapImage, 64, 64);
                            open_gallery.setImageBitmap(rotateBitmap(thumbImage, cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION).toFloat()));



                            processImage(it,
                                cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION).toFloat())
                        }

                    }, null)

                    // capture
                    val stillRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        .apply {
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

                                imageWritingSpinner.alpha = 1.0f
                                open_gallery.alpha = 0.0f

                                val anim : Animation = AlphaAnimation(.2f, 1.0f);
                                anim.setDuration(120); //You can manage the blinking time with this parameter
                                anim.setStartOffset(0);
                                anim.setFillAfter(true);
                                surfaceView.startAnimation(anim);

                                session?.capture(
                                    stillRequestBuilder?.build(),
                                    object : CameraCaptureSession.CaptureCallback() {},
                                    null
                                )
                            }

                            flip_cam.setOnClickListener {

                                //Toast.makeText(this@MainActivity,"Flip!", Toast.LENGTH_LONG).show()

                                isRearCam = !isRearCam // flip cam

                                session.abortCaptures()
                                cameraDevice.close()

                                openCamera()
                            }

                            open_gallery.setOnClickListener {
                                Toast.makeText(this@MainActivity,"Gallery!", Toast.LENGTH_LONG).show()
                                cameraDevice.close()
                                openGallery()
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

    private fun dateString(date: java.util.Date) : String {
        return java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(date)
    }

    private fun safeGetPublicDir() : File {

        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Camera Adversaria")

        if (!publicDir.exists()) publicDir.mkdirs()

        return publicDir;
    }

    private fun imageToBitmap(image: Image) : Bitmap {
        val buffer = image.planes[0].buffer
        buffer.rewind();
        val bytes = ByteArray(buffer.remaining())
        image.planes[0].buffer.get(bytes);

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size);
    }

    private fun rotateBitmap(bmp : Bitmap, degrees: Float) : Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        return rotated
    }

    private fun processImage(image: Image, rotation: Float) {

        val timeStamp = dateString(java.util.Date())

        val publicDir = safeGetPublicDir()

        // private version
        val private_file = File(filesDir, "JPEG_${timeStamp}.jpg")

        val bmp = rotateBitmap(imageToBitmap(image), 0.toFloat())

        var output: FileOutputStream? = null

            try {

                output = FileOutputStream(private_file)


                bmp.compress(Bitmap.CompressFormat.JPEG, 100, output);

                val exif =  android.media.ExifInterface(private_file.toString())
                if (rotation == 90.toFloat())
                    exif.setAttribute(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_ROTATE_90.toString())
                else if (rotation == 180.toFloat())
                    exif.setAttribute(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_ROTATE_180.toString())
                else if (rotation == 270.toFloat())
                    exif.setAttribute(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_ROTATE_270.toString())
                exif.saveAttributes()

                //Toast.makeText(this, "Writing!", Toast.LENGTH_LONG).show()
                imageWritingSpinner.alpha = 0.0f
                open_gallery.alpha = 1.0f

            } catch (e: java.io.IOException) {
                Log.e("ERROR", e.toString())
            } finally {

                val files = filesDir.listFiles();
                output?.close()
            }


            val gpuImage = GPUImage(this)
            gpuImage.setFilter(AdversarialFilter(0.0))
            gpuImage.setImage(private_file)

            val adversarial_image = gpuImage.getBitmapWithFilterApplied(bmp)

            if(gpuImage != null) {

                val adversarial_file = File(publicDir, "adversarial_"+private_file.toString().split("/").last())

                output = FileOutputStream(adversarial_file)

                adversarial_image?.let {
                    val rotatedBitmap = rotateBitmap(it, rotation)
                    rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }

                output.close()

                MediaStore.Images.Media.insertImage(getContentResolver(), adversarial_file.toString(), "Untitled" , "Camera Adversaria image");


            } else {
                Log.e("ERROR", "IT was null")
            }

            image.close();
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
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        // Invoked every time there's a new Camera preview frame.
    }


    fun displayLatestImageThumb() {

        val THUMBSIZE : Int = 64;

        val private_file = filesDir.listFiles()[filesDir.listFiles().size-1].toString()

        val thumbImage : Bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(private_file), THUMBSIZE, THUMBSIZE);


        val exif =  android.media.ExifInterface(private_file.toString())
        val rot = exif.getAttribute(android.media.ExifInterface.TAG_ORIENTATION);
        if(rot == android.media.ExifInterface.ORIENTATION_ROTATE_90.toString())
            open_gallery.setImageBitmap(rotateBitmap(thumbImage, 90.toFloat()))
        else if(rot == android.media.ExifInterface.ORIENTATION_ROTATE_180.toString())
            open_gallery.setImageBitmap(rotateBitmap(thumbImage, 180.toFloat()))
        else if(rot == android.media.ExifInterface.ORIENTATION_ROTATE_270.toString())
            open_gallery.setImageBitmap(rotateBitmap(thumbImage, 270.toFloat()))
        else
            open_gallery.setImageBitmap(thumbImage)

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

