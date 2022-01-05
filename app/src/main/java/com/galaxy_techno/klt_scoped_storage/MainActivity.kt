package com.galaxy_techno.klt_scoped_storage

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import java.io.File

//https://askandroidquestions.com/2020/11/15/android-11-documentfile-findfile-is-too-slow/

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val btnCamera: Button by lazy { findViewById(R.id.btn_camera) }
    private val btnGallery: Button by lazy { findViewById(R.id.btn_gallery) }
    private val imageView: ImageView by lazy { findViewById(R.id.imageView) }

    private var tempUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btnCamera.setOnClickListener {
//            takeImage()
            choosePDF()
        }
        btnGallery.setOnClickListener {
            selectImageFromGallery()
        }
    }


    private val fromPDF = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        it?.let {
            if (it.resultCode == Activity.RESULT_OK) {
                this.contentResolver?.takePersistableUriPermission(
                    it.data?.data!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val file = DocumentUtils.getFile(this,it.data?.data!!) //use pdf as file
                Toast.makeText(this,file.absolutePath,Toast.LENGTH_SHORT).show()
            }
        }

    }

    private val fromCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { status ->

        //if you want the exact bytes of photo
//        MediaStore.setRequireOriginal(photoUri)

        if (status) {
            tempUri?.let {
                imageView.setImageURI(it)

                val bitmap = uriToBitmap(it)
                Toast.makeText(this, bitmap.toString(), Toast.LENGTH_SHORT).show()
            }
        }

    }

    private val fromGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) {
        it?.let {
            imageView.setImageURI(it)
            val bitmap = uriToBitmap(it)
            Toast.makeText(this, bitmap.toString(), Toast.LENGTH_SHORT).show()
        }
    }


    private fun takeImage() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri ->
                tempUri = uri
                fromCamera.launch(uri)
            }
        }
    }

    private fun choosePDF() {

//        Intent.ACTION_OPEN_DOCUMENT_TREE = can select custom folder
//        Intent.ACTION_OPEN_DOCUMENT = select all folder
//        Intent.ACTION_CREATE_DOCUMENT = create file such as Email attachment

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }

        fromPDF.launch(intent)


    }

    private fun selectImageFromGallery() {
        lifecycleScope.launchWhenCreated {
            fromGallery.launch("image/*")
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(
            applicationContext,
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return when {

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                val source = ImageDecoder.createSource(this.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }

            else -> {
                MediaStore.Images.Media.getBitmap(
                    this.contentResolver,
                    uri
                )
            }
        }

    }
}

