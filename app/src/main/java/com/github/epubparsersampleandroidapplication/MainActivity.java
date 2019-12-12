package com.github.epubparsersampleandroidapplication;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.core.view.MenuItemCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mertakdut.BookSection;
import com.github.mertakdut.CssStatus;
import com.github.mertakdut.Reader;
import com.github.mertakdut.exception.OutOfPagesException;
import com.github.mertakdut.exception.ReadingException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity implements PageFragment.OnFragmentReadyListener {

    private Reader reader;

    private ViewPager mViewPager;

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * (FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    DatabaseReference rootRef,demoRef;
    private static final String TAG = "SensorsTest";

    private int pageCount = Integer.MAX_VALUE;
    private int pxScreenWidth;

    private boolean isPickedWebView = false;
    private MenuItem searchMenuItem;
    private SearchView searchView;
    private DatabaseReference mDatabase;
    private boolean isSkippedToPage = false;

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener proximitySensorListener;

    private Sensor rotationVectorSensor;
    private SensorEventListener rvListener;

    private long mThreshold = 1500;
//    private long mScrollThreshold = 3000;
    private long startMilli;
    private long endMilli;

    private Button plusbtn;
    private Button minusbtn;
    private int textsize=25;
    String filePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //mDatabase = FirebaseDatabase.getInstance().getReference("stud-e-book");
        //writeNewUser("1","aarav","a@a.com");

        String[] usernameArray= {"John", "Paul", "Ringo", "George","Raj"};
        String[] emailArray= {"john@gmail.com", "pp@yahoo.in", "ring12@gmail.com", "geo1972@outlook.com","rajram@gmail.com"};
        String[] passwordArray= {"****", "*****", "****", "****","*****"};

        int numberOfItems = usernameArray.length;
        rootRef = FirebaseDatabase.getInstance().getReference("stud-e-book-details");
        for (int i=0; i<numberOfItems; i++)
        {
            String name = usernameArray[i];
            String email = emailArray[i];
            String pwd = passwordArray[i];
            rootRef.child("user details").child(name).child("email").setValue(email);
            rootRef.child("user details").child(name).child("pwd").setValue(pwd);
        }

        //rootRef = FirebaseDatabase.getInstance().getReference("stud-e-book-test");
        //demoRef = rootRef.child("stud-e-book");
        //String value = "hello help";
        //demoRef.push().setValue(value);
        //rootRef.child("namess").child("name").setValue("Sri");

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);


        pxScreenWidth = getResources().getDisplayMetrics().widthPixels;

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = findViewById(R.id.container);
        mViewPager.setOffscreenPageLimit(0);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // Initialize sensor manager
        sensorManager =
                (SensorManager) getSystemService(SENSOR_SERVICE);

        // Using proximity sensor
        proximitySensor =
                sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if(proximitySensor == null) {
            Log.e(TAG, "Proximity sensor not available.");
//            finish();
        }

        rotationVectorSensor =
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        if(rotationVectorSensor == null) {
            Log.e(TAG, "Rotation sensor not available.");
//            finish();
        }

        Button b_prox = findViewById(R.id.pbutton);
        Button b_gyro = findViewById(R.id.gbutton);
        Button b_normal = findViewById(R.id.nbutton);


        b_prox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "In Prox Button");

                sensorManager.unregisterListener(rvListener);
                sensorManager.registerListener(proximitySensorListener,
                        proximitySensor, 2 * 1000 * 1000);

                proximitySensorListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent) {
                        Log.d(TAG, "IN PROX SENSOR");
                        // More code goes here
                        if (sensorEvent.values[0] < proximitySensor.getMaximumRange()) {
                            //near
                            //Toast.makeText(mContext, "near", Toast.LENGTH_SHORT).show();
                            startMilli = System.currentTimeMillis();

                        } else {
                            //far
                            //Toast.makeText(mContext, "far", Toast.LENGTH_SHORT).show();
                            endMilli = System.currentTimeMillis();
                            changePage(endMilli - startMilli);
                        }
                    }

                    private void changePage(long l) {

                        //Log.e("Time: ", String.valueOf(l));
                        startMilli = 0;

                        if (l < mThreshold) {
                            //Toast.makeText(mContext, "Next ", Toast.LENGTH_SHORT).show();
                            int nextpage=mViewPager.getCurrentItem()+1;
                            mViewPager.setCurrentItem(nextpage);
                        }
                        else {
                            //Toast.makeText(mContext, "Previous", Toast.LENGTH_SHORT).show(); /*Copyright (c) @ 2019 Yash Prakash
                            //                 */
                            if(mViewPager.getCurrentItem()>0) {
                                int nextpage=mViewPager.getCurrentItem()-1;
                                mViewPager.setCurrentItem(nextpage);
                            }
                            else {
                                Toast.makeText(MainActivity.this, "You are on the first page!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int i) {
                    }
                };
            }
        });


        b_gyro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "In Gyro Button");

                sensorManager.unregisterListener(proximitySensorListener);
                sensorManager.registerListener(rvListener,
                        rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);

                rvListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent) {
                        Log.d(TAG, "IN GYRO SENSOR");
                        float[] rotationMatrix = new float[16];
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values);

                        float[] remappedRotationMatrix = new float[16];
                        SensorManager.remapCoordinateSystem(rotationMatrix,
                                SensorManager.AXIS_X,
                                SensorManager.AXIS_Z,
                                remappedRotationMatrix);

                        float[] orientations = new float[3];
                        SensorManager.getOrientation(remappedRotationMatrix, orientations);

                        for(int i = 0; i < 3; i++) {
                            orientations[i] = (float)(Math.toDegrees(orientations[i]));
                        }

                        if(orientations[2] > 45) {
                            int nextpage=mViewPager.getCurrentItem()+1;
                            mViewPager.setCurrentItem(nextpage);
                        } else if(orientations[2] < -45) {
                            if (mViewPager.getCurrentItem() > 0) {
                                int nextpage = mViewPager.getCurrentItem() - 1;
                                mViewPager.setCurrentItem(nextpage);
                            } else {
                                Toast.makeText(MainActivity.this, "You are on the first page!", Toast.LENGTH_SHORT).show();
                            }
                        }
