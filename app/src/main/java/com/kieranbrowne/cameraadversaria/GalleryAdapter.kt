package com.kieranbrowne.cameraadversaria

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.android.synthetic.main.gallery_item.view.*
import kotlinx.android.synthetic.main.recyclerview_item_row.view.*
import java.io.File

class GalleryAdapter(private val photos: ArrayList<File>, val context: android.content.Context) : RecyclerView.Adapter<GalleryAdapter.PhotoHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
        return PhotoHolder(LayoutInflater.from(context).inflate(R.layout.gallery_item, parent, false))
    }

    override fun getItemCount(): Int = photos.size

    private fun rotateBitmap(bmp : Bitmap, degrees: Float) : Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        return rotated
    }

    private fun loadRotatedBitmap(file : File) : Bitmap {
        val adversarial_image = BitmapFactory.decodeFile(file.toString())

        val exif =  android.media.ExifInterface(file.toString())
        val rot = exif.getAttribute(android.media.ExifInterface.TAG_ORIENTATION);
        if(rot == android.media.ExifInterface.ORIENTATION_ROTATE_90.toString())
            return rotateBitmap(adversarial_image, 90.toFloat());
        if(rot == android.media.ExifInterface.ORIENTATION_ROTATE_180.toString())
            return rotateBitmap(adversarial_image, 180.toFloat());
        if(rot == android.media.ExifInterface.ORIENTATION_ROTATE_270.toString())
            return rotateBitmap(adversarial_image, 270.toFloat());
        return adversarial_image
    }

    private fun loadBitmap(file: File) : Bitmap {

        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Camera Adversaria")


        val private_file = file.toString()


        try {
            // try to load filtered image but fall back if not present
            val filtered_file = File(publicDir, "adversarial_"+private_file.split("/").last())
            val adversarial_image = loadRotatedBitmap(filtered_file)

            //runModelPrediction(adversarial_image)

            return adversarial_image


        } catch (e : java.lang.Exception) {

            val private_bmp = BitmapFactory.decodeFile(private_file)

            return private_bmp
        }

    }

    override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
        holder?.imageview?.setImageBitmap(loadBitmap(photos.get(position)))
    }

    class PhotoHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imageview = view.gallery_image

    }

}