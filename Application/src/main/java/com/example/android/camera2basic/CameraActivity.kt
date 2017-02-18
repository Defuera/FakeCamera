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
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import java.io.File


class CameraActivity : Activity() {

    private lateinit var uiLayer: ImageView
    private lateinit var animationLayer: ImageView
    private lateinit var prefs: SharedPreferences

    val PATH_DIRECTORY = "/sdcard/cfaker/"

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
    }

    private fun showInputDirectoryNameDialog() {
        val editText = EditText(this)
        val padding = resources.getDimensionPixelOffset(R.dimen.padding_16)
        editText.setPadding(padding, padding, padding, padding)
        val savedPath = getSavedDirectory()

        editText.setText(savedPath)
        editText.setSelection(savedPath.length)


        AlertDialog.Builder(this)
                .setTitle("Укажите путь к папке с ui.png и файлами анимации. ")
                .setMessage("Папка должна быть помещена на внутренню память телефона по адреусу $PATH_DIRECTORY и не иметь в названии пробелов")
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

    private fun loadPicturesAndAnimate() {
        val dirName = getSavedDirectory()
        val dir = File(PATH_DIRECTORY + dirName)
        if (!dir.exists()) {
            Toast.makeText(this, "Директории с названием $dirName не существует, проверьте правильность ввода данных.", Toast.LENGTH_SHORT).show()
            showInputDirectoryNameDialog()
        } else {
//            create
        }
    }

    private fun getSavedDirectory(): String {
        return prefs.getString("path", PATH_DIRECTORY)
    }

    private fun saveDirectory(path: String) {
        prefs.edit().putString("path", path).apply()
    }

}
