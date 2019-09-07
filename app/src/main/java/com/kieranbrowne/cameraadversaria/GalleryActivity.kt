package com.kieranbrowne.cameraadversaria

import android.app.Activity
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
//import com.sun.tools.corba.se.idl.Util.getAbsolutePath
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import kotlinx.android.synthetic.main.activity_gallery.*
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel



class GalleryActivity : AppCompatActivity() {

    private var index = 0



    val DIM_IMG_SIZE_X = 224
    val DIM_IMG_SIZE_Y = 224
    val DIM_PIXEL_SIZE = 3
    val DIM_BATCH_SIZE = 1


    //protected var imgData: ByteBuffer? = null




    //imgData.order(ByteOrder.nativeOrder())

    private var tflite: Interpreter? = null
    private var labels: List<String>? = null
    private var labelProbArray: FloatArray = FloatArray(1001)

    var result = Array(1) { FloatArray(1001) }

    var imgData: ByteBuffer = ByteBuffer.allocateDirect(
        DIM_BATCH_SIZE
                * DIM_IMG_SIZE_X
                * DIM_IMG_SIZE_Y
                * DIM_PIXEL_SIZE
                * 4)


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

        tflite = Interpreter(loadModelFile(this@GalleryActivity))
        labels = loadLabelList(this@GalleryActivity)

        loadImage()

        nextButton.setOnClickListener {

            index ++

            loadImage()

        }
    }

    private fun predict() {

        tflite?.run(imgData, result)

    }

    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        val MODEL_PATH = "mobilenet_v2_1.0_224.tflite"
        Log.d("working directly before", "this")
        Log.d("dir", activity.assets.list("./").joinToString())

        val fileDescriptor = activity.getAssets().openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun loadBitmapAsByteBuffer(bitmap: Bitmap) {
        imgData.rewind()

        val intValues = IntArray(bitmap.width*bitmap.height)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        Log.d("SIZE",intValues.size.toString())
        Log.d("SIZE",bitmap.width.toString())

        var idx = 0

        val IMG_MEAN = 128
        val IMG_STD = 128.0.toFloat()
        for (x in 0..DIM_IMG_SIZE_X-1) {
            for (y in 0..DIM_IMG_SIZE_Y-1) {
                val v: Int = intValues[idx++]
                //Log.d("idx", idx.toString())
                imgData.putFloat( (((v shr 16) and 0xFF) - IMG_MEAN)/IMG_STD )
                imgData.putFloat( (((v shr 8) and 0xFF) - IMG_MEAN)/IMG_STD )
                imgData.putFloat( (((v) and 0xFF) - IMG_MEAN)/IMG_STD )
            }
        }


    }

    private fun loadImage() {

        //val path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES))
        //Log.d("DIR", File(path.toURI()).listFiles().size.toString())

        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Camera Adversaria")


        val file = publicDir.listFiles()[publicDir.listFiles().size - index -1].toString()


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


        Log.d("Vals", result.joinToString())

        val croppedBitmap = Bitmap.createBitmap(myBitmap, 0, 0, DIM_IMG_SIZE_X, DIM_IMG_SIZE_X, null, false)

        //croppedBitmap

        loadBitmapAsByteBuffer(croppedBitmap) // load the current image
        predict()
        Log.d("Vals", result.joinToString())

        imageView.setImageBitmap(croppedBitmap)
        //imageView.setImageResource(R.drawable.test);

    }

}

