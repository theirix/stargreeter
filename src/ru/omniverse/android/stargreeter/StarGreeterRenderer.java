package ru.omniverse.android.stargreeter;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.Iterator;

import static ru.omniverse.android.stargreeter.Utils.TAG;

public class StarGreeterRenderer implements GLSurfaceView.Renderer {

    private final ResourceLoader mResourceLoader;
    private final Handler mStopHandler;

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

    // TODO debug stuff
    private boolean reportedPerSlide;

    private boolean mFinished = false;

    private final StarGreeterData mStarGreeterData;
    private Slide mCurrentSlide;
    private final Object listLock = new Object();
    private Iterator<Slide> mSlideIterator;

    private volatile long mPreviousFlipTick = 0;

    // Lighting movement stuff
    private boolean mDynamicLightingInProgress;
    private float mLightingCounterPhase;

    private boolean mOverexposeInProgress = false;
    private long mOverexposeTimer;
    private final long OVEREXPOSE_TIME = 750;

    private static final long TOUCH_BOUGHT_TIME = 3000;

    // Camera movement stuff
    private volatile boolean mAutoZoomInProgress;

    private float cameraCounterMin, cameraCounterMax;

    public static final float ZOOM_SMOOTH_FACTOR = 0.05f;
    private int mFlybyTime;
    private float mRatio;

    private long mPrevVelTimestamp = 0;
    private int mSlideTimeMultiplier;

    // Ctor
    public StarGreeterRenderer(Context context, StarGreeterData starGreeterData, Handler stopHandler) {
        mStarGreeterData = starGreeterData;
        mStopHandler = stopHandler;

        mDX = mDY = 0;
        mAbsoluteZoom = ZOOM_MAX;
        mDistance = calculateDistance(mAbsoluteZoom);

        mResourceLoader = new ResourceLoader(context);

        // Preload fonts
        for (Slide slide : mStarGreeterData.getAllSlides()) {
            mResourceLoader.loadCachedFont(slide.getFontName());
        }

        // Set to the first slide
        mSlideIterator = mStarGreeterData.getAllSlides().iterator();
    }

    // external interface

    public void setCurrentZoom(float currentZoom) {
        if (mAutoZoomInProgress && (mAbsoluteZoom < ZOOM_MAX * 0.7))
            mTouched = true;

        if (mAutoZoomInProgress)
            return;

        mTouched = true;

        float zoomToBe = Math.max(ZOOM_MIN, Math.min(mAbsoluteZoom * currentZoom, ZOOM_MAX));
        float delta = zoomToBe - mAbsoluteZoom;
        if (Math.abs(delta) < 1E-3)
            return;
        mAbsoluteZoom += delta * ZOOM_SMOOTH_FACTOR;
        mDistance = calculateDistance(mAbsoluteZoom);
    }

