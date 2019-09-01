package com.kieranbrowne.cameraadversaria

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
//import com.sun.tools.corba.se.idl.Util.getAbsolutePath
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import kotlinx.android.synthetic.main.activity_gallery.*
import java.io.File


class GalleryActivity : AppCompatActivity() {

    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        loadImage()

        nextButton.setOnClickListener {

            index ++

            loadImage();

        }
    }

    private fun loadImage() {

        //val path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
        //Log.d("DIR", File(path.toURI()).listFiles().size.toString())

        val file = filesDir.listFiles()[filesDir.listFiles().size - index -1].toString()


        Log.d("FILE", file)


        val myBitmap = BitmapFactory.decodeFile(file)

        val data = filesDir.listFiles()[filesDir.listFiles().size-1].readText(Charsets.UTF_8)

        val bm = BitmapFactory.decodeResource(resources, R.drawable.test);

        myBitmap?.let {
            Log.d("GALLERYSIZE",it.width.toString() +"x"+it.height.toString())
        }

        data?.let {
            //Log.d("GALLERYSIZE",it)
        }



        imageView.setImageBitmap(myBitmap)
        //imageView.setImageResource(R.drawable.test);

    }

}

