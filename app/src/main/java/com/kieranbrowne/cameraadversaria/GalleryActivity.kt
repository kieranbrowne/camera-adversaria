package com.kieranbrowne.cameraadversaria

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_gallery.*
import java.io.*

class GalleryActivity : AppCompatActivity() {

    val photos: ArrayList<File> = ArrayList()

    private fun addPhotos() {
        for (f in filesDir.listFiles().reversed()) {
            if(f.toString().contains("""JPEG_\d+_\d+.jpg""".toRegex()))
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

