package com.example.android.camera2basic

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.m039.el_adapter.ListItemAdapter
import rx.Single
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File


class MainActivity : Activity() {

    private lateinit var uiLayer: ImageView
    private lateinit var animationLayer: ImageView
    private lateinit var prefs: SharedPreferences

    private var animationLoading: Boolean = false
    private var subscription: Subscription? = null

    val FRAME_PER_SECOND = 24
    val FRAME_DURATION = 1000 / FRAME_PER_SECOND
    val PATH_DIRECTORY = Environment.getExternalStorageDirectory().absolutePath + "/cfaker/"
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
            showInputDirectoryNameDialog()
            false
        }

        uiLayer.setOnClickListener {
            onUiLayerClicked()
        }

        displayUi()
    }

    private fun showInputDirectoryNameDialog() {
        val recycler = RecyclerView(this)
        recycler.layoutManager = LinearLayoutManager(this)

        val adapter = ListItemAdapter()

        val dialog = AlertDialog.Builder(this)
                .setTitle("Укажите путь к папке с ui.png и файлами анимации. ")
                .setMessage("Папка должна быть помещена на внутренню память телефона по адреусу $PATH_DIRECTORY и не иметь в названии пробелов")
                .setView(recycler)
                .create()

        adapter
                .addViewCreator(
                        String::class.java,
                        { parent -> View.inflate(parent.context, R.layout.widget_item, null) as TextView }
                )
                .addViewBinder(TextView::setText)
                .addOnItemViewClickListener { _, item ->
                    saveDirectory(item)
                    displayUi()
                    dialog.dismiss()
                }

        val files = File(PATH_DIRECTORY).listFiles()
        if (files != null) {
            adapter.addItems(
                    files
                            .filter { it.isDirectory }
                            .map { it.name }
            )
        }

        recycler.adapter = adapter
        dialog.show()
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
        val dir = File(PATH_DIRECTORY + dirName)
        if (!dir.exists()) {
            Toast.makeText(this, "Директории с названием $dirName не существует, проверьте правильность ввода данных.", Toast.LENGTH_SHORT).show()
            showInputDirectoryNameDialog()
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

        val dirPath = PATH_DIRECTORY + getSavedDirectory()
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
        val filePath = PATH_DIRECTORY + getSavedDirectory() + File.separator + FILE_NAME_UI
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
