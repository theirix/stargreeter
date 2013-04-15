package ru.omniverse.android.stargreeter;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

class ResourceLoader {

    private final Resources mContextResources;
    private final AssetManager mAssetManager;

    ResourceLoader(Context context) {
        this.mContextResources = context.getResources();
        this.mAssetManager = context.getAssets();
    }

    public Document loadXml(final int resourceId) {

        final InputStream inputStream = mContextResources.openRawResource(resourceId);

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            return builder.parse(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Can not load xml file", e);
        }
    }

    public String loadShader(final int resourceId) {
        final InputStream inputStream = mContextResources.openRawResource(resourceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String nextLine;
        final StringBuilder body = new StringBuilder();
        try {
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException("Can not read shader file", e);
        }

        return body.toString();
    }

    public Bitmap loadBitmap(final int resourceId) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   // No pre-scaling
        return BitmapFactory.decodeResource(mContextResources, resourceId, options);
    }


    private Map<String, Typeface> mFontCache = new HashMap<String, Typeface>();

    public Typeface loadCachedFont(String name) {
        if (mFontCache.containsKey(name)) {
            return mFontCache.get(name);
        }
        // Create the Typeface from Font File
        Typeface tf = Typeface.createFromAsset(mAssetManager, name + ".ttf");
        mFontCache.put(name, tf);
        return tf;
    }

}
