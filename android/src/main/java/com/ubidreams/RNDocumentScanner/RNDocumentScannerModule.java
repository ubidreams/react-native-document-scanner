package com.ubidreams.RNDocumentScanner;

import android.graphics.Bitmap;
import android.util.Log;

import com.adityaarora.liveedgedetection.util.ScanUtils;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import org.opencv.core.Point;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class RNDocumentScannerModule extends ReactContextBaseJavaModule {
    private final ReactApplicationContext reactContext;
    private final RNDocumentScannerManager manager;

    public RNDocumentScannerModule(ReactApplicationContext reactContext, RNDocumentScannerManager manager) {
        super(reactContext);

        this.reactContext = reactContext;
        this.manager = manager;
    }

    @Override
    public String getName() {
        return "RNDocumentScannerModule";
    }

    @ReactMethod
    public void crop(ReadableArray points, ReadableMap options, Promise promise) {
        WritableMap result = Arguments.createMap();

        // get points
        ReadableMap topLeftPoint = points.getMap(0);
        ReadableMap topRightPoint = points.getMap(1);
        ReadableMap bottomRightPoint = points.getMap(2);
        ReadableMap bottomLeftPoint = points.getMap(3);

        // get options
        int width = options.getInt("width");
        int height = options.getInt("height");
        boolean thumbnail = options.getBoolean("thumbnail");

        // go opencv !
        Point topLeft = new Point(topLeftPoint.getDouble("x"), topLeftPoint.getDouble("y"));
        Point topRight = new Point(topRightPoint.getDouble("x"), topRightPoint.getDouble("y"));
        Point bottomLeft = new Point(bottomLeftPoint.getDouble("x"), bottomLeftPoint.getDouble("y"));
        Point bottomRight = new Point(bottomRightPoint.getDouble("x"), bottomRightPoint.getDouble("y"));

        Bitmap croppedBitmap = ScanUtils.enhanceReceipt(manager.getCopyBitmap(), topLeft, topRight, bottomLeft, bottomRight);

        // resize cropped image ?
        if (width > 0 || height > 0) {
            croppedBitmap = this.resizeBitmap(croppedBitmap, width, height);
        }

        // save image to cache directory
        String croppedImageFilePath = this.saveBitmapToCacheDirectory(croppedBitmap);

        // add image file path to result
        result.putString("image", "file://" + croppedImageFilePath);

        // create thumbnail ?
        if (thumbnail) {
            // resize original image to create a thumbnail
            Bitmap thumbnailBitmap = this.resizeBitmap(croppedBitmap, 250, 250);

            // save thumbnail to cache directory
            String thumbnailFilePath = this.saveBitmapToCacheDirectory(thumbnailBitmap);

            // add thumbnail file path to result
            result.putString("thumbnail", "file://" + thumbnailFilePath);
        }

        // resolve promise with result
        promise.resolve(result);
    }

    private Bitmap resizeBitmap (Bitmap bitmap, int width, int height) {
        Bitmap resizedBitmap = null;
        float ratio;

        if (width < 0) width = 0;
        if (height < 0) height = 0;

        float ratioX = (float) width / bitmap.getWidth();
        float ratioY = (float) height / bitmap.getHeight();

        if (ratioX == 0 || ratioY == 0) {
            if (ratioX > 0) {
                ratio = ratioX;
            } else {
                ratio = ratioY;
            }
        } else {
            ratio = Math.min(ratioX, ratioY);
        }

        int finalWidth = (int) (bitmap.getWidth() * ratio);
        int finalHeight = (int) (bitmap.getHeight() * ratio);

        try {
            resizedBitmap = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
        } catch (OutOfMemoryError e) {
            Log.d(this.getName(), "Error resizing bitmap");
            e.printStackTrace();
        }

        return resizedBitmap;
    }

    private String saveBitmapToCacheDirectory (Bitmap bitmap) {
        FileOutputStream fos = null;
        String fileName = UUID.randomUUID().toString() + ".png";
        String imageFilePath = this.reactContext.getCacheDir().getAbsolutePath() + "/" + fileName;

        try {
            fos = new FileOutputStream(imageFilePath);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            Log.d(this.getName(), "Error writing file in cache");
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                Log.d(this.getName(), "Error closing file output stream");
                e.printStackTrace();
            }
        }

        return imageFilePath;
    }
}
