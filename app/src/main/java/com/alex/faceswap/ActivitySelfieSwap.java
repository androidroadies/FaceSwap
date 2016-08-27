package com.alex.faceswap;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;


public class ActivitySelfieSwap extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    private Uri fileUri;
    public String photoFileName = "photo.jpg";

    /* Constants */
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int RESULT_LOAD_IMAGE = 200;
    private static final int MEDIA_TYPE_IMAGE = 1;

    // Objects to display images in
    private static ImageView image1, image2, image3;
    // Paths to first and second images
    private String path1 = null, path2 = null;
    private Bitmap bitmap1 = null, bitmap2 = null, bitmap3 = null;

    private int selectedTabIndex = 0;
    private TabLayout tabLayout;

    private CoordinatorLayout coordinatorLayout;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie_swap);

        // Use with snackbars
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.main_content);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        tabLayout.getTabAt(0).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_face_white_48dp, null));
        tabLayout.getTabAt(1).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_face_white_48dp, null));
        tabLayout.getTabAt(2).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_people_white_48dp, null));

        tabLayout.setOnTabSelectedListener(this);
    }


    /**
     * Starts the camera for collecting image with it.
     */
    public void cameraMode(View view) {
        if (checkPermissions()) {

            // Start camera
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, getPhotoFileUri(photoFileName));

            // Avoid crash
            if (intent.resolveActivity(getPackageManager()) != null)
                // Start the image capture intent to take photo
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

        } else
            Snackbar.make(view, "No permission to use camera", Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    /**
     * Opens the gallery.
     */
    public void galleryMode(View view) {
        if (checkPermissions()) {

            // Open photo album
            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, RESULT_LOAD_IMAGE);

        } else {
            Snackbar.make(view, "No permission to use memory", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    /**
     * Swaps faces.
     */
    public void swapMode(final View view) {

        int selectedTab = tabLayout.getSelectedTabPosition();
        final String[] errMessage = {null};
        final boolean[] allOk = {true};

        // Swap faces, selfie mode
        if (bitmap1 != null && bitmap2 != null && selectedTab <= 1) {

            // Make the images of equal size, otherwise nasty errors.
            int maxW = bitmap1.getWidth() > bitmap2.getWidth() ? bitmap1.getWidth() : bitmap2.getWidth();
            int maxH = bitmap1.getHeight() > bitmap2.getHeight() ? bitmap1.getHeight() : bitmap2.getHeight();

            Bitmap.Config conf = Bitmap.Config.ARGB_8888;
            Bitmap bmp1Temp = Bitmap.createBitmap(maxW, maxH, conf);
            Bitmap bmp2Temp = Bitmap.createBitmap(maxW, maxH, conf);

            final Bitmap bmp1 = overlay(bmp1Temp, bitmap1);
            final Bitmap bmp2 = overlay(bmp2Temp, bitmap2);


            Snackbar.make(view, "Swapping faces", Snackbar.LENGTH_LONG).setAction("Action", null).show();

            // Must have this for not to lock uithread
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    Bitmap swappedBitmap = null;

                    final FaceSwap faceSwap = new FaceSwap(getResources(), bmp1, bmp2);

                    try {
                        faceSwap.prepareSelfieSwapping();
                    } catch (FaceSwap.FaceSwapException e) {
                        errMessage[0] = e.getLocalizedMessage();
                        e.printStackTrace();
                        allOk[0] = false;
                        Snackbar.make(view, errMessage[0], Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }

                    // If no errors: proceed
                    if (allOk[0]) {

                        swappedBitmap = faceSwap.selfieSwap();

                        Bitmap dest = Bitmap.createBitmap(swappedBitmap, 0, 0, bitmap2.getWidth(), bitmap2.getHeight());


                        if (swappedBitmap != null) {
                            Intent intent = new Intent(ActivitySelfieSwap.this, ResultActivity.class);
                            GlobalBItmap.img = dest;
                            startActivity(intent);
                        }

                    }

                }
            }, 500);



        } else if (bitmap3 != null && selectedTab == 2) {
            // Swap faces, many faced mode
            Snackbar.make(view, "Swapping faces", Snackbar.LENGTH_LONG).setAction("Action", null).show();

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    Bitmap swappedBitmap = null;
                    final FaceSwap faceSwap = new FaceSwap(getResources(), bitmap3);

                    try {
                        faceSwap.prepareManyFacedSwapping();

                    } catch (FaceSwap.FaceSwapException e) {
                        errMessage[0] = e.getLocalizedMessage();
                        e.printStackTrace();
                        allOk[0] = false;
                        Snackbar.make(view, errMessage[0], Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }

                    if (allOk[0]) {
                        swappedBitmap = faceSwap.manyFacedSwap();

                        if (swappedBitmap != null) {
                            Intent intent = new Intent(ActivitySelfieSwap.this, ResultActivity.class);
                            GlobalBItmap.img = swappedBitmap;
                            startActivity(intent);
                        }
                    }
                }
            }, 500);


        } else {
            Snackbar.make(view, "Image not available :(", Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }


    }


    /**
     * Paste bmp2 over bmp1. The latter shall be zeros.
     */
    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, new Matrix(), null);
        return bmOverlay;
    }


    // Returns the Uri for a photo stored on disk given the fileName
    public Uri getPhotoFileUri(String fileName) {
        // Only continue if the SD Card is mounted
        if (isExternalStorageAvailable()) {
            // Get safe storage directory for photos
            File mediaStorageDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FaceSwap");

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                Log.d("FaceSwap", "failed to create directory");
            }

            // Return the file target for the photo based on filename
            return Uri.fromFile(new File(mediaStorageDir.getPath() + File.separator + fileName));
        }
        return null;
    }


    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }


    /**
     * -------------------------------------------------------------------------------------------
     * Code for Tab listeners
     * -------------------------------------------------------------------------------------------
     */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        PlaceholderFragmentA phA = (PlaceholderFragmentA) mSectionsPagerAdapter.getItem(0);
        PlaceholderFragmentB phB = (PlaceholderFragmentB) mSectionsPagerAdapter.getItem(1);
        PlaceholderFragmentC phC = (PlaceholderFragmentC) mSectionsPagerAdapter.getItem(2);

        switch (tabLayout.getSelectedTabPosition()) {
            case 0:
                if (bitmap1 != null)
                    phA.setImage(bitmap1);
                break;
            case 1:
                if (bitmap2 != null)
                    phB.setImage(bitmap2);
                break;
            case 2:
                if (bitmap3 != null)
                    phC.setImage(bitmap3);
                break;

        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) { /* Not used */ }

    @Override
    public void onTabReselected(TabLayout.Tab tab) { /* Not used */ }


    /**
     * -------------------------------------------------------------------------------------------
     * Code for Fragments
     * -------------------------------------------------------------------------------------------
     */

    /* First image Fragment --------------------------------------------------------------------- */

    public static class PlaceholderFragmentA extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        ImageView image = image1;

        public PlaceholderFragmentA() {}

        public void setImage(Bitmap bm) {
            image.setImageBitmap(bm);
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragmentA newInstance(int sectionNumber) {
            PlaceholderFragmentA fragment = new PlaceholderFragmentA();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_selfie_swap, container, false);
            image1 = (ImageView) rootView.findViewById(com.tzutalin.dlib.R.id.image);
            return rootView;
        }
    }


    /* Second image Fragment -------------------------------------------------------------------- */

    public static class PlaceholderFragmentB extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        ImageView image = image2;

        public PlaceholderFragmentB() {}

        public void setImage(Bitmap bm) {
            image.setImageBitmap(bm);
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragmentB newInstance(int sectionNumber) {
            PlaceholderFragmentB fragment = new PlaceholderFragmentB();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_selfie_swap, container, false);
            image2 = (ImageView) rootView.findViewById(com.tzutalin.dlib.R.id.image);

            return rootView;
        }
    }

    /* Fragment for many faced swaps ------------------------------------------------------------ */

    public static class PlaceholderFragmentC extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        ImageView image = image3;

        public PlaceholderFragmentC() {}

        public void setImage(Bitmap bm) {
            image.setImageBitmap(bm);
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragmentC newInstance(int sectionNumber) {
            PlaceholderFragmentC fragment = new PlaceholderFragmentC();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_selfie_swap, container, false);
            image3 = (ImageView) rootView.findViewById(com.tzutalin.dlib.R.id.image);

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // Return a PlaceholderFragment
            if (position == 0) return PlaceholderFragmentA.newInstance(position);
            if (position == 1) return PlaceholderFragmentB.newInstance(position);
            if (position == 2) return PlaceholderFragmentC.newInstance(position);

            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

    }


    /**
     * ----------------------------------------------------------------------------------------------
     * Code for handling images returned from camera or photo album -
     * ----------------------------------------------------------------------------------------------
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        String path = null;

        PlaceholderFragmentA phA = (PlaceholderFragmentA) mSectionsPagerAdapter.getItem(0);
        PlaceholderFragmentB phB = (PlaceholderFragmentB) mSectionsPagerAdapter.getItem(1);
        PlaceholderFragmentC phC = (PlaceholderFragmentC) mSectionsPagerAdapter.getItem(2);

        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            /* Handle if picture was taken with camera */

            if (resultCode == RESULT_OK) {
                Uri takenPhotoUri = getPhotoFileUri(photoFileName);
                path = takenPhotoUri.getPath();
                Bitmap bitmap = makeBitmap(path);

                if (tabLayout.getSelectedTabPosition() == 0) {
                    bitmap1 = bitmap;
                    phA.setImage(bitmap1);
                } else if (tabLayout.getSelectedTabPosition() == 1) {
                    bitmap2 = bitmap;
                    phB.setImage(bitmap2);
                } else if (tabLayout.getSelectedTabPosition() == 2) {
                    bitmap3 = bitmap;
                    phC.setImage(bitmap3);
                }

            } else {
                Snackbar.make(coordinatorLayout, "Picture wasn't taken!", Snackbar.LENGTH_LONG).show();
            }
        }


        /* Handle if picture was selected in the file browser */

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();
            Bitmap bitmap = null;

            if (selectedImage.toString().startsWith("content://com.google.android.apps.photos.content")) {
                // Selected image has to be downloaded
                try {
                    InputStream is = getContentResolver().openInputStream(selectedImage);
                    if (is != null) {
                        Bitmap bitmapTemp = BitmapFactory.decodeStream(is);
                        bitmap = resizeBitmap(bitmapTemp);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            } else {
                // Selected image is alreade stored in phone
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                path = cursor.getString(columnIndex);
                cursor.close();

                bitmap = makeBitmap(path);
            }

            if (bitmap != null) {
                if (tabLayout.getSelectedTabPosition() == 0) {
                    bitmap1 = bitmap;
                    phA.setImage(bitmap1);
                } else if (tabLayout.getSelectedTabPosition() == 1) {
                    bitmap2 = bitmap;
                    phB.setImage(bitmap2);
                } else if (tabLayout.getSelectedTabPosition() == 2) {
                    bitmap3 = bitmap;
                    phC.setImage(bitmap3);
                }
            }

        } else {
            Toast.makeText(this, "No picture was selected!", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Returns a bitmap of right size proportions.
     */
    private Bitmap makeBitmap(String path) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        Bitmap bitmap2 = resizeBitmap(bitmap);
        return bitmap2;
    }


    /**
     * Returns a resized bitmap of bm,
     */
    private Bitmap resizeBitmap(Bitmap bm) {

        int h = bm.getHeight();
        int w = bm.getWidth();

        int maxSize = 1200;

        if (h == w) {
            h = maxSize;
            w = maxSize;
        } else if (h > w) {
            float ratio = (float) w / (float) h;
            h = maxSize;
            w = (int) (maxSize * ratio);
        } else {
            float ratio = (float) h / w;
            w = maxSize;
            h = (int) (maxSize * ratio);
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, w, h, true);
        return resizedBitmap;
    }


    /**
     * -------------------------------------------------------------------------------------------
     * Permission code
     * -------------------------------------------------------------------------------------------
     */

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 123;
    private boolean granted = true;

    /**
     * Controls if an app has permission to use the camera and internal storage.
     *
     * @return
     */
    private boolean checkPermissions() {
        granted = true;

        // List the permissions
        String requests[] = {
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };

        for (int i = 0; i < requests.length; i++) {
            if (ContextCompat.checkSelfPermission(this, requests[i])
                    != PackageManager.PERMISSION_GRANTED) granted = false;
        }

        if (granted) return true;

        ActivityCompat.requestPermissions(this, requests, MY_PERMISSIONS_REQUEST_READ_CONTACTS);

        return granted;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    granted = true;
                } else
                    granted = false;
                return;
            }
        }
    }


}
