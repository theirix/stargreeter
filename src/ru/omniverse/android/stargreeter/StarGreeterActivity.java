/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.omniverse.android.stargreeter;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

public class StarGreeterActivity extends Activity {

    private GLSurfaceView mGLView;
    private StarGreeterData mStarGreeterData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStarGreeterData = new StarGreeterData(new ResourceLoader(this).loadXml(R.raw.stargreeter));
        Log.d(Utils.TAG, "StarGreeterData loaded " + mStarGreeterData);

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity
        mGLView = new StarGreeterGLSurfaceView(this, mStarGreeterData);
        setContentView(mGLView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The following call pauses the rendering thread.
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
        mGLView.onPause();

        stopService(new Intent(this, AudioService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Intent intent = new Intent(this, AudioService.class);
        intent.putExtra("name", mStarGreeterData.getAudioName());
        startService(intent);

        // The following call resumes a paused rendering thread.
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
        mGLView.onResume();
    }
}

