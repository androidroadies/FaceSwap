package com.alex.faceswap;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceLandmark;
import com.tzutalin.dlib.PeopleDet;
import com.tzutalin.dlib.VisionDetRet;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;

/**
 * Created by alexander on 2016-07-03.
 */
public class FacialLandmarkDetector {

    private Resources resources;
    private ArrayList<ArrayList<PointF>> landmarks;    // Container for landmark points
    private int nbrOfFaces = 0;             // For track the number of faces

    /**
     * Constructor, resources needed for plotting
     */
    public FacialLandmarkDetector(Resources resources) {
        landmarks = new ArrayList<ArrayList<PointF>>();
        this.resources = resources;
    }


    public FacialLandmarkDetector() {
        landmarks = new ArrayList<ArrayList<PointF>>();
    }


    public int getNbrOfFaces() {
        return landmarks.size();
    }

    public ArrayList<ArrayList<PointF>> getLandmarks() {
        return landmarks;
    }

    /**
     * Detects faces and facial landmark of the bitmap with path path
     */
    public ArrayList<ArrayList<PointF>> detectPeopleAndLandmarks(Bitmap bm) {

        PeopleDet peopleDet = new PeopleDet();
        List<VisionDetRet> results = peopleDet.detBitmapFace(bm, Constants.getFaceShapeModelPath());


        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none
        if (bitmapConfig == null)
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;

        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bm = bm.copy(bitmapConfig, true);
        int width = bm.getWidth();
        int height = bm.getHeight();
        // By ratio scale
        float aspectRatio = bm.getWidth() / (float) bm.getHeight();

        final int MAX_SIZE = 2048;
        int newWidth = MAX_SIZE;
        int newHeight = MAX_SIZE;
        float resizeRatio = 1;
        newHeight = Math.round(newWidth / aspectRatio);

        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
            bm = getResizedBitmap(bm, newWidth, newHeight);
            resizeRatio = (float) bm.getWidth() / (float) width;
        }


        // Loop result list
        for (VisionDetRet ret : results) {
            Rect bounds = new Rect();
            bounds.left = (int) (ret.getLeft() * resizeRatio);
            bounds.top = (int) (ret.getTop() * resizeRatio);
            bounds.right = (int) (ret.getRight() * resizeRatio);
            bounds.bottom = (int) (ret.getBottom() * resizeRatio);

            // temporary container
            ArrayList<PointF> tempPoints = new ArrayList<PointF>();


            // Get landmark
            FaceLandmark landmark = ret.getFaceLandmark();
            if (landmark != null) {
                for (int index = 0; index != landmark.getLandmarkPointSize(); index++) {
                    Point point = landmark.getLandmarkPoint(index);
                    int pointX = (int) (point.x * resizeRatio);
                    int pointY = (int) (point.y * resizeRatio);

                    PointF pt = new PointF(point.x, point.y);
                    tempPoints.add(pt);

                }
            }

            // Add landmarks of one face
            landmarks.add(tempPoints);
        }

        return landmarks;
    }


    @DebugLog
    protected Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
        return resizedBitmap;
    }

}
