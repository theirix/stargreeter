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

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class StarGreeterRenderer implements GLSurfaceView.Renderer {

//    private static final String TAG = "StarGreeterRenderer";
    private final ResourceLoader mResourceLoader;

    //private Triangle mTriangle;
    //private Square mSquare;
    private TriangleColored mTriangleColored;

    private Background mBackground;


    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
//    private final float[] mTranslationMatrix = new float[16];
    private final float[] mScaleMatrix = new float[16];
    private final float[] mTmp = new float[16];

    // Declare as volatile because we are updating it from another thread
    //private volatile float mAngle;
    private volatile float mDX;
    private volatile float mDY;
    private volatile float mAbsoluteZoom;
    private volatile float mDistance;
    private volatile String mDisplay;

    private GLText glText;

    // zoom limits, should correlate with projection box
    private static final float DEPTH_MAX = 7f;
    private static final float DEPTH_MIN = 1.0f;
    // does not affect anything
    private static final float ZOOM_MIN = 0.05f;
    private static final float ZOOM_MAX = 5.0f;

    private boolean mTouched = false;

    public StarGreeterRenderer(Context context) {
        //mAngle = 0;
        mDX = mDY = 0;
        mAbsoluteZoom = ZOOM_MAX;
        mDistance = calculateDistance(mAbsoluteZoom);
        mDisplay = String.format("cur=- abs=%.1f", mAbsoluteZoom);

        mResourceLoader = new ResourceLoader(context);
    }

//    private double gauss(double x, double mu, double sigma) {
//        return (1.0 / (sigma * Math.sqrt(2 * Math.PI)))
//                * Math.exp(-(x - mu) * (x - mu) / (2 * mu * mu));
//    }

    public void setCurrentZoom(float currentZoom) {
        mTouched = true;

        //currentZoom = 1.0f + (currentZoom - 1.0f) * (float)gauss(currentZoom, 1.0, 0.5);
        //currentZoom = Math.max(0.1f, Math.min(currentZoom, 10.0f));
        float zoomToBe = Math.max(ZOOM_MIN, Math.min(mAbsoluteZoom * currentZoom, ZOOM_MAX));
        float delta = zoomToBe - mAbsoluteZoom;
        if (Math.abs(delta) < 1E-3)
            return;
        mAbsoluteZoom += delta * 0.05f;
        //mAbsoluteZoom *= currentZoom;
        mDistance = calculateDistance(mAbsoluteZoom);

        mDisplay = String.format("cur=%.1f abs=%.1f", currentZoom, mAbsoluteZoom);
    }

    public void setCurrentTranslate(float dx, float dy) {
        mTouched = true;

        mDX += dx;
        mDY += dy;
    }

    private static float calculateDistance(float absoluteZoom) {
        return (DEPTH_MAX + DEPTH_MIN) / 2
                + (absoluteZoom - (ZOOM_MAX + ZOOM_MIN) / 2)
                * (DEPTH_MIN - DEPTH_MAX) / (ZOOM_MAX - ZOOM_MIN);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        GLES20.glEnable(GLES20.GL_BLEND);
        //GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        //GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
        //GLES20.glEnable(GL10.GL_ALPHA_BITS);

        //mTriangle = new Triangle();
//        mSquare = new Square();

        mBackground = new Background(mResourceLoader);
        mTriangleColored = new TriangleColored(mResourceLoader);

        // Create the GLText
        glText = new GLText(mResourceLoader);

        // Load the font from file (set size + padding), creates the texture
        // NOTE: after a successful call to this the font is ready for rendering!
        glText.load("Federation.ttf", 50, 2, 2);  // Create Font (Height: 14 Pixels / X+Y Padding 2 Pixels)

    }


    @Override
    public void onDrawFrame(GL10 unused) {

        // Draw background color
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        float[] mPrev = new float[16];

        mBackground.draw();

        if (!mTouched) {
            float tmp = (SystemClock.uptimeMillis() % (int) (2 * Math.PI * 1000)) / 1000.0f;
            mDistance = 1.0f + 3.0f * (1 + (float) Math.sin(tmp));
        }


        // Set the camera position (View matrix)
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, mDistance, 0f, 0f, 1.0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

        System.arraycopy(mMVPMatrix, 0, mPrev, 0, mPrev.length);

        float textScale = 100;
        Matrix.setIdentityM(mScaleMatrix, 0);
        Matrix.scaleM(mScaleMatrix, 0, textScale, textScale, 0.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, dupMatrix(mMVPMatrix), 0, mScaleMatrix, 0);

        //Matrix.setRotateM(mTranslationMatrix, 0, mDX, 0, -1.0f, 0);
        //Matrix.multiplyMM(mMVPMatrix, 0, dupMatrix(mMVPMatrix), 0, mTranslationMatrix, 0);

        // Draw triangle
        mTriangleColored.draw(mMVPMatrix, mVMatrix, mProjMatrix);


        System.arraycopy(mPrev, 0, mMVPMatrix, 0, mPrev.length);

        /*float textScale = 0.03f;
        Matrix.setIdentityM(mScaleMatrix, 0);
        Matrix.scaleM(mScaleMatrix, 0, textScale, textScale, 0.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, dupMatrix(mMVPMatrix), 0, mScaleMatrix, 0);*/
        drawText();


    }

    private float[] dupMatrix(float[] input) {
        System.arraycopy(input, 0, mTmp, 0, input.length);
        return mTmp;
    }

    private void drawText() {
        glText.begin(1, 1, 1, 0.5f, mMVPMatrix);
        glText.draw(String.format("%.1f", mDistance), 30, 10, 0);
        glText.draw(String.format(mDisplay), 50, 20 + glText.getCharHeight(), 0);
        glText.draw("star trek", -120, -10, 0);
        glText.end();

    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method

        float ratio = (float) width / height;
        float near = 1, far = 30;
        float top = 75, bottom = -top;
        if (width < height) {
            top /= ratio;
            bottom /= ratio;
        }
        Matrix.frustumM(mProjMatrix, 0, ratio * bottom, ratio * top, bottom, top, near, far);
    }

}