    public void setCurrentMove(float dx, float dy) {
        if (mAutoZoomInProgress && (mAbsoluteZoom < ZOOM_MAX * 0.7))
            mTouched = true;

        if (mAutoZoomInProgress)
            return;

        mTouched = true;

        // scale
        dx /= 2;
        dy /= 2;

        long time = SystemClock.elapsedRealtime();
        float velX = 0, velY = 0;
        if (mPrevVelTimestamp > 0) {
            velX = dx / (time - mPrevVelTimestamp);
            velY = dy / (time - mPrevVelTimestamp);
        }
        mPrevVelTimestamp = time;

        mDX += dx * (1.0f + Math.abs(velX) / 2);
        mDY += dy * (1.0f + Math.abs(velY) / 2);
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

    // end of external interface

    // ZOOM_MAX -> DEPTH_MAX ; ZOOM_MIN -> DEPTH_MIN ;
    private static float calculateDistance(float absoluteZoom) {
        return (DEPTH_MAX + DEPTH_MIN) / 2
                + (absoluteZoom - (ZOOM_MAX + ZOOM_MIN) / 2)
                * (DEPTH_MIN - DEPTH_MAX) / (ZOOM_MAX - ZOOM_MIN);
    }

    private long calculateDeltaTime(long currentTick) {
        return -currentTick + mPreviousFlipTick + mSlideTimeMultiplier * mStarGreeterData.getSlideTime() * 1000;
    }

    private void flipSlideIfNeeded() {
        // Flip to the next side
        long currentTick = SystemClock.elapsedRealtime();

        // Time left for the current slide (decreasing to zero)
        long deltaTime = calculateDeltaTime(currentTick);

        // Override timer if touched
        if (mTouched) {
            mPreviousFlipTick = currentTick - TOUCH_BOUGHT_TIME;
            deltaTime = calculateDeltaTime(currentTick);
            mTouched = false;
        }

        if (deltaTime <= 0) {

            mOverexposeInProgress = false;

            Slide slide = null;
            synchronized (listLock) {
                if (mSlideIterator.hasNext()) {
                    slide = mSlideIterator.next();
                } else if (!mFinished) {
                    mFinished = true;
                    Log.d(TAG, "Finished");
                    if (!mStarGreeterData.isKeepLastSlide())
                        mStopHandler.sendEmptyMessage(0);
                }
            }

            if (slide != null) {

                mPreviousFlipTick = currentTick;
                mCurrentSlide = slide;
                createCurrentSlide();
            }
        } // if there is a currently active slide and there is some time before the end of slide...
        else if (!mFinished && mCurrentSlide != null && deltaTime < OVEREXPOSE_TIME) {
            boolean isLast;
            synchronized (listLock) {
                isLast = !mSlideIterator.hasNext();
            }
            if (!isLast) {
                // decreasing to zero
                mOverexposeTimer = OVEREXPOSE_TIME - deltaTime;
                mOverexposeInProgress = true;
            }
        }

        if (glText == null) {
            throw new RuntimeException("GLText had not created yet");
        }

    }

    private void createCurrentSlide() {
        Log.d(TAG, "Flipped to " + mCurrentSlide.getText().split("\n")[0]);

        // Activate new slide
        mAutoZoomInProgress = true;
        mDynamicLightingInProgress = false;
        //mFlingInProgress = false;
        mOverexposeInProgress = false;
        mDistance = DEPTH_MAX;
        mAbsoluteZoom = ZOOM_MIN;
        mDX = mDY = 0;
        mLightingCounterPhase = 0.0f;

        mSlideTimeMultiplier = mCurrentSlide.equals(mStarGreeterData.getBeginning()) ? 2 : 1;
        mFlybyTime = mSlideTimeMultiplier * mStarGreeterData.getSlideTime() * 1000 / 3;

        cameraCounterMin = SystemClock.elapsedRealtime();
        cameraCounterMax = cameraCounterMin + mFlybyTime;

        mTouched = false;
        reportedPerSlide = false;

        createGLText();
    }

    private void createGLText() {
        glText = new GLText(mResourceLoader);
        // Load the font from file (set size + padding), creates the texture
        glText.load(mCurrentSlide.getFontName(), mCurrentSlide.getFontSize(), 2, 2);
    }

    private String[] getCurrentSlideLines() {
        return mCurrentSlide.getText().split("\\r?\\n");
    }

    private float[] dupMatrix(float[] input) {
        Utils.copyVector(input, mTmp);
        return mTmp;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        Log.d(TAG, "Context recreated");

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        mBackground = new Background(mResourceLoader);

        if (glText != null) {
            Log.d(TAG, "Recreate gltext");
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

        final long time = SystemClock.elapsedRealtime();

        if (mAutoZoomInProgress) {
            float cameraAlpha = (DEPTH_MAX - DEPTH_MIN) / (cameraCounterMin - cameraCounterMax);
            float cameraBeta = DEPTH_MAX - cameraAlpha * cameraCounterMin;
            mDistance = cameraBeta + cameraAlpha * time;

            if (mDistance < DEPTH_MIN * 1.01) {
                // stop camera movement
                mAutoZoomInProgress = false;
                mDistance = DEPTH_MIN;
                mAbsoluteZoom = ZOOM_MAX;
            }
        }

        float lightingCounter = (time % (int) (2 * Math.PI * 1000)) / 1000.0f;

        // adjust lights
        final float xLightOffset = 0.5f;
        final float lightAmplitude = 2f;
        final float lightSlowFactor = 1.0f;

        // fixed lighting if far enough
        boolean shouldDynamicLighting = Math.abs(mDistance - DEPTH_MIN) < (DEPTH_MAX - DEPTH_MIN) / 3.0;
        if (mDynamicLightingInProgress != shouldDynamicLighting) {
            if (shouldDynamicLighting) {
                // save a phase if enabling fixed lighting
                mLightingCounterPhase = (float) Math.asin((0.0f - xLightOffset) / lightAmplitude)
                        - lightSlowFactor * lightingCounter;
            }
            mDynamicLightingInProgress = shouldDynamicLighting;
        }

        // leave light at center
        float xLight = 0, yLight = 0;
        if (mDynamicLightingInProgress) {
            xLight = xLightOffset + lightAmplitude * (float) Math.sin(lightingCounter * lightSlowFactor
                    + mLightingCounterPhase);
            yLight = 0;// 0.7f * (float) Math.sin(1 + counter / 100.0f);

        }
        glText.setLightPosition(xLight, yLight, 1.0f);


        // Set the camera position (View matrix)
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, mDistance, 0f, 0f, 1.0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);


        /*// if a scene is currently flinging, adjust position according to velocity
        if (mFlingInProgress && time >= mFlingEndTime){
            Log.d(TAG, "Fling completed");
            mFlingInProgress = false;
        }
        if (mFlingInProgress) {
            float percentTime = (float) (time - mFlingStartTime) / (float) (mFlingEndTime - mFlingStartTime);
            float percentDistance = new OvershootInterpolator(0.0f).getInterpolation(percentTime);
            mFlingDx = mTotalDx * percentDistance;
            mFlingDy = mTotalDy * percentDistance;
            Log.d(TAG, "percentTime = " + percentTime + " dist = " + percentDistance
                    + " dx=" + mDX + ";" + mDY + " fling: "+mFlingDx+";"+mFlingDy);
            mDX += mFlingDx;
            mDY += mFlingDy;
        }*/

        // Move a scene always, dx and dy are actual values
        Matrix.setIdentityM(mTranslationMatrix, 0);
        Matrix.translateM(mTranslationMatrix, 0, mDX, mDY, 0.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, dupMatrix(mMVPMatrix), 0, mTranslationMatrix, 0);

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
            Log.d(TAG, "scaleX = " + scaleX + " scaleY = " + scaleY + " scale = " + scale + " maxStringLen=" + stringsWidth);
            reportedPerSlide = true;
        }
        Matrix.setIdentityM(mScaleMatrix, 0);
        Matrix.scaleM(mScaleMatrix, 0, scale, scale, 1.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, dupMatrix(mMVPMatrix), 0, mScaleMatrix, 0);

        drawText();
    }

