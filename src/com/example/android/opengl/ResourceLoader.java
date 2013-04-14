package com.example.android.opengl;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceLoader {

    public static final String TAG = "ResourceLoader";

    private final Resources mContextResources;
    private final AssetManager mAssetManager;

    ResourceLoader(Context context) {
        this.mContextResources = context.getResources();
        this.mAssetManager = context.getAssets();
    }

    public String loadShader(final int resourceId) {
        final InputStream inputStream = mContextResources.openRawResource(
                resourceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(
                inputStream);
        final BufferedReader bufferedReader = new BufferedReader(
                inputStreamReader);

        String nextLine;
        final StringBuilder body = new StringBuilder();

        try {
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            return null;
        }

        String shaderCode = body.toString();
        Log.d(TAG, "Loaded shader for " + resourceId + ":\n" + shaderCode);
        return shaderCode;
    }

    public Bitmap loadBitmap(final int resourceId) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   // No pre-scaling
        return BitmapFactory.decodeResource(mContextResources, resourceId, options);
    }

    public AssetManager getAssetManager() {
        return mAssetManager;
    }
}
