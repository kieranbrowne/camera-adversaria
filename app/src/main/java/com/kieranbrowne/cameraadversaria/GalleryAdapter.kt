package com.kieranbrowne.cameraadversaria

import android.app.Activity
import android.os.AsyncTask
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
import android.widget.AutoCompleteTextView
import android.widget.SeekBar
import jp.co.cyberagent.android.gpuimage.GPUImage
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.android.synthetic.main.gallery_item.view.*
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class GalleryAdapter(private val photos: ArrayList<File>, val context: android.content.Context, val activity: Activity) : RecyclerView.Adapter<GalleryAdapter.PhotoHolder>() {

    val parentcontext = context


    val DIM_IMG_SIZE_X = 224
    val DIM_IMG_SIZE_Y = 224
    val DIM_PIXEL_SIZE = 3
    val DIM_BATCH_SIZE = 1

    private var model: Interpreter? = null
    private var labels: List<String>? = null

    @Throws(IOException::class)
    private fun loadLabelList(activity: Activity): List<String> {
        val labels = ArrayList<String>()
        val reader = BufferedReader(InputStreamReader(activity.assets.open("labels_mobilenet_quant_v1_224.txt")))
        var line: String?
        while (true) {
            line = reader.readLine()
            if(line == null) break
            labels.add(line)
        }
        reader.close()
        return labels
    }


    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        val MODEL_PATH = "mobilenet_v2_1.0_224.tflite"


        val fileDescriptor = activity.assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun loadBitmapAsByteBuffer(bitmap: Bitmap) : ByteBuffer {

        var imgData: ByteBuffer = ByteBuffer.allocateDirect(
            DIM_BATCH_SIZE
                    * DIM_IMG_SIZE_X
                    * DIM_IMG_SIZE_Y
                    * DIM_PIXEL_SIZE
                    * 4) // floats are 4 bytes each

        imgData.order(ByteOrder.nativeOrder())

        imgData.rewind()

        val intValues = IntArray(bitmap.width*bitmap.height)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var idx = 0

        val IMG_MEAN = 128.toFloat()
        val IMG_STD = 128.0f
        for (x in 0..DIM_IMG_SIZE_X-1) {
            for (y in 0..DIM_IMG_SIZE_Y-1) {
                val v: Int = intValues[idx++]

                imgData.putFloat( (((v shr 16) and 0xFF) - IMG_MEAN)/IMG_STD )
                imgData.putFloat( (((v shr 8) and 0xFF) - IMG_MEAN)/IMG_STD )
                imgData.putFloat( (((v) and 0xFF) - IMG_MEAN)/IMG_STD )
            }
        }


        return imgData
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {


        model = Interpreter(loadModelFile(activity))


        labels = loadLabelList(activity)


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


        val private_file = file


        try {
            // try to load filtered image but fall back if not present
            val filtered_file = File(publicDir, "adversarial_"+private_file.toString().split("/").last())
            val adversarial_image = loadRotatedBitmap(filtered_file)

            //runModelPrediction(adversarial_image)

            return adversarial_image


        } catch (e : java.lang.Exception) {

            val private_bmp = loadRotatedBitmap(private_file)

            return private_bmp
        }

    }

    override fun onBindViewHolder(holder: PhotoHolder, position: Int) {

        holder.loadImageAsync(photos.get(position)).execute()

        holder.predictAsync(loadBitmap(photos.get(position))).execute()

        // deal with filtering
        holder.filterSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                holder.filterSpinner.alpha = 1.0f
                holder.filterAsync(context, photos.get(position), (progress/100.0)*(progress/200.0)).execute()

            }
        })
    }




    inner class PhotoHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imageview = view.gallery_image
        val filterSpinner = view.filterSpinner
        val filterSeekBar = view.filterSeekBar
        val predictedClass = view.predictedClass


        inner class loadImageAsync(file: File) : AsyncTask<Void, Void, Bitmap>() {

            val file = file

            override fun doInBackground(vararg params: Void?): Bitmap? {

                return loadBitmap(file)

            }

            override fun onPreExecute() {
                super.onPreExecute()
            }

            override fun onPostExecute(result: Bitmap) {
                super.onPostExecute(result)
                imageview.setImageBitmap(result)
            }

        }

        inner class filterAsync(context: Context, file: File, amp : Double) : AsyncTask<Void, Void, Bitmap>() {

            val amp = amp
            val private_file = file.toString()
            val context = context
            val file = file

            override fun doInBackground(vararg params: Void?): Bitmap? {
                val gpuImage = GPUImage(context)
                gpuImage.setFilter(AdversarialFilter(amp,Math.random()))
                // ...

                val bitmap = BitmapFactory.decodeFile(private_file)
                val filteredBitmap = gpuImage.getBitmapWithFilterApplied(bitmap)

                try {

                    val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Camera Adversaria")
                    val filtered = File(publicDir, "adversarial_"+private_file.split("/").last())


                    val output = FileOutputStream(filtered)

                    filteredBitmap?.let {
                        it.compress(Bitmap.CompressFormat.JPEG, 100, output)
                    }

                    val originalexif = android.media.ExifInterface(private_file.toString())

                    val exif =  android.media.ExifInterface(filtered.toString())
                    exif.setAttribute(android.media.ExifInterface.TAG_ORIENTATION, originalexif.getAttribute(android.media.ExifInterface.TAG_ORIENTATION))
                    exif.saveAttributes()

                    output.close()

                } catch (e : Exception) {

                }

                return filteredBitmap
            }

            override fun onPreExecute() {
                super.onPreExecute()
                // ...
            }

            override fun onPostExecute(result: Bitmap) {
                super.onPostExecute(result)
                // ...

                val exif =  android.media.ExifInterface(file.toString())
                val rot = exif.getAttribute(android.media.ExifInterface.TAG_ORIENTATION);
                if(rot == android.media.ExifInterface.ORIENTATION_ROTATE_90.toString())
                    imageview.setImageBitmap(rotateBitmap(result, 90.toFloat()))
                else if(rot == android.media.ExifInterface.ORIENTATION_ROTATE_180.toString())
                    imageview.setImageBitmap(rotateBitmap(result, 180.toFloat()))
                else if(rot == android.media.ExifInterface.ORIENTATION_ROTATE_270.toString())
                    imageview.setImageBitmap(rotateBitmap(result, 270.toFloat()))
                else imageview.setImageBitmap(result)

                filterSpinner.alpha = 0.0f

                predictAsync(loadBitmap(file)).execute()
            }

        }




        inner class predictAsync(bmp: Bitmap) : AsyncTask<Void, Void, String>() {

            val bmp = bmp

            override fun doInBackground(vararg params: Void?): String? {


                val croppedBitmap = Bitmap.createBitmap(bmp, 0, 0, kotlin.math.min(bmp.width,bmp.height), kotlin.math.min(bmp.width,bmp.height), null, false)

                val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap,DIM_IMG_SIZE_X, DIM_IMG_SIZE_X, false)
                //croppedBitmap


                val result = Array(1) { FloatArray(1001) }

                model = Interpreter(loadModelFile(activity))

                model?.run(loadBitmapAsByteBuffer(scaledBitmap),result) // run prediction over bitmap

                model?.close()


                var biggest: Float = 0.0.toFloat()
                var biggestidx = 0
                for(i in 0.until(result[0].size)) {
                    if(result[0][i] > biggest) {
                        biggest = result[0][i]
                        biggestidx = i
                    }
                }


                // ...
                return labels?.get(biggestidx).toString() + " " + "%.2f".format(biggest*100.0)+"%"
            }

            override fun onPreExecute() {
                super.onPreExecute()
                // ...
            }

            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)
                // ...
                predictedClass.setText(result)
            }
        }



    }


}

