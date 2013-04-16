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
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.Iterator;

public class StarGreeterRenderer implements GLSurfaceView.Renderer {

    private final ResourceLoader mResourceLoader;

    private Background mBackground;

    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
    private final float[] mTranslationMatrix = new float[16];
    private final float[] mScaleMatrix = new float[16];
    private final float[] mTmp = new float[16];

    // Declare as volatile because we are updating it from another thread
    private volatile float mDX, mDY;
    private volatile float mAbsoluteZoom;
    private volatile float mDistance;

    private GLText glText;

    public static final float PROJECTION_SIZE = 150;
    public static final float FAR_PLANE = 30;
    public static final float NEAR_PLANE = 0.7f;

    // zoom limits, should correlate with projection box
    private static final float DEPTH_MAX = 10.0f;
    private static final float DEPTH_MIN = 1.1f;
    // does not affect anything
    private static final float ZOOM_MIN = 0.05f;
    private static final float ZOOM_MAX = 5.0f;

    @SuppressWarnings("UnusedDeclaration")
    private boolean mTouched = false;

    private final StarGreeterData mStarGreeterData;
    private Slide mCurrentSlide;
    private final Object listLock = new Object();
    private Iterator<Slide> mSlideIterator;

    private volatile long mPreviousFlipTick = 0;

    // Lighting movement stuff
    private boolean mFixedLighting;
    private float mLightingCounterPhase;

    // Camera movement stuff
    private boolean mAllowAutoZoom;

    private float cameraCounterMin, cameraCounterMax;

    public static final float ZOOM_SMOOTH_FACTOR = 0.05f;
    private int flybyTime;
    private float mRatio;

    public StarGreeterRenderer(Context context) {
        mDX = mDY = 0;
        mAbsoluteZoom = ZOOM_MAX;
        mDistance = calculateDistance(mAbsoluteZoom);

        mResourceLoader = new ResourceLoader(context);

        // Load config
        mStarGreeterData = new StarGreeterData(mResourceLoader.loadXml(R.raw.stargreeter));
        Log.d(Utils.TAG, "starGreeterData = " + mStarGreeterData);

        // Preload fonts
        for (Slide slide : mStarGreeterData.getAllSlides()) {
            mResourceLoader.loadCachedFont(slide.getFontName());
        }

        flybyTime = mStarGreeterData.getSlideTime() * 1000 / 3;

        // Set to the first slide
        mSlideIterator = mStarGreeterData.getAllSlides().iterator();
    }

    public void setCurrentZoom(float currentZoom) {
        mTouched = true;

        float zoomToBe = Math.max(ZOOM_MIN, Math.min(mAbsoluteZoom * currentZoom, ZOOM_MAX));
        float delta = zoomToBe - mAbsoluteZoom;
        if (Math.abs(delta) < 1E-3)
            return;
        mAbsoluteZoom += delta * ZOOM_SMOOTH_FACTOR;
        mDistance = calculateDistance(mAbsoluteZoom);
    }

    public void setCurrentTranslate(float dx, float dy) {
        mTouched = true;

        mDX += dx;
        mDY += dy;
    }

    // ZOOM_MAX -> DEPTH_MAX ; ZOOM_MIN -> DEPTH_MIN ;
    private static float calculateDistance(float absoluteZoom) {
        return (DEPTH_MAX + DEPTH_MIN) / 2
                + (absoluteZoom - (ZOOM_MAX + ZOOM_MIN) / 2)
                * (DEPTH_MIN - DEPTH_MAX) / (ZOOM_MAX - ZOOM_MIN);
    }


    // TODO
    private boolean reportedPerSlide;

    private void flipSlideIfNeeded() {
        // Flip to the next side
        long currentTick = SystemClock.elapsedRealtime();
        if (currentTick - mPreviousFlipTick > mStarGreeterData.getSlideTime() * 1000) {

            Slide slide = null;
            synchronized (listLock) {
                if (mSlideIterator.hasNext()) {
                    slide = mSlideIterator.next();
                }
            }

            if (slide != null) {

                mPreviousFlipTick = currentTick;

                mCurrentSlide = slide;
                Log.d(Utils.TAG, "Flipped to " + mCurrentSlide.getText().split("\n")[0]);

                // Activate new slide
                mAllowAutoZoom = true;
                mFixedLighting = true;
                mDistance = DEPTH_MAX;
                mAbsoluteZoom = ZOOM_MIN;

                mLightingCounterPhase = 0.0f;

                cameraCounterMin = SystemClock.elapsedRealtime();
                cameraCounterMax = cameraCounterMin + flybyTime;

                mTouched = false;
                reportedPerSlide = false;

                createGLText();
            }
        }

        if (glText == null) {
            throw new RuntimeException("GLText had not created yet");
        }

    }

    private void createGLText() {
        glText = new GLText(mResourceLoader);
        // Load the font from file (set size + padding), creates the texture
        glText.load(mCurrentSlide.getFontName(), mCurrentSlide.getFontSize(), 2, 2);
    }

    public void resetView() {
        mDX = mDY = 0;
        mDistance = DEPTH_MIN;
        mAbsoluteZoom = ZOOM_MAX;
    }

