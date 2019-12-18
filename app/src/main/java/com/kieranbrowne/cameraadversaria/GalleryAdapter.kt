package com.kieranbrowne.cameraadversaria

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Environment
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import jp.co.cyberagent.android.gpuimage.GPUImage
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.android.synthetic.main.gallery_item.view.*
import java.io.File
import java.io.FileOutputStream

class GalleryAdapter(private val photos: ArrayList<File>, val context: android.content.Context) : RecyclerView.Adapter<GalleryAdapter.PhotoHolder>() {

    val parentcontext = context

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

        // display image
        val loadImage : (()->Unit) = {
            holder.imageview?.setImageBitmap(loadBitmap(photos.get(position)))
            holder.filterSpinner.alpha = 0.0f
        }
        loadImage()

        // deal with filtering
        holder.filterSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // TODO Auto-generated method stub
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // TODO Auto-generated method stub
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // TODO Auto-generated method stub

                holder.filterSpinner.alpha = 1.0f
                val filter = FilterRunnable(context, photos.get(position), loadImage, (progress/100.0)*(progress/200.0))
                Thread(filter).start()

            }
        })
    }




    class PhotoHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imageview = view.gallery_image
        val filterSpinner = view.filterSpinner
        val filterSeekBar = view.filterSeekBar



    }

    inner class FilterRunnable(context: Context, file: File, callback: ()->Unit, amp: Double) : Runnable {

        val context = context
        val amp = amp

        val file = file
        val callback = callback

        private fun filterImage() {



            var output: FileOutputStream? = null



            val private_file = file.toString()
            val bitmap = BitmapFactory.decodeFile(private_file)

            val gpuImage = GPUImage(context)
            gpuImage.setFilter(AdversarialFilter(amp))
            //gpuImage.setImage(file)

            val filteredBitmap = gpuImage.getBitmapWithFilterApplied(bitmap)

            //MediaStore.Images.Media.insertImage(getContentResolver(), filteredBitmap, "Hello" , "Test Desc");


            if(gpuImage != null) {

                val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Camera Adversaria")
                val filtered = File(publicDir, "adversarial_"+private_file.split("/").last())


                output = FileOutputStream(filtered)

                filteredBitmap?.let {
                    it.compress(Bitmap.CompressFormat.JPEG, 100, output)
                }

                val originalexif = android.media.ExifInterface(private_file.toString())

                val exif =  android.media.ExifInterface(filtered.toString())
                exif.setAttribute(android.media.ExifInterface.TAG_ORIENTATION, originalexif.getAttribute(android.media.ExifInterface.TAG_ORIENTATION))
                exif.saveAttributes()

                output.close()

            } else {
                Log.e("ERROR", "IT was null")
            }

            callback()


            /*runOnUiThread({

                //loadImage()
            })*/

        }

        override fun run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

            filterImage()

        }

    }


}