    private void drawText() {

        if (mCurrentSlide == null)
            return;

        final int color = mOverexposeInProgress
                ? Color.WHITE
                : mCurrentSlide.getFontColor();
        final float alpha = mOverexposeInProgress
                ? Math.max(Math.min((1.0f * OVEREXPOSE_TIME - mOverexposeTimer) / OVEREXPOSE_TIME, 1.0f), 0.0f)
                : 1.0f;
        glText.begin(Color.red(color), Color.green(color), Color.blue(color), alpha, mMVPMatrix);
//        glText.begin(1,1,1,1, mMVPMatrix);

        final String[] strings = getCurrentSlideLines();

        // calcualate positions
        final float hf = glText.getCharHeight();
        final float h0 = hf * 0.2f;
        final float h = strings.length * (hf + h0) - h0;

        for (int i = 0; i < strings.length; i++) {
            float y = -h / 2 + hf / 2 + i * (hf + h0);
            glText.drawC(strings[strings.length - i - 1], 0, y, 0);
        }

//        glText.drawTexture(0,0, mMVPMatrix);
//            glText.draw(String.format("%.1f %.1f", mVelX, mVelY), 30, 30, 0);
//            glText.draw(String.format("%.1f", mDistance), 30, 30, 0);
        glText.end();

    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.d(TAG, "Surface changed");

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


