package com.ubidreams.RNDocumentScanner;

import android.graphics.PointF;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import java.util.List;

public class RNDocumentScannerModule extends ReactContextBaseJavaModule {

    private final String tag = "RNDocumentScanner";
    private final ReactApplicationContext reactContext;

    private int width;
    private int height;

    static {
        System.loadLibrary("opencv_java3");
    }

    public RNDocumentScannerModule(final ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNDocumentScanner";
    }

    @ReactMethod
    public void detectEdges(String imagePath, ReadableMap layout, Promise promise) {
        // get layout
        this.width = layout.getInt("width");
        this.height = layout.getInt("height");

        // get bitmap from path
        BitmapOpenCV bitmap = new BitmapOpenCV(this.reactContext, imagePath, width, height);

        // go opencv !
        List<PointF> pointsFound = bitmap.detectEdges();

        // build points array
        WritableArray points = new WritableNativeArray();

        for (int i = 0; i < 4; i++) {
            WritableMap point = new WritableNativeMap();
            point.putDouble("x", pointsFound.get(i).x);
            point.putDouble("y", pointsFound.get(i).y);

            points.pushMap(point);
        }

        // resolve promise
        promise.resolve(points);
    }

    @ReactMethod
    public void crop(ReadableArray points, Promise promise) {
    }

}
