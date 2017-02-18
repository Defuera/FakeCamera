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
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast

class CameraActivity : Activity() {

    private var uiLayer: ImageView? = null
    private var animationLayer: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        if (null == savedInstanceState) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit()
        }

        uiLayer = findViewById(R.id.ui_layer) as ImageView
        animationLayer = findViewById(R.id.animation_layer) as ImageView


        uiLayer!!.setOnLongClickListener {
            Toast.makeText(this@CameraActivity, "long press", Toast.LENGTH_SHORT).show()
            false
        }
    }

}
