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

