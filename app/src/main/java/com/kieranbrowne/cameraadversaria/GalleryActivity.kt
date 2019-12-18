package com.kieranbrowne.cameraadversaria

import android.app.Activity
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
//import com.sun.tools.corba.se.idl.Util.getAbsolutePath
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import jp.co.cyberagent.android.gpuimage.GPUImage
import kotlinx.android.synthetic.main.activity_gallery.*
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.widget.Toast
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener





class GalleryActivity : AppCompatActivity() {


    val photos: ArrayList<File> = ArrayList()



    private fun addPhotos() {
        for (f in filesDir.listFiles().reversed()) {
            photos.add(f)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)


        addPhotos()

        gallery_list.layoutManager = android.support.v7.widget.LinearLayoutManager(this,  android.support.v7.widget.LinearLayoutManager.HORIZONTAL, false)

        gallery_list.adapter = GalleryAdapter(photos, this, this)


    }


}

