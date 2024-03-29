package ru.omniverse.android.stargreeter;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

@SuppressWarnings("FieldCanBeLocal")
class StarGreeterGLSurfaceView extends GLSurfaceView {

    private final StarGreeterRenderer mRenderer;

    private final ScaleGestureDetector mScaleDetector;
    private final GestureDetector mGestureDetector;

    private float mPreviousX;
    private float mPreviousY;

    public StarGreeterGLSurfaceView(final Activity activity, StarGreeterData starGreeterData) {
        super(activity);
        final Context context = activity;

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        setDebugFlags(DEBUG_CHECK_GL_ERROR);

        // Handler from the rendering thread
        Handler stopHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                activity.finish();
            }
        };
        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new StarGreeterRenderer(context, starGreeterData, stopHandler);
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
                final float RESET_TAP_THRESHOLD = 0.07f;
                if (e.getX() / getWidth() < RESET_TAP_THRESHOLD && e.getY() / getHeight() < RESET_TAP_THRESHOLD) {
                    Log.d(Utils.TAG, "Double tap in a top-left corner resets an app");
                    mRenderer.resetApp();

                    activity.finish();
                } else {
                    Log.d(Utils.TAG, "Double tap reseta a view");
                    mRenderer.resetView();
                }
                return true;
            }

            /*@Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

                mRenderer.beginFling(velocityX, velocityY);
                return true;
            } */

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });
    }

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
                    mRenderer.setCurrentMove(x - mPreviousX, -(y - mPreviousY));
                    requestRender();
                }
        }

        mPreviousX = x;
        mPreviousY = y;
        return true;
    }
}
