package com.alex.faceswap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import static org.opencv.android.Utils.bitmapToMat;
import static org.opencv.android.Utils.matToBitmap;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by alexander on 2016-06-29.
 */
public class FaceSwap {
    private Resources resources;
    private Context context;
    private ArrayList<ArrayList<PointF>> landmarks1, landmarks2;
    private Bitmap bitmap1, bitmap2;

    /* Native methods --------------------------------------------------------------------------- */

    public native void portraitSwapNative(   long addrImg1,
                                            long addrImg2,
                                            int[] landmarksX1,
                                            int[] landmarksY1,
                                            int[] landmarksX2,
                                            int[] landmarksY2,
                                            long addrResult);


    /* ------------------------------------------------------------------------------------------ */


    /** Load Native Library */
    static {
        if (!OpenCVLoader.initDebug()) {

        } else {
            System.loadLibrary("nativefaceswap");

        }
    }

    /**
     * Constructor, Only for Portrait swaps
     */
    public FaceSwap(Resources resources, Bitmap bitmap1, Bitmap bitmap2)  {
        this.resources = resources;
        this.bitmap1 = bitmap1;
        this.bitmap2 = bitmap2;
    }


    public void prepareSwapping() throws  FaceSwapException{
        // Get facial landmarks
        FacialLandmarkDetector landmarkDetector = new FacialLandmarkDetector();
        landmarks1 = landmarkDetector.detectPeopleAndLandmarks(bitmap1);
        landmarks2 = landmarkDetector.detectPeopleAndLandmarks(bitmap2);
    }


    public Bitmap portraitSwap() {

        ArrayList<PointF> pts1 = landmarks1.get(0);
        ArrayList<PointF> pts2 = landmarks2.get(1);

        int[] X1 = new int[pts1.size()];
        int[] Y1 = new int[pts1.size()];
        int[] X2 = new int[pts2.size()];
        int[] Y2 = new int[pts2.size()];

        for (int i = 0; i < pts1.size(); ++i) {
            int x = pts1.get(i).X();
            int y = pts1.get(i).Y();
            X1[i] = x;
            Y1[i] = y;
        }

        for (int i = 0; i < pts2.size(); ++i) {
            int x = pts2.get(i).X();
            int y = pts2.get(i).Y();
            X2[i] = x;
            Y2[i] = y;
        }

        Mat img1 = new Mat();
        bitmapToMat(bitmap1, img1);
        Mat img2 = new Mat();
        bitmapToMat(bitmap2, img2);


        Imgproc.cvtColor(img1,img1, Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(img2,img2, Imgproc.COLOR_BGRA2BGR);

        Mat swapped = new Mat();
        portraitSwapNative(img1.getNativeObjAddr(), img2.getNativeObjAddr(), X1, Y1, X2, Y2, swapped.getNativeObjAddr());

        matToBitmap(swapped, bitmap1);
        return  bitmap1;
    }


    public class FaceSwapException extends Exception {
        public FaceSwapException(String message) {
            super(message);
        }
    }


}