//                 else if(Math.abs(orientations[2]) < 10) {
//                    getWindow().getDecorView().setBackgroundColor(Color.WHITE);
//                }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int i) {

                    }
                };
            }
        });


        b_normal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "NO SENSORS");
                sensorManager.unregisterListener(proximitySensorListener);
                sensorManager.unregisterListener(rvListener);
//                sensorManager.cancelTriggerSensor(proximitySensorListener, proximitySensor);
            }
        });



        if (getIntent() != null && getIntent().getExtras() != null) {
            filePath = getIntent().getExtras().getString("filePath");
            Log.d("FilePath",filePath);
            isPickedWebView = getIntent().getExtras().getBoolean("isWebView");

            try {
                reader = new Reader();

                // Setting optionals once per file is enough.
                reader.setMaxContentPerSection(1250);
                reader.setCssStatus(isPickedWebView ? CssStatus.INCLUDE : CssStatus.OMIT);
                reader.setIsIncludingTextContent(true);
                reader.setIsOmittingTitleTag(true);

                // This method must be called before readSection.
                reader.setFullContent(filePath);

//                int lastSavedPage = reader.setFullContentWithProgress(filePath);
                if (reader.isSavedProgressFound()) {
                    int lastSavedPage = reader.loadProgress();
                    mViewPager.setCurrentItem(lastSavedPage);
                }

            } catch (ReadingException e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }


        }
    }
    private void writeNewUser(String userId, String name, String email) {
        User user = new User(name, email);
        System.out.print(user);
        mDatabase.child("stud-e-book").child(userId).setValue(user);

    }

    private void insertBooks() {
//        insert all books with categories and titles

    }

    private void insertRecommendedBooks() {
//        insert all books with their recommended 3

    }


    private void trackBook() {
// insert user uuid and book uuid with page number
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(proximitySensorListener,
                proximitySensor, 2 * 1000 * 1000);
        sensorManager.registerListener(rvListener,
                rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(proximitySensorListener);
        sensorManager.unregisterListener(rvListener);
    }

    @Override
    public View onFragmentReady(int position) {

        BookSection bookSection = null;

        try {
            bookSection = reader.readSection(position);
        } catch (ReadingException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (OutOfPagesException e) {
            e.printStackTrace();
            this.pageCount = e.getPageCount();

            if (isSkippedToPage) {
                Toast.makeText(MainActivity.this, "Max page number is: " + this.pageCount, Toast.LENGTH_LONG).show();
            }

            mSectionsPagerAdapter.notifyDataSetChanged();
        }

        isSkippedToPage = false;

        if (bookSection != null) {
            return setFragmentView(isPickedWebView, bookSection.getSectionContent(), "text/html", "UTF-8"); // reader.isContentStyled
        }

        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                if (query != null && !query.equals("")) {

                    if (TextUtils.isDigitsOnly(query)) {
                        loseFocusOnSearchView();

                        int skippingPage = Integer.valueOf(query);

                        if (skippingPage >= 0) {
                            isSkippedToPage = true;
                            mViewPager.setCurrentItem(skippingPage);
                        } else {
                            Toast.makeText(MainActivity.this, "Page number can't be less than 0", Toast.LENGTH_LONG).show();
                        }

                    } else {
                        loseFocusOnSearchView();
                        Toast.makeText(MainActivity.this, "Only numbers are allowed", Toast.LENGTH_LONG).show();
                    }

                    return true;
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            loseFocusOnSearchView();
        } else {
            //final Intent intent = new Intent(this,FragmentOne.class);
            int startIndex = filePath.lastIndexOf("/");
            String FileName = filePath.substring(startIndex+1,filePath.length());
            Log.d("CurrentItem",FileName);

            Bundle bundle = new Bundle();
            bundle.putString("fileName", FileName);
            bundle.putString("activity","MainActivity");
            // set Fragmentclass Arguments
            FragmentOne fragOne = new FragmentOne();
            fragOne.setInstance(FileName);
            Log.d("Arguments","Passed");
            super.onBackPressed();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            reader.saveProgress(mViewPager.getCurrentItem());
            Toast.makeText(MainActivity.this, "Saved page: " + mViewPager.getCurrentItem() + "...", Toast.LENGTH_LONG).show();
        } catch (ReadingException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Progress is not saved: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (OutOfPagesException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Progress is not saved. Out of Bounds. Page Count: " + e.getPageCount(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private View setFragmentView(boolean isContentStyled, String data, String mimeType, String encoding) {

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        if (isContentStyled) {
            WebView webView = new WebView(MainActivity.this);
            webView.loadDataWithBaseURL(null, data, mimeType, encoding, null);

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//            }

            webView.setLayoutParams(layoutParams);

            return webView;
        } else {
            ScrollView scrollView = new ScrollView(MainActivity.this);
            scrollView.setLayoutParams(layoutParams);

            final TextView textView = new TextView(MainActivity.this);
            textView.setLayoutParams(layoutParams);

            plusbtn = findViewById(R.id.plus);
            minusbtn = findViewById(R.id.minus);
//            final int txtsize=(int)textView.getTextSize();

            plusbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    textsize += 5;
                    textView.setTextSize(textsize);
//                Log.d("HEREEE", "onClick: ");
                }
            });
            minusbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    textsize -= 5;
                    textView.setTextSize(textsize);
                }
            });

            textView.setTextSize(textsize);
            textView.setText(Html.fromHtml(data, new Html.ImageGetter() {
                @Override
                public Drawable getDrawable(String source) {
                    String imageAsStr = source.substring(source.indexOf(";base64,") + 8);
                    byte[] imageAsBytes = Base64.decode(imageAsStr, Base64.DEFAULT);
                    Bitmap imageAsBitmap = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);

                    int imageWidthStartPx = (pxScreenWidth - imageAsBitmap.getWidth()) / 2;
                    int imageWidthEndPx = pxScreenWidth - imageWidthStartPx;

                    Drawable imageAsDrawable = new BitmapDrawable(getResources(), imageAsBitmap);
                    imageAsDrawable.setBounds(imageWidthStartPx, 0, imageWidthEndPx, imageAsBitmap.getHeight());
                    return imageAsDrawable;
                }
            }, null));

            int pxPadding = dpToPx(12);

            textView.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);

            scrollView.addView(textView);
            return scrollView;
        }
    }

    private void loseFocusOnSearchView() {
        searchView.setQuery("", false);
        searchView.clearFocus();
        searchView.setIconified(true);
        MenuItemCompat.collapseActionView(searchMenuItem);
    }

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return pageCount;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            return PageFragment.newInstance(position);
        }
    }
}
