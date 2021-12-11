package com.altimeter.bdureau.bearconsole.Flight;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.altimeter.bdureau.bearconsole.ConsoleApplication;
import com.altimeter.bdureau.bearconsole.Help.AboutActivity;
import com.altimeter.bdureau.bearconsole.Help.HelpActivity;
import com.altimeter.bdureau.bearconsole.R;

import com.altimeter.bdureau.bearconsole.ShareHandler;
import com.altimeter.bdureau.bearconsole.connection.SearchBluetooth;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.afree.data.xy.XYSeries;
import org.afree.data.xy.XYSeriesCollection;
import org.afree.graphics.geom.Font;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import static java.lang.Math.abs;

public class FlightViewTabActivity extends AppCompatActivity {
    private FlightData myflight = null;
    private ViewPager mViewPager;
    SectionsPageAdapter adapter;
    private Tab1Fragment flightPage1 = null;
    private Tab2Fragment flightPage2 = null;
    private Button btnDismiss, buttonMap, butSelectCurves;
    private static ConsoleApplication myBT;
    private static double FEET_IN_METER = 1;

    private static String curvesNames[] = null;
    private static String currentCurvesNames[] = null;
    private static boolean[] checkedItems = null;
    private XYSeriesCollection allFlightData = null;
    private static XYSeriesCollection flightData = null;
    private static ArrayList<ILineDataSet> dataSets;
    static int colors[] = {Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED,
            Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW, Color.RED, Color.BLUE, Color.BLACK,
            Color.GREEN, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.YELLOW};
    static Font font;
    private static String FlightName = null;
    private static XYSeries speed;
    private static XYSeries accel;

