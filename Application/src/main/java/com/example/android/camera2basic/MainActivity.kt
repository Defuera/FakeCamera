package com.example.android.camera2basic

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import ir.sohreco.androidfilechooser.FileChooserDialog
import java.io.File


/**
 * https://docs.google.com/document/d/14I7E6EM0KH-JOwwWG3ZE3iBC9KOa7GmiuFZ-uYePQdw/edit#heading=h.z4v2w9ra93i0
 */
class MainActivity : AppCompatActivity() {

    val DEFAULT_DIRECTORY = Environment.getExternalStorageDirectory().absolutePath + File.separator
    private lateinit var uiLayer: ImageView
    private lateinit var prefs: SharedPreferences

    private val listImages = ArrayList<Bitmap>()

    private var imageIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        Fabric.with(this, Crashlytics())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        prefs = getSharedPreferences("sp", Context.MODE_PRIVATE)

        if (null == savedInstanceState) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit()
        }

        uiLayer = findViewById(R.id.ui_layer) as ImageView

        uiLayer.setOnLongClickListener {
            chooseDirectory()
            false
        }

        uiLayer.setOnClickListener {
            showNextImage()
        }

        preloadAndDisplayImages()
    }


    //region Directory chooser

    private fun chooseDirectory() {
        AlertDialog.Builder(this)
                .setTitle("Выбрать папку с помощью:")
                .setPositiveButton(
                        "Ручной ввод",
                        { _, _ -> showInputPathDialog() }
                )
                .setNegativeButton(
                        "Навигатор",
                        { _, _ -> showNavigator() }
                )
                .show()

    }

    private fun showNavigator() {
        var dirPath: String? = null
        try {

            val builder = FileChooserDialog.Builder(
                    FileChooserDialog.ChooserType.DIRECTORY_CHOOSER,
                    FileChooserDialog.ChooserListener {
                        dir ->
                        onChooseDirectory(dir)
                    })
                    .setSelectDirectoryButtonText("OK")

            dirPath = getSavedDirectory()
            val directory = File(dirPath)
            if (directory.exists()) {
                builder.setInitialDirectory(directory)
            }

            builder.build().show(supportFragmentManager, "tag")
        } catch (e: Exception) {
            Toast.makeText(this, "Директория $dirPath не существует ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInputPathDialog() {
        val editText = EditText(this)
        val padding = resources.getDimensionPixelOffset(R.dimen.padding_16)
        editText.setPadding(padding, padding, padding, padding)
        val savedPath = getSavedDirectory()

        editText.setText(savedPath)
        editText.setSelection(savedPath?.length ?: 0)

        AlertDialog.Builder(this)
                .setTitle("Укажите путь к папке с ui.png и файлами анимации. ")
                .setView(editText)
                .setPositiveButton(
                        "OK",
                        { dialogInterface, _ ->
                            dialogInterface.dismiss()
                            onChooseDirectory(editText.text.toString())
                        }
                )
                .show()
    }

    private fun onChooseDirectory(dir: String) {
        Toast.makeText(this, "Директория выбрана $dir", Toast.LENGTH_SHORT).show()
        saveDirectory(dir)
        preloadAndDisplayImages()
    }

    //endregion


    //region Display images

    private fun preloadAndDisplayImages() {
        listImages.clear()
        uiLayer.setImageBitmap(null)

        val dirPath = getSavedDirectory()

        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            return
        }

        val listFiles = dir.listFiles()
        if (listFiles == null || listFiles.filter { it.extension.contains("png") }.isEmpty()) {
            return
        }

        listFiles
                .filter { it.extension.contains("png") }
                .forEach {

                    Glide.with(this)
                            .load(it)
                            .asBitmap()
                            .into(
                                    object : SimpleTarget<Bitmap>() {
                                        override fun onResourceReady(bmp: Bitmap, glideAnimation: GlideAnimation<in Bitmap>?) {
                                            listImages.add(bmp)
                                        }

                                    }
                            )

                }

        Handler().postDelayed({
            if (listImages.isNotEmpty()) {
                displayUi()
            }
        },
                600)

    }

    private fun showNextImage() {
        if (listImages.isEmpty()) {
            chooseDirectory()
        } else {
            imageIndex++
            displayUi()
        }
    }

    private fun displayUi() {
        if (imageIndex > listImages.lastIndex) {
            imageIndex = 0
        }

        uiLayer.setImageBitmap(listImages[imageIndex])
    }

    //endregion


    //region data storage

    private fun getSavedDirectory(): String? {
        return prefs.getString("path", DEFAULT_DIRECTORY)
    }

    private fun saveDirectory(path: String) {
        prefs.edit().putString("path", path).apply()
    }

    //endregion

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }
}