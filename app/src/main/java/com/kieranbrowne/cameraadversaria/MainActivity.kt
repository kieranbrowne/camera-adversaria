package com.kieranbrowne.cameraadversaria

import android.content.Intent
import android.app.Activity
import android.os.Bundle
import android.provider.MediaStore
//import kotlinx.android.synthetic.main.activity_main.*
import android.graphics.Bitmap
import android.view.Surface
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCaptureSession
import java.io.File
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.Manifest.permission
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.media.ImageReader
import android.graphics.ImageFormat
import android.Manifest
import android.view.TextureView
import android.graphics.SurfaceTexture


class MainActivity : Activity(), TextureView.SurfaceTextureListener {


    private var textureView: TextureView? = null
    lateinit var previewSurface: Surface




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        setTextureView() // setup texture surfaces


    }

    private fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
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

            cameraManager.openCamera(firstCamera, object : CameraDevice.StateCallback() {
                override fun onDisconnected(p0: CameraDevice) {}
                override fun onError(p0: CameraDevice, p1: Int) {}

                override fun onOpened(cameraDevice: CameraDevice) {
                    // use the camera
                    val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraDevice.id)

                    cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.let { streamConfigurationMap ->
                        streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888)?.let { yuvSizes ->
                            val previewSize = yuvSizes.last()
                            textureView!!.layoutParams = android.widget.FrameLayout.LayoutParams(previewSize.width, previewSize.height, android.view.Gravity.CENTER)
                        }
                    }

                    val captureCallback = object : CameraCaptureSession.StateCallback() {
                        override fun onConfigureFailed(session: CameraCaptureSession) {}

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
                        }
                    }

                    cameraDevice.createCaptureSession(mutableListOf(previewSurface), captureCallback, null)
                }
            }, null)

            //val previewSize = mCamera!!.parameters.previewSize
            //textureView!!.layoutParams = FrameLayout.LayoutParams(previewSize.width, previewSize.height, Gravity.CENTER)


        }

    }


    private fun setTextureView() {
        textureView = TextureView(this)
        textureView!!.surfaceTextureListener = this
        setContentView(textureView)

    }


    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {

        previewSurface = Surface(surfaceTexture)

        openCamera()

        //textureView!!.alpha = 1.0f;
        //textureView!!.rotation = 90.0f;
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


        //Log.d("TEST", cameraId)


}