    public void resetApp() {
        mPreviousFlipTick = 0;
        synchronized (listLock) {
            mSlideIterator = mStarGreeterData.getAllSlides().iterator();
        }
        resetView();
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        Log.d(Utils.TAG, "Context recreated");

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mBackground = new Background(mResourceLoader);

        if (glText != null) {
            Log.d(Utils.TAG, "Recreate gltext");
            createGLText();
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        flipSlideIfNeeded();

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mBackground.draw();

        // adjust camera distance

        float cameraCounter = SystemClock.elapsedRealtime();

        if (mAllowAutoZoom) {
            float cameraAlpha = (DEPTH_MAX - DEPTH_MIN) / (cameraCounterMin - cameraCounterMax);
            float cameraBeta = DEPTH_MAX - cameraAlpha * cameraCounterMin;
            mDistance = cameraBeta + cameraAlpha * cameraCounter;

            if (mDistance < DEPTH_MIN * 1.01) {
                // stop camera movement
                mAllowAutoZoom = false;
                mDistance = DEPTH_MIN;
                mAbsoluteZoom = ZOOM_MAX;
            }
        }

        float lightingCounter = (SystemClock.elapsedRealtime() % (int) (2 * Math.PI * 1000)) / 1000.0f;

        // adjust lights
        final float xLightOffset = 0.5f;
        final float lightAmplitude = 3f;
        final float lightSlowFactor = 2f;

        // fixed lighting if far enough
        boolean shouldFixedLighting = Math.abs(mDistance - DEPTH_MIN) > (DEPTH_MAX - DEPTH_MIN) / 3.0;
        if (mFixedLighting != shouldFixedLighting) {
            if (!shouldFixedLighting) {
                // save a phase if enabling fixed lighting
                mLightingCounterPhase = (float) Math.asin((0.0f - xLightOffset) / lightAmplitude)
                        - lightSlowFactor * lightingCounter;
            }
            mFixedLighting = shouldFixedLighting;
        }

        // leave light at center
        float xLight = 0, yLight = 0;
        if (!mFixedLighting) {
            xLight = xLightOffset + lightAmplitude * (float) Math.sin(lightingCounter * lightSlowFactor
                    + mLightingCounterPhase);
            yLight = 0;// 0.7f * (float) Math.sin(1 + counter / 100.0f);

        }
        glText.setLightPosition(xLight, yLight, 1.0f);


        // Set the camera position (View matrix)
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, mDistance, 0f, 0f, 1.0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

        if (!mAllowAutoZoom) {
            // Allow touch movement only if autozoom finished
            Matrix.setIdentityM(mTranslationMatrix, 0);
            Matrix.translateM(mTranslationMatrix, 0, mDX, mDY, 0.0f);
            Matrix.multiplyMM(mMVPMatrix, 0, dupMatrix(mMVPMatrix), 0, mTranslationMatrix, 0);
        }

        // scale text
        int stringsWidth = 0;
        for (String str : getCurrentSlideLines()) {
            int len = 0;
            for (int i = 0; i < str.length(); i++) {
                len += glText.getCharWidth(str.charAt(i));
            }
            stringsWidth = Math.max(stringsWidth, len);
        }
        float stringsHeight = getCurrentSlideLines().length * glText.getCharHeight() +
                (getCurrentSlideLines().length - 1) * glText.getCharHeight() * 0.2f;

        float preScale = 1.2f;
        float scaleX = preScale * (PROJECTION_SIZE * mRatio / stringsWidth);
        float scaleY = preScale * (PROJECTION_SIZE / stringsHeight);
        float scale = Math.min(scaleX, scaleY);
        if (!reportedPerSlide) {
            Log.d(Utils.TAG, "scaleX = " + scaleX + " scaleY = " + scaleY + " scale = " + scale + " maxStringLen=" + stringsWidth);
            reportedPerSlide = true;
        }
        Matrix.setIdentityM(mScaleMatrix, 0);
        Matrix.scaleM(mScaleMatrix, 0, scale, scale, 1.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, dupMatrix(mMVPMatrix), 0, mScaleMatrix, 0);

        drawText();
    }

    private void drawText() {

        if (mCurrentSlide != null) {
            glText.begin(Color.red(mCurrentSlide.getFontColor()),
                    Color.green(mCurrentSlide.getFontColor()),
                    Color.blue(mCurrentSlide.getFontColor()),
                    1.0f,
                    mMVPMatrix);

            final String[] strings = getCurrentSlideLines();

            // calcualate positions
            final float hf = glText.getCharHeight();
            final float h0 = hf * 0.2f;
            final float h = strings.length * (hf + h0) - h0;

            for (int i = 0; i < strings.length; i++) {
                float y = -h / 2 + hf / 2 + i * (hf + h0);
//                Log.d(Utils.TAG, "i=" + i + " y=" + y);
                glText.drawC(strings[i], 0, y, 0);
            }

//            glText.draw(String.format("%.1f", mDistance), 30, 30, 0);
            glText.end();
        }

    }

    private String[] getCurrentSlideLines() {
        return mCurrentSlide.getText().split("\\r?\\n");
    }

    private float[] dupMatrix(float[] input) {
        Utils.copyVector(input, mTmp);
        return mTmp;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.d(Utils.TAG, "Surface changed");

        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method

        mRatio = (float) width / height;
        float top = PROJECTION_SIZE / 2, bottom = -top;
        if (width < height) {
            top /= mRatio;
            bottom /= mRatio;
        }
        Matrix.frustumM(mProjMatrix, 0, mRatio * bottom, mRatio * top, bottom, top, NEAR_PLANE, FAR_PLANE);
    }
}


