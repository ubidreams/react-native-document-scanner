package com.ubidreams.RNDocumentScanner;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

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

import org.opencv.core.Point;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class RNDocumentScannerModule extends ReactContextBaseJavaModule {

    private final String tag = "RNDocumentScanner";
    private final ReactApplicationContext reactContext;
    private BitmapOpenCV bitmap;

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
        int width = layout.getInt("width");
        int height = layout.getInt("height");

        // get bitmap from path
        this.bitmap = new BitmapOpenCV(this.reactContext, imagePath, width, height);

        // go opencv !
        List<PointF> pointsFound = this.bitmap.detectEdges();

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
        // get points
        ReadableMap topLeftPoint = points.getMap(0);
        ReadableMap topRightPoint = points.getMap(1);
        ReadableMap bottomRightPoint = points.getMap(2);
        ReadableMap bottomLeftPoint = points.getMap(3);

        // go opencv !
        Point point1 = new Point(topLeftPoint.getDouble("x"), topLeftPoint.getDouble("y"));
        Point point2 = new Point(topRightPoint.getDouble("x"), topRightPoint.getDouble("y"));
        Point point3 = new Point(bottomRightPoint.getDouble("x"), bottomRightPoint.getDouble("y"));
        Point point4 = new Point(bottomLeftPoint.getDouble("x"), bottomLeftPoint.getDouble("y"));
        Point[] pts = {point1, point2, point3, point4};

        Bitmap croppedBitmap = this.bitmap.fourPointTransform(pts);

        // save image to cache directory
        FileOutputStream fos = null;
        String fileName = UUID.randomUUID().toString() + ".png";
        String croppedImageFilePath = this.reactContext.getCacheDir().getAbsolutePath() + "/" + fileName;

        try {
            fos = new FileOutputStream(croppedImageFilePath);
            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            Log.d(tag, "Error writing file in cache");
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                Log.d(tag, "Error closing file output stream");
                e.printStackTrace();
            }
        }

        // resolve promise
        promise.resolve("file://" + croppedImageFilePath);
    }

}
