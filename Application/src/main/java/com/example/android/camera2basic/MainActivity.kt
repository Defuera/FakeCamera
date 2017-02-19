package com.example.android.camera2basic

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import ir.sohreco.androidfilechooser.FileChooserDialog
import rx.Single
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var uiLayer: ImageView
    private lateinit var animationLayer: ImageView
    private lateinit var prefs: SharedPreferences

    private var animationLoading: Boolean = false
    private var subscription: Subscription? = null

    val FRAME_PER_SECOND = 24
    val FRAME_DURATION = 1000 / FRAME_PER_SECOND
    val FILE_NAME_UI = "ui.png"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        prefs = getSharedPreferences("sp", Context.MODE_PRIVATE)

        if (null == savedInstanceState) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit()
        }

        uiLayer = findViewById(R.id.ui_layer) as ImageView
        animationLayer = findViewById(R.id.animation_layer) as ImageView


        uiLayer.setOnLongClickListener {
            releaseResources()
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
        if (savedDirectory != null) {
            val file = File(savedDirectory)
            if (file.exists()) {
                builder.setInitialDirectory(file)
            }
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
        if (animationLoading) {
            Toast.makeText(this, "Пожалуйста подождите, анимация загружается", Toast.LENGTH_SHORT).show()
            return
        }

        if (animationLayer.background != null) {
            releaseResources()
            return
        }

        val dirName = getSavedDirectory()
        if (dirName == null) {
            chooseDirectory()
            return
        }

        val dir = File(dirName)
        if (!dir.exists()) {
            Toast.makeText(this, "Директории с названием $dirName не существует, проверьте правильность ввода данных.", Toast.LENGTH_SHORT).show()
            chooseDirectory()
        } else {
            animationLoading = true
            subscription = loadAnimation()
                    .subscribe(
                            {
                                animationDrawable ->
                                animationLoading = false
                                animationLayer.background = animationDrawable
                                animationDrawable.isOneShot = false
                                animationDrawable.start()
                            },
                            {
                                t ->
                                animationLoading = false
                                Toast.makeText(this, t.localizedMessage, Toast.LENGTH_SHORT).show()
                            }
                    )
        }

    }

    private fun releaseResources() {
        subscription?.unsubscribe()
        subscription = null

        val background: Drawable? = animationLayer.background
        val animationDrawable = if (background != null) background as AnimationDrawable else null
        animationDrawable?.stop()
        animationLayer.background = null
    }

    @Synchronized
    private fun loadAnimation(): Single<AnimationDrawable> {
        return Single
                .fromCallable { createAnimationDrawableBlocking() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    private fun createAnimationDrawableBlocking(): AnimationDrawable {
        val animationDrawable = AnimationDrawable()

        val dirPath = getSavedDirectory()
        val dir = File(dirPath)

        dir
                .listFiles()
                .filter {
                    Log.i("DEnsText", it.absolutePath)
                    it.name != FILE_NAME_UI && it.extension == "png"
                }
                .forEach {
                    val bm: Bitmap = Glide.with(this)
                            .load(it.absolutePath)
                            .asBitmap()
                            .into(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL, com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
                            .get()

                    val bitmapDrawable = BitmapDrawable(resources, bm)
                    animationDrawable.addFrame(bitmapDrawable, FRAME_DURATION)
                }

        return animationDrawable
    }

    private fun displayUi() {
        val filePath = getSavedDirectory() + File.separator + FILE_NAME_UI
        if (File(filePath).exists()) {
            Glide.with(this)
                    .load(filePath)
                    .into(uiLayer)
        } else {
            uiLayer.setImageDrawable(null)
            Toast.makeText(this, "ui.png not found in ${getSavedDirectory()}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSavedDirectory(): String? {
        return prefs.getString("path", null)
    }

    private fun saveDirectory(path: String) {
        prefs.edit().putString("path", path).apply()
    }


    fun screenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    fun screenHeigh(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }
}
