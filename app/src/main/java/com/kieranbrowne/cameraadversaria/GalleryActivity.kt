package com.kieranbrowne.cameraadversaria

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
//import com.sun.tools.corba.se.idl.Util.getAbsolutePath
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.Log
import kotlinx.android.synthetic.main.activity_gallery.*


class GalleryActivity : AppCompatActivity() {

    lateinit var bmp : Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)


        val file = filesDir.listFiles()[filesDir.listFiles().size-1].toString()

        Log.d("FILE", file)


        val myBitmap = BitmapFactory.decodeFile(file)

        val data = filesDir.listFiles()[filesDir.listFiles().size-1].readText(Charsets.UTF_8)

        val bm = BitmapFactory.decodeResource(resources, R.drawable.test);

        myBitmap?.let {
            Log.d("GALLERYSIZE",it.width.toString() +"x"+it.height.toString())
        }

        data?.let {
            Log.d("GALLERYSIZE",it)
        }



        imageView.setImageBitmap(myBitmap)
        //imageView.setImageResource(R.drawable.test);
    }

    public fun changeBMP(bitmap : Bitmap) {
        bmp = bitmap
    }
}

