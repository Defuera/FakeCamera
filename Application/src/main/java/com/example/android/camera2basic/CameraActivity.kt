/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2basic

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.AnimationDrawable
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
import java.io.File


class CameraActivity : Activity() {

    private lateinit var uiLayer: ImageView
    private lateinit var animationLayer: ImageView
    private lateinit var prefs: SharedPreferences

    val FRAME_PER_SECOND = 24
    val FRAME_DURATION = 1000 / FRAME_PER_SECOND
    val PATH_DIRECTORY = Environment.getExternalStorageDirectory().absolutePath + "/cfaker/"
    val FILE_NAME_UI = "ui.png"

    val ad = AnimationDrawable()

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
            showInputDirectoryNameDialog()
            false
        }

        uiLayer.setOnClickListener {
            loadPicturesAndAnimate()
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

        adapter.addItems(
                File(PATH_DIRECTORY)
                        .listFiles()
                        .filter { it.isDirectory }
                        .map { it.name }
        )

        recycler.adapter = adapter

        dialog.show()
    }

    private fun loadPicturesAndAnimate() {
        val dirName = getSavedDirectory()
        val dir = File(PATH_DIRECTORY + dirName)
        if (!dir.exists()) {
            Toast.makeText(this, "Директории с названием $dirName не существует, проверьте правильность ввода данных.", Toast.LENGTH_SHORT).show()
            showInputDirectoryNameDialog()
        } else {
            startAnimation()
        }
    }

    private fun startAnimation() {
        val dirPath = PATH_DIRECTORY + getSavedDirectory()
        val dir = File(dirPath)

        dir
                .listFiles()
                .filter {
                    Log.i("DEnsText", it.absolutePath)
                    it.name != FILE_NAME_UI && it.extension == "png"
                }
                .subList(0, 30)
                .forEach {
                    val createFromPath = Drawable.createFromPath(it.absolutePath)
                    ad.addFrame(createFromPath, FRAME_DURATION)
                }

        animationLayer.background = ad
        ad.isOneShot = false
        ad.start()
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

}
