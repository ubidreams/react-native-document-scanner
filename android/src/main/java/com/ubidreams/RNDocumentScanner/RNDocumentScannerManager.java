package com.ubidreams.RNDocumentScanner;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.adityaarora.liveedgedetection.enums.ScanHint;
import com.adityaarora.liveedgedetection.interfaces.IScanner;
import com.adityaarora.liveedgedetection.util.ScanUtils;
import com.adityaarora.liveedgedetection.view.Quadrilateral;
import com.adityaarora.liveedgedetection.view.ScanSurfaceView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;

public class RNDocumentScannerManager extends SimpleViewManager<ScanSurfaceView> implements IScanner {

  public static final String REACT_CLASS = "RNDocumentScanner";
  private ThemedReactContext reactContext;
  private ScanSurfaceView view;
  private ReadableMap scanHintOptions;
  private Bitmap copyBitmap;

  static {
    System.loadLibrary("opencv_java3");
  }

  public enum Events {
    EVENT_DISPLAY_HINT("displayHint"),
    EVENT_ON_PICTURE_CLICKED("onPictureClicked");

    private final String mName;

    Events(final String name) {
      mName = name;
    }

    @Override
    public String toString() {
      return mName;
    }
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Override
  protected ScanSurfaceView createViewInstance(ThemedReactContext reactContext) {
    this.reactContext = reactContext;

    Activity activity = reactContext.getCurrentActivity();
    if (activity != null) {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    view = new ScanSurfaceView(activity, this);
    return view;
  }

  @Override
  @Nullable
  public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
    for (Events event : Events.values()) {
      builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
    }
    return builder.build();
  }

  @ReactProp(name = "scanHintOptions")
  public void setScanHintOptions(ScanSurfaceView view, @Nullable ReadableMap scanHintOptions) {
    this.scanHintOptions = scanHintOptions;
  }


  @Override
  public void setPaintAndBorder(ScanHint scanHint, Paint paint, Paint border) {
    String colorHex = "";

    switch (scanHint) {
      case FIND_RECT:
        colorHex = scanHintOptions.getMap("findRect").getString("color");
        break;

      case MOVE_CLOSER:
        colorHex = scanHintOptions.getMap("moveCloser").getString("color");
        break;

      case MOVE_AWAY:
        colorHex = scanHintOptions.getMap("moveAway").getString("color");
        break;

      case ADJUST_ANGLE:
        colorHex = scanHintOptions.getMap("adjustAngle").getString("color");
        break;

      case CAPTURING_IMAGE:
        colorHex = scanHintOptions.getMap("capturingImage").getString("color");
        break;

      default:
        break;
    }

    int paintColor = Color.argb(
      80,
      Integer.valueOf(colorHex.substring(1, 3), 16),
      Integer.valueOf(colorHex.substring(3, 5), 16),
      Integer.valueOf(colorHex.substring(5, 7), 16)
    );
    int borderColor = Color.rgb(
      Integer.valueOf(colorHex.substring(1, 3), 16),
      Integer.valueOf(colorHex.substring(3, 5), 16),
      Integer.valueOf(colorHex.substring(5, 7), 16)
    );

    paint.setColor(paintColor);

    border.setStrokeWidth(4);
    border.setColor(borderColor);
  }

  @Override
  public void displayHint(ScanHint scanHint) {
    String type = "";

    switch (scanHint) {
      case  FIND_RECT:
        type = "findRect";
        break;

      case MOVE_CLOSER:
        type = "moveCloser";
        break;

      case MOVE_AWAY:
        type = "moveAway";
        break;

      case ADJUST_ANGLE:
        type = "adjustAngle";
        break;

      case CAPTURING_IMAGE:
        type = "capturingImage";
        break;

      default:
        break;
    }

    if (type.length() > 0) {
      WritableMap event = Arguments.createMap();
      event.putString("type", type);

      reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
        view.getId(),
        Events.EVENT_DISPLAY_HINT.toString(),
        event
      );
    }
  }

  @Override
  public void onPictureClicked(Bitmap bitmap) {
    try {
      copyBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

      int width = view.getWidth();
      int height = view.getHeight();

      Log.d("rom2", "width: " + view.getWidth() + ", height: " + view.getHeight());

      copyBitmap = ScanUtils.resizeToScreenContentSize(copyBitmap, width, height);

      Mat originalMat = new Mat(copyBitmap.getHeight(), copyBitmap.getWidth(), CvType.CV_8UC1);
      Utils.bitmapToMat(copyBitmap, originalMat);

      ArrayList<PointF> pointsF;

      try {
        Quadrilateral quad = ScanUtils.detectLargestQuadrilateral(originalMat);

        if (null != quad) {
          double resultArea = Math.abs(Imgproc.contourArea(quad.contour));
          double previewArea = originalMat.rows() * originalMat.cols();

          if (resultArea > previewArea * 0.08) {
            pointsF = new ArrayList<>();
            pointsF.add(new PointF((float) quad.points[0].x, (float) quad.points[0].y));
            pointsF.add(new PointF((float) quad.points[1].x, (float) quad.points[1].y));
            pointsF.add(new PointF((float) quad.points[2].x, (float) quad.points[2].y));
            pointsF.add(new PointF((float) quad.points[3].x, (float) quad.points[3].y));
          } else {
            pointsF = ScanUtils.getPolygonDefaultPoints(copyBitmap);
          }

        } else {
          pointsF = ScanUtils.getPolygonDefaultPoints(copyBitmap);
        }


        WritableMap event = Arguments.createMap();
        WritableArray points = Arguments.createArray();

        for (PointF pointF : pointsF) {
          WritableMap point = Arguments.createMap();
          point.putDouble("x", pointF.x);
          point.putDouble("y", pointF.y);

          points.pushMap(point);
        }

        event.putArray("points", points);

        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
          view.getId(),
          Events.EVENT_ON_PICTURE_CLICKED.toString(),
          event
        );
      } catch (Exception e) {
        Log.e(this.getName(), e.getMessage(), e);
      }
    } catch (Exception e) {
      Log.e(this.getName(), e.getMessage(), e);
    }
  }

}
