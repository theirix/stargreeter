package ru.omniverse.android.stargreeter;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created with IntelliJ IDEA.
 * User: irix
 * Date: 14.04.2013
 * Time: 22:52
 */
@SuppressWarnings("FieldCanBeLocal")
class StarGreeterGLSurfaceView extends GLSurfaceView {

    private final StarGreeterRenderer mRenderer;

    private final ScaleGestureDetector mScaleDetector;
    private final GestureDetector mGestureDetector;

    public StarGreeterGLSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        setDebugFlags(DEBUG_CHECK_GL_ERROR);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new StarGreeterRenderer(context);
        setRenderer(mRenderer);

        // Render the view only when there is a change in the drawing data
        //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                mRenderer.setCurrentZoom(detector.getScaleFactor());
                requestRender();
                return false;
            }
        });

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                final int RESET_TAP_THRESHOLD = 50;
                if (e.getX() < RESET_TAP_THRESHOLD && e.getY() < RESET_TAP_THRESHOLD) {
                    Log.d(Utils.TAG, "Double tap in a top-left corner resets an app");
                    mRenderer.resetApp();
                } else {
                    Log.d(Utils.TAG, "Double tap reseta a view");
                    mRenderer.resetView();
                }
                return true;
            }
        });
    }

    private static final float TOUCH_MOVE_FACTOR = 0.5f;
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        mScaleDetector.onTouchEvent(e);
        mGestureDetector.onTouchEvent(e);

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (!mScaleDetector.isInProgress()) {

                    float dx = x - mPreviousX;
                    float dy = y - mPreviousY;
                    mRenderer.setCurrentTranslate(dx * TOUCH_MOVE_FACTOR, -dy * TOUCH_MOVE_FACTOR);
                    requestRender();
                }
        }

        mPreviousX = x;
        mPreviousY = y;
        return true;
    }
}
