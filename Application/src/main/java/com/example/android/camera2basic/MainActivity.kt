package com.example.android.camera2basic

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import ir.sohreco.androidfilechooser.FileChooserDialog
import java.io.File

/**
 * https://docs.google.com/document/d/14I7E6EM0KH-JOwwWG3ZE3iBC9KOa7GmiuFZ-uYePQdw/edit#heading=h.z4v2w9ra93i0
 */
class MainActivity : AppCompatActivity() {

    private lateinit var uiLayer: ImageView
    private lateinit var prefs: SharedPreferences

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
            onUiLayerClicked()
        }

        displayUi()
    }

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
        val builder = FileChooserDialog.Builder(
                FileChooserDialog.ChooserType.DIRECTORY_CHOOSER,
                FileChooserDialog.ChooserListener {
                    dir ->
                    Toast.makeText(this, "Директория выбрана $dir", Toast.LENGTH_SHORT).show()
                    saveDirectory(dir)
                    displayUi()
                })
                .setSelectDirectoryButtonText("OK")

        val savedDirectory = getSavedDirectory()
        val directory = File(savedDirectory)
        if (directory.exists()) {
            builder.setInitialDirectory(directory)
        }

        builder.build().show(supportFragmentManager, "tag")
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
                        { dialogInterface, i ->
                            dialogInterface.dismiss()
                            saveDirectory(editText.text.toString())
                        }
                )
                .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

    }

    private fun onUiLayerClicked() {
        showNextImage()
    }

    private fun showNextImage() {
        imageIndex++
        displayUi()
    }

    private fun displayUi() {
        val dirPath = getSavedDirectory()

        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            uiLayer.setImageDrawable(null)
            Toast.makeText(this, "Директории с названием ${dir.name} не существует, проверьте правильность ввода данных.", Toast.LENGTH_SHORT).show()
            chooseDirectory()
            return
        }

        val listFiles = dir.listFiles()
        if (listFiles == null || listFiles.filter { it.extension.contains("png") }.isEmpty()) {
            uiLayer.setImageDrawable(null)
            Toast.makeText(this, "*.png not found in ${getSavedDirectory()}", Toast.LENGTH_SHORT).show()
            return
        }

        val imageFilesList = listFiles.filter { it.extension.contains("png") }
        if (imageIndex > imageFilesList.lastIndex) {
            imageIndex = 0
        }

        val imagePath = imageFilesList[imageIndex].absolutePath

        Glide.with(this)
                .load(imagePath)
                .crossFade(0)
                .into(uiLayer)
    }

    private fun getSavedDirectory(): String? {
        return prefs.getString("path", Environment.getExternalStorageDirectory().absolutePath + File.separator)
    }

    private fun saveDirectory(path: String) {
        prefs.edit().putString("path", path).apply()
    }

}