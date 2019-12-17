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

    private var index = 0



    val DIM_IMG_SIZE_X = 224
    val DIM_IMG_SIZE_Y = 224
    val DIM_PIXEL_SIZE = 3
    val DIM_BATCH_SIZE = 1


    //protected var imgData: ByteBuffer? = null




    //imgData.order(ByteOrder.nativeOrder())

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




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        model = Interpreter(loadModelFile(this@GalleryActivity))
        labels = loadLabelList(this@GalleryActivity)

        loadImage() // load the lastest image

        nextButton.setOnClickListener {

            index ++

            loadImage()

        }

        filterSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // TODO Auto-generated method stub
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // TODO Auto-generated method stub
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // TODO Auto-generated method stub

                filterSpinner.alpha = 1.0f
                val filter = FilterRunnable(this@GalleryActivity, (progress/100.0)*(progress/200.0))
                Thread(filter).start()

            }
        })
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
        Log.d("SIZE",intValues.size.toString())
        Log.d("SIZE",bitmap.width.toString())

        var idx = 0

        val IMG_MEAN = 128.toFloat()
        val IMG_STD = 128.0f
        for (x in 0..DIM_IMG_SIZE_X-1) {
            for (y in 0..DIM_IMG_SIZE_Y-1) {
                val v: Int = intValues[idx++]
                //Log.d("idx", idx.toString())
                imgData.putFloat( (((v shr 16) and 0xFF) - IMG_MEAN)/IMG_STD )
                imgData.putFloat( (((v shr 8) and 0xFF) - IMG_MEAN)/IMG_STD )
                imgData.putFloat( (((v) and 0xFF) - IMG_MEAN)/IMG_STD )
            }
        }

        return imgData
    }

    private fun runModelPrediction(bmp : Bitmap) {
        val croppedBitmap = Bitmap.createBitmap(bmp, 0, 0, kotlin.math.min(bmp.width,bmp.height), kotlin.math.min(bmp.width,bmp.height), null, false)

        val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap,DIM_IMG_SIZE_X, DIM_IMG_SIZE_X, false)
        //croppedBitmap
        

        val result = Array(1) { FloatArray(1001) }

        model = Interpreter(loadModelFile(this@GalleryActivity))

        model?.run(loadBitmapAsByteBuffer(scaledBitmap),result) // run prediction over bitmap

        model?.close()

        //Log.d("Vals", result[0].joinToString())

        var biggest: Float = 0.0.toFloat()
        var biggestidx = 0
        for(i in 0.until(result[0].size)) {
            if(result[0][i] > biggest) {
                biggest = result[0][i]
                biggestidx = i
            }
        }

        Log.d("biggest", "Prediction is "+labels?.get(biggestidx).toString() +" "+biggest.toString() )


        predictedClass.setText(labels?.get(biggestidx).toString() + " " + "%.2f".format(biggest*100.0)+"%")
    }


    private fun loadImage() {

        //val path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
        //Log.d("DIR", File(path.toURI()).listFiles().size.toString())

        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Camera Adversaria")


        val file = publicDir.listFiles()[publicDir.listFiles().size - index -1].toString()


        try {
            val myBitmap = BitmapFactory.decodeFile(file)

            //val data = filesDir.listFiles()[filesDir.listFiles().size-1].readText(Charsets.UTF_8)

            myBitmap?.let {
                Log.d("GALLERYSIZE",it.width.toString() +"x"+it.height.toString())
            }

            runModelPrediction(myBitmap)


            imageView.setImageBitmap(myBitmap)


        } catch (e : java.lang.Exception) {
            Log.d("FUCK", "1")

            val file = filesDir.listFiles()[filesDir.listFiles().size - index -1].toString()

            val myBitmap = BitmapFactory.decodeFile(file)

            imageView.setImageBitmap(myBitmap)
        }


        //filterImage()

    }

    inner class FilterRunnable(context: Context, amp: Double) : Runnable {

        val context = context
        val amp = amp

        private fun filterImage() {



            var output: FileOutputStream? = null

            val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Camera Adversaria")

            if (!publicDir.exists()) {
                publicDir.mkdirs()
            }


            val file = filesDir.listFiles()[filesDir.listFiles().size - index -1].toString()
            val bitmap = BitmapFactory.decodeFile(file)

            val gpuImage = GPUImage(context)
            gpuImage.setFilter(AdversarialFilter(amp))
            //gpuImage.setImage(file)

            val filteredBitmap = gpuImage.getBitmapWithFilterApplied(bitmap)

            //MediaStore.Images.Media.insertImage(getContentResolver(), filteredBitmap, "Hello" , "Test Desc");


            if(gpuImage != null) {

                val filtered = File(publicDir, "adversarial_"+file.split("/").last())

                /*val uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "com.kieranbrowne.cameraadversaria.fileprovider",
                    filtered)

                Log.d("URI", uri.toString())*/

                //resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)



                output = FileOutputStream(filtered)
                //output.write(bytes)
                filteredBitmap?.let {
                    it.compress(Bitmap.CompressFormat.PNG, 100, output)
                }

                output.close()
                //gpuImage.saveToPictures("GPUImage", "ImageWithFilter.jpg", null)

                //MediaStore.Images.Media.insertImage(getContentResolver(), filtered.toString(), "Title" , "yo");

                //MediaStore.Images.Media.insertImage(ContentResolver cr, String imagePath, String name, String description)



                imageView.setImageBitmap(filteredBitmap)

            } else {
                Log.e("ERROR", "IT was null")
            }

            runOnUiThread({
                filterSpinner.alpha = 0.0f
                loadImage()
            })


            Log.d("DONE", "done")


        }

        override fun run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

            filterImage()

        }

    }

}

