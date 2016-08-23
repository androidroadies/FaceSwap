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
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.Toast;

import com.alex.faceswap.R;

import java.io.File;


public class MainActivityFaceSwap extends AppCompatActivity {

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
    private static ImageView image1;
    private static ImageView image2;
    // Paths to first and second images
    private String path1 = null, path2 = null;
    private Bitmap bitmap1 = null, bitmap2 = null;

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
        setContentView(R.layout.activity_main_activity_face_swap);

        // Use with snackbars
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.main_content);

        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());


        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        tabLayout.getTabAt(0).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_face_white_48dp, null));
        tabLayout.getTabAt(1).setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_person_white_48dp, null));

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

        // Update image view
        if (bitmap1 != null && bitmap2 != null) {

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
                        faceSwap.prepareSwapping();
                    } catch (FaceSwap.FaceSwapException e) {
                        e.printStackTrace();
                    }

                    swappedBitmap = faceSwap.portraitSwap();

                    Bitmap dest = Bitmap.createBitmap(swappedBitmap, 0, 0, bitmap2.getWidth(), bitmap2.getHeight());


                    if (tabLayout.getSelectedTabPosition() == 0) {
                        PlaceholderFragmentA ph = (PlaceholderFragmentA) mSectionsPagerAdapter.getItem(0);
                    } else {
                        PlaceholderFragmentB ph = (PlaceholderFragmentB) mSectionsPagerAdapter.getItem(1);
                    }


                    if (swappedBitmap != null) {
                        Intent intent = new Intent(MainActivityFaceSwap.this, ResultActivity.class);
                        GlobalBItmap.img = dest;
                        startActivity(intent);
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity_face_swap, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


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

        public PlaceholderFragmentA() {
        }

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
            View rootView = inflater.inflate(R.layout.fragment_main_activity_face_swap, container, false);
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

        public PlaceholderFragmentB() {
        }

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
            View rootView = inflater.inflate(R.layout.fragment_main_activity_face_swap, container, false);
            image2 = (ImageView) rootView.findViewById(com.tzutalin.dlib.R.id.image);

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
            if (position == 0)
                return PlaceholderFragmentA.newInstance(position + 1);

            return PlaceholderFragmentB.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 2;
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

        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            /* Handle if picture was taken with camera */

            if (resultCode == RESULT_OK) {
                Uri takenPhotoUri = getPhotoFileUri(photoFileName);
                path = takenPhotoUri.getPath();
                Bitmap bitmap = makeBitmap(path);

                if (tabLayout.getSelectedTabPosition() == 0) {
                    bitmap1 = bitmap;
                } else {
                    bitmap2 = bitmap;
                }

            } else {
                Snackbar.make(coordinatorLayout, "Picture wasn't taken!", Snackbar.LENGTH_LONG).show();
            }
        }


        /* Handle if picture was selected in the file browser */

        if (requestCode == RESULT_LOAD_IMAGE) {

            if (resultCode == RESULT_OK) {


                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                path = cursor.getString(columnIndex);
                cursor.close();

                Bitmap bitmap = makeBitmap(path);

                if (tabLayout.getSelectedTabPosition() == 0) {
                    bitmap1 = bitmap;
                } else {
                    bitmap2 = bitmap;
                }
            } else {

                Toast.makeText(this, "No picture was selected!", Toast.LENGTH_SHORT).show();
            }

        }

        if (path != null) {

            Bitmap selBitmap = makeBitmap(path);

            PlaceholderFragmentA phA = (PlaceholderFragmentA) mSectionsPagerAdapter.getItem(0);
            PlaceholderFragmentB phB = (PlaceholderFragmentB) mSectionsPagerAdapter.getItem(1);


            if (tabLayout.getSelectedTabPosition() == 0) {

                if (selBitmap.getWidth() * selBitmap.getHeight() < 160000) {
                    Toast.makeText(this, "Selected image is too small", Toast.LENGTH_LONG).show();
                    return;
                }

            } else {

                if (selBitmap.getWidth() * selBitmap.getHeight() < 160000) {
                    Toast.makeText(this, "Selected image is too small", Toast.LENGTH_LONG
                    ).show();
                    return;
                }
            }

            if (tabLayout.getSelectedTabPosition() == 0) phA.setImage(selBitmap);
            else phB.setImage(selBitmap);
        }

    }


    /**
     * Returns a bitmap of right size proportions.
     */
    private Bitmap makeBitmap(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        Bitmap bitmap2 = resizeBitmap(bitmap);

        return bitmap2;
    }


    /**
     * Returns a resized bitmap of bm,
     */
    private Bitmap resizeBitmap(Bitmap bm) {

        int h = bm.getHeight();
        int w = bm.getWidth();

        if (h == w) {
            h = 2048;
            w = 2048;
        } else if (h > w) {
            float ratio = (float) w / (float) h;
            h = 2048;
            w = (int) (2048.0 * ratio);
        } else {
            float ratio = (float) h / w;
            w = 2048;
            h = (int) (2048.0 * ratio);
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