    private static String[] units = null;
    public static String SELECTED_FLIGHT = "MyFlight";
    public static int numberOfCurves = 0;
    File imagePath;

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray("CURRENT_CURVES_NAMES_KEY", currentCurvesNames);
        outState.putBooleanArray("CHECKED_ITEMS_KEY", checkedItems);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentCurvesNames = savedInstanceState.getStringArray("CURRENT_CURVES_NAMES_KEY");
        checkedItems = savedInstanceState.getBooleanArray("CHECKED_ITEMS_KEY");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_ASK_PERMISSIONS = 123;
            int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
        }
        // recovering the instance state
        if (savedInstanceState != null) {
            currentCurvesNames = savedInstanceState.getStringArray("CURRENT_CURVES_NAMES_KEY");
            checkedItems = savedInstanceState.getBooleanArray("CHECKED_ITEMS_KEY");
        }

        //get the connection pointer
        myBT = (ConsoleApplication) getApplication();

        //Check the local and force it if needed
        //getApplicationContext().getResources().updateConfiguration(myBT.getAppLocal(), null);

        setContentView(R.layout.activity_flight_view_tab);
        mViewPager = (ViewPager) findViewById(R.id.container);


        btnDismiss = (Button) findViewById(R.id.butDismiss);
        buttonMap = (Button) findViewById(R.id.butMap);
        butSelectCurves = (Button) findViewById(R.id.butSelectCurves);

        if (myBT.getAltiConfigData().getAltimeterName().equals("AltiGPS")) {
            buttonMap.setVisibility(View.VISIBLE);
            numberOfCurves = 7;
        } else {
            buttonMap.setVisibility(View.INVISIBLE);
            numberOfCurves = 5;
        }


        Intent newint = getIntent();
        FlightName = newint.getStringExtra(FlightListActivity.SELECTED_FLIGHT);
        myflight = myBT.getFlightData();
        // get all the data that we have recorded for the current flight
        allFlightData = new XYSeriesCollection();
        allFlightData = myflight.GetFlightData(FlightName);

        //calculate speed
        //altitude
        speed = null;
        //speed =getSpeedSerie(allFlightData.getSeries(getResources().getString(R.string.curve_altitude)));
        speed = allFlightData.getSeries(getResources().getString(R.string.curve_speed));

        // calculate acceleration
        accel = null;
        //accel = getAccelSerie(speed);
        accel = allFlightData.getSeries(getResources().getString(R.string.curve_accel));

        // by default we will display the altitude
        // but then the user will be able to change the data
        flightData = new XYSeriesCollection();
        //altitude
        flightData.addSeries(allFlightData.getSeries(getResources().getString(R.string.curve_altitude)));

        // get a list of all the curves that have been recorded
        //int numberOfCurves = allFlightData.getSeries().size();

        Log.d("numberOfCurves", "numberOfCurves:" + allFlightData.getSeries().size());
        curvesNames = new String[numberOfCurves];
        units = new String[numberOfCurves];
        for (int i = 0; i < numberOfCurves; i++) {
            curvesNames[i] = allFlightData.getSeries(i).getKey().toString();
        }

        // Read the application config
        myBT.getAppConf().ReadConfig();
        //metrics
        if (myBT.getAppConf().getUnits().equals("0")) {
            //Meters
            units[0] = "(" + getResources().getString(R.string.Meters_fview) + ")";
            units[3] = "(m/secs)";
            units[4] = "(m/secs²)";
        }
        //imperial
        else {
            //Feet
            units[0] = getResources().getString(R.string.Feet_fview);
            //(feet/secs)
            units[3] = "(" + getResources().getString(R.string.unit_feet_per_secs) + ")";
            //(feet/secs²)
            units[4] = "(" + getResources().getString(R.string.unit_feet_per_square_secs) + ")";
        }
        units[1] = "(°C)";
        units[2] = "(mbar)";

        if (myBT.getAppConf().getUnits().equals("0")) {//meters
            FEET_IN_METER = 1;
        } else {
            FEET_IN_METER = 3.28084;
        }

        if (currentCurvesNames == null) {
            //This is the first time so only display the altitude
            dataSets = new ArrayList<>();
            currentCurvesNames = new String[curvesNames.length];
            currentCurvesNames[0] = this.getResources().getString(R.string.curve_altitude);
            checkedItems = new boolean[curvesNames.length];
            checkedItems[0] = true;
        }
        setupViewPager(mViewPager);


        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                allFlightData = null;
                finish();      //exit the application configuration activity
            }
        });

        butSelectCurves.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int numberOfCurves = flightData.getSeries().size();
                currentCurvesNames = new String[numberOfCurves];

                for (int i = 0; i < numberOfCurves; i++) {
                    currentCurvesNames[i] = flightData.getSeries(i).getKey().toString();
                }
                // Set up the alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(FlightViewTabActivity.this);

                checkedItems = new boolean[curvesNames.length];
                // Add a checkbox list
                for (int i = 0; i < curvesNames.length; i++) {
                    if (Arrays.asList(currentCurvesNames).contains(curvesNames[i]))
                        checkedItems[i] = true;
                    else
                        checkedItems[i] = false;
                }


                builder.setMultiChoiceItems(curvesNames, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        // The user checked or unchecked a box
                    }
                });
                // Add OK and Cancel buttons
                builder.setPositiveButton(getResources().getString(R.string.fv_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The user clicked OK
                        flightPage1.drawGraph();
                        flightPage1.drawAllCurves(allFlightData);
                    }
                });
                //cancel
                builder.setNegativeButton(getResources().getString(R.string.fv_cancel), null);

                // Create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }

        });
        //this has a GPS so display Map =>FlightViewMapsActivity
        buttonMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i;
                // Make an intent to start next activity.
                i = new Intent(FlightViewTabActivity.this, FlightViewMapsActivity.class);

                //Change the activity.
                i.putExtra(SELECTED_FLIGHT, FlightName);
                startActivity(i);
            }
        });
    }


    private void setupViewPager(ViewPager viewPager) {
        adapter = new SectionsPageAdapter(getSupportFragmentManager());
        flightPage1 = new Tab1Fragment(allFlightData);
        flightPage2 = new Tab2Fragment(myflight, allFlightData);

        adapter.addFragment(flightPage1, "TAB1");
        adapter.addFragment(flightPage2, "TAB2");

        viewPager.setAdapter(adapter);
    }

    public class SectionsPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList();
        private final List<String> mFragmentTitleList = new ArrayList();

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        public SectionsPageAdapter(FragmentManager fm) {
            super(fm);
        }


        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }
    }

    public static class Tab1Fragment extends Fragment {
        private LineChart mChart;
        public XYSeriesCollection allFlightData;

        int graphBackColor, fontSize, axisColor, labelColor, nbrColor;

        public Tab1Fragment(XYSeriesCollection data) {
            this.allFlightData = data;
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.tabflight_view_mp_fragment, container, false);

            mChart = (LineChart) view.findViewById(R.id.linechart);

            mChart.setDragEnabled(true);
            mChart.setScaleEnabled(true);
            drawGraph();
            drawAllCurves(allFlightData);

            return view;
        }

        private void drawGraph() {

            graphBackColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphBackColor()));
            fontSize = myBT.getAppConf().ConvertFont(Integer.parseInt(myBT.getAppConf().getFontSize()));
            axisColor = myBT.getAppConf().ConvertColor(Integer.parseInt(myBT.getAppConf().getGraphColor()));
            labelColor = Color.BLACK;
            nbrColor = Color.BLACK;
        }

        private void drawAllCurves(XYSeriesCollection allFlightData) {
            dataSets.clear();

            flightData = new XYSeriesCollection();
            for (int i = 0; i < curvesNames.length; i++) {
                Log.d("drawAllCurves", "i:" + i);
                Log.d("drawAllCurves", "curvesNames:" + curvesNames[i]);
                if (checkedItems[i]) {
                    flightData.addSeries(allFlightData.getSeries(curvesNames[i]));

                    int nbrData = allFlightData.getSeries(i).getItemCount();

                    ArrayList<Entry> yValues = new ArrayList<>();

                    for (int k = 0; k < nbrData; k++) {
                        yValues.add(new Entry(allFlightData.getSeries(i).getX(k).floatValue(), allFlightData.getSeries(i).getY(k).floatValue()));
                    }

                    LineDataSet set1 = new LineDataSet(yValues, getResources().getString(R.string.flight_time));
                    set1.setColor(colors[i]);

                    set1.setDrawValues(false);
                    set1.setDrawCircles(false);
                    set1.setLabel(curvesNames[i] + " " + units[i]);
                    set1.setValueTextColor(labelColor);

                    set1.setValueTextSize(fontSize);
                    dataSets.add(set1);

                }
            }

            LineData data = new LineData(dataSets);
            mChart.clear();
            mChart.setData(data);
            mChart.setBackgroundColor(graphBackColor);
            Description desc = new Description();
            //time (ms)
            desc.setText(getResources().getString(R.string.unit_time));
            mChart.setDescription(desc);
        }
    }

    /*
    This is the flight information tab
     */
    public static class Tab2Fragment extends Fragment {

        private FlightData myflight;
        XYSeriesCollection allFlightData;
        private TextView nbrOfSamplesValue, flightNbrValue;
        private TextView apogeeAltitudeValue, flightDurationValue, burnTimeValue, maxVelociyValue, maxAccelerationValue;
        private TextView timeToApogeeValue, mainAltitudeValue, maxDescentValue, landingSpeedValue;

        private AlertDialog.Builder builder = null;
        private AlertDialog alert;

        String SavedCurves = "";
        boolean SavedCurvesOK = false;

        public Tab2Fragment(FlightData data, XYSeriesCollection data2) {

            myflight = data;
            this.allFlightData = data2;
        }

        public void msg(String s) {
            Toast.makeText(getActivity().getApplicationContext(), s, Toast.LENGTH_LONG).show();
        }

        private Button buttonExportToCsv;
        int nbrSeries;

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            View view = inflater.inflate(R.layout.tabflight_info_fragment, container, false);

            buttonExportToCsv = (Button) view.findViewById(R.id.butExportToCsv);
            apogeeAltitudeValue = view.findViewById(R.id.apogeeAltitudeValue);
            flightDurationValue = view.findViewById(R.id.flightDurationValue);
            burnTimeValue = view.findViewById(R.id.burnTimeValue);
            maxVelociyValue = view.findViewById(R.id.maxVelociyValue);
            maxAccelerationValue = view.findViewById(R.id.maxAccelerationValue);
            timeToApogeeValue = view.findViewById(R.id.timeToApogeeValue);
            mainAltitudeValue = view.findViewById(R.id.mainAltitudeValue);
            maxDescentValue = view.findViewById(R.id.maxDescentValue);
            landingSpeedValue = view.findViewById(R.id.landingSpeedValue);
            nbrOfSamplesValue = view.findViewById(R.id.nbrOfSamplesValue);
            flightNbrValue = view.findViewById(R.id.flightNbrValue);

            XYSeriesCollection flightData;

            flightData = myflight.GetFlightData(FlightName);
            int nbrData = flightData.getSeries(0).getItemCount();
            nbrSeries = flightData.getSeriesCount();
            // flight nbr
            flightNbrValue.setText(FlightName + "");

            //nbr of samples
            nbrOfSamplesValue.setText(nbrData + "");

            //flight duration
            double flightDuration = flightData.getSeries(0).getMaxX() / 1000;
            flightDurationValue.setText(String.format("%.2f",flightDuration) + " secs");
            //apogee altitude
            double apogeeAltitude = flightData.getSeries(0).getMaxY();
            apogeeAltitudeValue.setText(String.format("%.0f",apogeeAltitude) + " " + myBT.getAppConf().getUnitsValue());

            //apogee time
            int pos = searchX(flightData.getSeries(0), apogeeAltitude);
            double apogeeTime = (double) flightData.getSeries(0).getX(pos);
            timeToApogeeValue.setText(String.format("%.2f",apogeeTime / 1000) + " secs");

            //calculate max speed
            double maxSpeed = speed.getMaxY();
            maxVelociyValue.setText((long) maxSpeed + " " + myBT.getAppConf().getUnitsValue() + "/secs");

            //landing speed
            double landingSpeed = 0;
            if (searchY(speed, flightData.getSeries(0).getMaxX() - 2000) != -1) {
                landingSpeed = speed.getY(searchY(speed, flightData.getSeries(0).getMaxX() - 2000)).doubleValue();
                landingSpeedValue.setText((long) landingSpeed+ " " + myBT.getAppConf().getUnitsValue() + "/secs");
            } else {
                landingSpeedValue.setText("N/A");
            }
            //max descente speed
            double maxDescentSpeed = 0;
            if (searchY(speed, apogeeTime + 2000) != -1) {
                maxDescentSpeed = speed.getY(searchY(speed, apogeeTime + 2000)).doubleValue();
                maxDescentValue.setText((long) maxDescentSpeed+ " " + myBT.getAppConf().getUnitsValue() + "/secs");
            } else {
                maxDescentValue.setText("N/A");
            }
            //max acceleration value
            double maxAccel = accel.getMaxY();
            maxAccel = (maxAccel * FEET_IN_METER) / 9.80665;

            maxAccelerationValue.setText((long) maxAccel + " G");

            //burntime value
            double burnTime = 0;
            if (searchX(speed, maxSpeed) != -1)
                burnTime = speed.getX(searchX(speed, maxSpeed)).doubleValue();
            if (burnTime != 0)
                burnTimeValue.setText(String.format("%.2f",burnTime / 1000) + " secs");
            else
                burnTimeValue.setText("N/A");
            //main value
            // remain TODO!!!
            mainAltitudeValue.setText(" " + myBT.getAppConf().getUnitsValue());

            buttonExportToCsv.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    SavedCurves = "";
                    SavedCurvesOK = true;
                    //export the data to a csv file
                    for (int j = 0; j < numberOfCurves; j++) {
                        Log.d("Flight win", "Saving curve:" + j);
                        saveData(j, allFlightData);
                    }
                    builder = new AlertDialog.Builder(Tab2Fragment.this.getContext());
                    //Running Saving commands
                    //getResources().getString(R.string.flight_time)
                    if(SavedCurvesOK) {
                        builder.setMessage(getResources().getString(R.string.save_curve_msg) + Environment.DIRECTORY_DOWNLOADS + "\\BearConsoleFlights \n" + SavedCurves)
                                .setTitle(getResources().getString(R.string.save_curves_title))
                                .setCancelable(false)
                                .setPositiveButton(R.string.save_curve_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(final DialogInterface dialog, final int id) {
                                        dialog.cancel();
                                    }
                                });

                        alert = builder.create();
                        alert.show();
                        msg(getResources().getString(R.string.curves_saved_msg));
                    }
                    else {
                        msg("Failed saving flights");
                    }

                }
            });

            return view;
        }

        private void saveData(int nbr, XYSeriesCollection Data) {

            String valHeader = "altitude";

            if (nbr == 0) {
                valHeader = getResources().getString(R.string.curve_altitude);
            } else if (nbr == 1) {
                valHeader = getResources().getString(R.string.curve_temperature);
            } else if (nbr == 2) {
                valHeader = getResources().getString(R.string.curve_pressure);
            } else if (nbr == 3) {
                valHeader = getResources().getString(R.string.curve_speed);
            } else if (nbr == 4) {
                valHeader = getResources().getString(R.string.curve_accel);
            } else if (nbr == 5) {
                valHeader = getResources().getString(R.string.curve_latitude);
            } else if (nbr == 6) {
                valHeader = getResources().getString(R.string.curve_longitude);
            }


            String csv_data = "time(ms)," + valHeader + " " + units[nbr] + "\n";/// your csv data as string;

            int nbrData = Data.getSeries(nbr).getItemCount();
            for (int i = 0; i < nbrData; i++) {
                csv_data = csv_data + (double) Data.getSeries(nbr).getX(i) + "," + (double) Data.getSeries(nbr).getY(i) + "\n";
            }
            File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            //if you want to create a sub-dir
            root = new File(root, "BearConsoleFlights");
            root.mkdir();

            SimpleDateFormat sdf = new SimpleDateFormat("_dd-MM-yyyy_hh-mm-ss");
            String date = sdf.format(System.currentTimeMillis());

            // select the name for your file
            root = new File(root, FlightName + "-" + Data.getSeries(nbr).getKey().toString() + date + ".csv");
            Log.d("Flight win", FlightName + Data.getSeries(nbr).getKey().toString() + date + ".csv");
            try {
                Log.d("Flight win", "attempt to write");
                FileOutputStream fout = new FileOutputStream(root);
                fout.write(csv_data.getBytes());
                fout.close();
                Log.d("Flight win", "write done");
                SavedCurves = SavedCurves +
                        FlightName + Data.getSeries(nbr).getKey().toString() + date + ".csv\n";

            } catch (FileNotFoundException e) {
                e.printStackTrace();

                boolean bool = false;
                try {
                    // try to create the file
                    bool = root.createNewFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                if (bool) {
                    // call the method again
                    saveData(nbr, Data);
                } else {
                    Log.d("Flight win", "Failed to create flight files");
                    //msg("failed");
                    SavedCurvesOK=false;
                    //throw new IllegalStateException(getString(R.string.failed_create_flight_file_msg));// DOES NOT WORK !!
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*
        Return the position of the first X value it finds from the beginning
         */
        public int searchX(XYSeries serie, double searchVal) {
            int nbrData = serie.getItemCount();
            int pos = -1;
            for (int i = 1; i < nbrData; i++) {
                if ((searchVal >= serie.getY(i - 1).doubleValue()) && (searchVal <= serie.getY(i).doubleValue())) {
                    pos = i;
                    break;
                }
            }
            return pos;
        }

        /*
        Return the position of the first Y value it finds from the beginning
         */
        public int searchY(XYSeries serie, double searchVal) {
            int nbrData = serie.getItemCount();
            int pos = -1;
            for (int i = 1; i < nbrData; i++) {
                if ((searchVal >= serie.getX(i - 1).doubleValue()) && (searchVal <= serie.getX(i).doubleValue())) {
                    pos = i;
                    break;
                }
            }
            return pos;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_flights, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //open application settings screen
        if (id == R.id.action_share) {
            ShareHandler.share(ShareHandler.takeScreenshot(findViewById(android.R.id.content).getRootView()), this.getApplicationContext());
            return true;
        }
        //open help screen
        if (id == R.id.action_help) {
            Intent i = new Intent(this, HelpActivity.class);
            i.putExtra("help_file", "help_flight");
            startActivity(i);
            return true;
        }
        //open about screen
        if (id == R.id.action_about) {
            Intent i = new Intent(this, AboutActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}