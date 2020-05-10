package com.example.insulinpump;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

// Main activity is the interface handler (and activity_main.xml is the default display)
// Main activity is now also calculate.java

public class MainActivity extends WearableActivity {
    private static final String TAG = "MainActivity.java";

    // opens database access
    database_configuration db = new database_configuration(this);

    // for insulin calculations (was calculate.java)
    int insulin = 0;                // insulin dosage
    int basInsulin = 0;             // used for basal insulin
    int senGlucose = 160;             // glucose level detected on sensor
    int glucose = -1;               // glucose level used
    double total = 0;               // insulin amount given
    static double insulinReservoir = 200;  // available insulin

    // activity_main.xml:
    TextView lblGlucose;

    // insulin.xml,
    TextView txtGlucose, lblInsulin, txtInsulinUnits;
    Button btnBasal, btnBolus;

    // percent.xml
    Switch prefSensor;
    SeekBar prefResistance;
    boolean ifSensor = false; // uses sensor or not
    int resistance = 1; // user's resistance to insulin (0: sensitive; 1: normal; 2: resistant)

    // all xml:
    private TextView txtTime;
    DecimalFormat noZero = new DecimalFormat("#"); // deletes decimal point
    String currentView = "activity_main.xml"; // stores the currently active layout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // insulin.xml, activity_main.xml:
        lblGlucose = (TextView) findViewById(R.id.lblGlucoseInsulin);
        // Enables Always-on
        setAmbientEnabled();

        // Updates insulin units
        lblInsulin = findViewById(R.id.lblInsulinPercent);
        lblInsulin.setText(noZero.format(Math.floor(insulinReservoir)) + "u");

        // Loads top bar
        loadTop();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(broadcastReceiver);
    }

    // all xml -------------------------------------------------------------------------------------
    // Sets battery percentage in top bar
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);

            // Changes the battery percentage depending on active layout
            switch (currentView) {
                case "activity_main.xml" :
                    TextView lblBatteryPercent = findViewById(R.id.lblBatteryPercent);
                    lblBatteryPercent.setText(String.valueOf(level) + "%");
                    break;
                case "percent.xml" :
                    TextView txtBatteryPercent = findViewById(R.id.txtBatteryPercent);
                    TextView lblBatteryPercentPer = findViewById(R.id.lblBatteryPercentPer);
                    txtBatteryPercent.setText(String.valueOf(level) + "%");
                    lblBatteryPercentPer.setText(String.valueOf(level) + "%");
                    break;
                case "insulin.xml" :
                case "basal.xml" :
                case "bolus.xml" :
                    TextView lblBatteryInsulin = findViewById(R.id.lblBatteryInsulin);
                    lblBatteryInsulin.setText(String.valueOf(level) + "%");
                    break;
            }
        }
    };

    // Used to create a virtual blood glucose sensor
    public void mainInsulin() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    final int min = -1;
                    final int max = 1;
                    final int random = new Random().nextInt((max - min) + 1) + min;
                    String newGlucose = (String.valueOf(senGlucose + random));
                    senGlucose = Integer.parseInt(newGlucose);

                    // Updates glucose on default display
                    if (currentView.equals("activity_main.xml")) {
                        TextView txtCurrentInsulin = findViewById(R.id.lblGlucoseInsulin);
                        txtCurrentInsulin.setText(newGlucose);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "mainInsulin(): " + e.getMessage());
                }
            }
        });
    }

    // Used to create a clock on all the displays
    class CountDownRunner implements Runnable{
        // @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    // Updates the clock every second
                    switch (currentView) {
                        case "activity_main.xml" :
                            if (preference.ifSensor) {
                                mainInsulin();
                            }
                            mainTime();
                            break;
                        case "percent.xml" :
                            percentTime();
                            break;
                        case "insulin.xml" :
                        case "basal.xml" :
                        case "bolus.xml" :
                            insulinTime();
                            break;
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.d(TAG, "CountDownRunner : " + e.getMessage());
                }
            }
        }
    }

    // Clock for activity_main.xml
    public void mainTime() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    TextView txtCurrentTime = (TextView) findViewById(R.id.lblTimeLog);
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
                    String dateTime = simpleDateFormat.format(calendar.getTime());

                    txtCurrentTime.setText(dateTime);
                } catch (Exception e) {
                    Log.d(TAG, "mainTime(): " + e.getMessage());
                }
            }
        });
    }

    // Clock for insulin.xml
    public void insulinTime() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    TextView txtCurrentTime = (TextView) findViewById(R.id.lblTimeLog);
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
                    String dateTime = simpleDateFormat.format(calendar.getTime());

                    txtCurrentTime.setText(dateTime);
                } catch (Exception e) {}
            }
        });
    }

    // Clock for percent.xml
    public void percentTime() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    TextView txtCurrentTime = (TextView) findViewById(R.id.lblTimePercent);
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
                    String dateTime = simpleDateFormat.format(calendar.getTime());

                    txtCurrentTime.setText(dateTime);
                } catch (Exception e) {}
            }
        });
    }

    // Sets up the top bar (Clock, battery percent, insulin units) ********************************* insulin units need to be implemented
    public void loadTop() {
        // Running time thread
        Thread clock = null;
        Runnable runnable = new CountDownRunner();
        clock = new Thread(runnable);
        clock.start();

        // Updating battery percentage
        this.registerReceiver(this.broadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    // Feature to display an alert (used in calculate())
    public void alert(String title, String message) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert);
        AlertDialog finalAlert = alert.create();
        finalAlert.show();

        // Adjusting the alert
        TextView txtMessage = (TextView) finalAlert.findViewById(android.R.id.message);
        txtMessage.setTextSize(9);
        TextView txtTitle = finalAlert.findViewById(android.R.id.title);
        txtTitle.setGravity(Gravity.CENTER);
    }

    // Display functions: (all xml)-----------------------------------------------------------------

    // activity_main.xml
    public void goHome(android.view.View View) {
        setContentView(R.layout.activity_main);
        currentView = "activity_main.xml";

        // Updates insulin units
        lblInsulin = findViewById(R.id.lblInsulinPercent);
        lblInsulin.setText(noZero.format(Math.floor(insulinReservoir)) + "u");

        loadTop();
    }

    // percent.xml
    public void percentView(android.view.View View) {
        setContentView(R.layout.percent);
        currentView = "percent.xml";

        // Updates insulin units
        txtInsulinUnits = findViewById(R.id.txtInsulinUnits);
        TextView lblInsulinPer = findViewById(R.id.lblInsulinPercent);
        lblInsulinPer.setText(noZero.format(Math.floor(insulinReservoir)) + "u");
        txtInsulinUnits.setText("" + insulinReservoir);

        prefResistance = findViewById(R.id.prefResistance);
        prefSensor = findViewById(R.id.prefSensor);

        // Sets preferences
        prefResistance.setProgress(resistance);
        prefSensor.setChecked(ifSensor);

        // Preference Listeners:
        prefSensor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (prefSensor.isChecked()) {
                    preference.ifSensor = true;
                } else {
                    preference.ifSensor = false;
                }
            }
        });
        prefResistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // does nothing
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // does nothing
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (progress) {
                    case 0 :
                        resistance = 0;
                    case 1 :
                        resistance = 1;
                    case 2 :
                        resistance = 2;
                }
            }
        });

        loadTop();
    }

    // log_display.xml
    public void logView(android.view.View View) {
        setContentView(R.layout.log_display);
        currentView = "log_display.xml";
        displayData();
    }

    // insulin.xml;
    public void insulinView(android.view.View View) {
        setContentView(R.layout.insulin);
        currentView = "insulin.xml";

        txtGlucose = (TextView) findViewById(R.id.txtGlucoseInsulin);
        btnBasal = (Button) findViewById(R.id.btnBasal);
        btnBolus = (Button) findViewById(R.id.btnBolus);

        btnBasal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                basalView();
            }
        });
        btnBolus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bolusView();
            }
        });

        // Updates insulin units
        TextView lblInsulinPercent = findViewById(R.id.lblInsulinPercent);
        lblInsulinPercent.setText(noZero.format(Math.floor(insulinReservoir)) + "u");

//        db.resetLog(); //***************************************************************************TESTING (use to avoid a big log)
        loadTop();
    }

    public void basalView() {
        setContentView(R.layout.basal);
        currentView = "basal.xml";

        // Updates insulin units
        TextView lblInsulinPercent = findViewById(R.id.lblInsulinPercent);
        lblInsulinPercent.setText(noZero.format(Math.floor(insulinReservoir)) + "u");

        Button btnBasalInsulin = findViewById(R.id.btnBasalInsulin);

        btnBasalInsulin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                carbCalculation();
            }
        });

        loadTop();
    }

    public void bolusView() {
        setContentView(R.layout.bolus);
        currentView = "bolus.xml";

        // Updates insulin units
        TextView lblInsulinPercent = findViewById(R.id.lblInsulinPercent);
        lblInsulinPercent.setText(noZero.format(Math.floor(insulinReservoir)) + "u");

        TextView txtGlucoseInsulin = findViewById(R.id.txtGlucoseInsulin);

        // Updates glucose
        if (preference.ifSensor) {
            txtGlucoseInsulin.setText(senGlucose + "");
        } else {
            txtGlucoseInsulin.setText("");
        }

        Button btnBolusInsulin = findViewById(R.id.btnBolusInsulin);

        btnBolusInsulin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculate();
            }
        });

        loadTop();
    }

    // log_display.xml -----------------------------------------------------------------------------
    // Shows log data in ListView and uses loadTop();
    public void displayData() {
        Intent intent = new Intent(MainActivity.this, DisplayLog.class);
        startActivity(intent); // adds contents from table to a list and puts in current view
        finish();
    }

    // insulin.xml ---------------------------------------------------------------------------------
    // Receives glucose value
    public int getGlucose() {
        int glucose = -1;
        try {
            TextView txtGlucoseInsulin = findViewById(R.id.txtGlucoseInsulin);
            glucose = Integer.parseInt(txtGlucoseInsulin.getText().toString());
        } catch (Exception e) {
            throw new NumberFormatException("getGlucose(): Value inserted was not an Integer.");
        }
        if (glucose > 4000 || glucose < 0) {
            throw new ArithmeticException("getGlucose(): Glucose value is invalid. Must be a positive number no greater than 4000.");
        }
        return glucose;
    }

    // Calculates basal insulin dosage
    public void carbCalculation(){
        TextView txtCarbs = findViewById(R.id.txtCarbs);
        double getCarbs = Double.parseDouble(String.valueOf(txtCarbs.getText())); //gets the carbs from the edit text
        double calculatedUnits =  getCarbs/ 12; //the division of carbs divided by 12

        glucose = (int) getCarbs; // uses glucose to update log

        administerInsulin((int) calculatedUnits, true);
    }

    // Calculates bolus insulin dosage
    public void calculate() {
        // Resets values
        glucose = -1;
        insulin = 0;

        try {
            glucose = getGlucose();

            if (glucose >= 500) {
                try {
                    alert("HIGH GLUCOSE", "You have a very high blood glucose level, please call your doctor immediately.");
                } catch (Exception e) {
                    // ignore alert exception
                }
                insulin = 12;
            } else if (glucose >= 349) {
                insulin = 8;
            } else if (glucose >= 300) {
                insulin = 7;
            } else if (glucose >= 250) {
                insulin = 5;
            } else if (glucose >= 200) {
                insulin = 3;
            } else if (glucose >= 150) {
                insulin = 1;
            } else if (glucose <= 50) {
                try {
                    alert("LOW GLUCOSE", "You have a very low blood glucose level, please call your doctor immediately.");
                } catch (Exception e) {
                    // ignore alert exception
                }
            }

            // Calculating insulin-sensitivity or resistance
            if (resistance == 0) { // sensitive
                insulin -= Math.floor(insulin * 1.45 - insulin);
            } else if (resistance == 2) { // resistant
                insulin = (int) Math.floor(insulin * 1.33) + 1;
                if (insulin == 11) {
                    insulin = 12;
                } else if (insulin == 1) {
                    insulin = 0;
                }
            }

            administerInsulin(insulin, false);

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    // Insulin goes to needle and data is logged
    public void administerInsulin(int insulinDose, boolean isBasal) {
        boolean ifFail = false;
        String description = " ";

        // Check if insulin is available in reservoir
        if (insulinReservoir < insulinDose) {

            ifFail = true;

            // Display error if correct dose cannot be administered
            try {
                alert("LOW RESERVOIR", "Your insulin reservoir is too low and the correct dose cannot be administered.");
            } catch (Exception e) {
                // ignore alert exception
            }
        }

        // Injects the insulin
        try {
            if (isBasal) { // constant insulin supply
                basInsulin = insulinDose;
                // Running basal thread
                Thread basal = null;
                Runnable runnable = new BasalDosage();
                basal = new Thread(runnable);
                basal.start();
            } else { // instant insulin supply
                updateReservoir(insulinDose); // cancels running basal supply
            }
        } catch (Exception e) {
            ifFail = true;
            description = e.getMessage().substring(0, 255);
        }

        // Logging data
        Log.v(TAG, "administerInsulin(...) : getData(" + glucose + ", " + insulinDose + ", " + isBasal + ", " + ifFail + ", " + description + ")");
        db.addData(glucose, insulinDose, isBasal, ifFail, description);
    }

    // Steadily injects insulin at a rate of .5u/s *************************************************(rate would be slower in real-world)
    class BasalDosage implements Runnable {
        @Override
        public void run() {
            // Updates insulin
            while (total < basInsulin) {
                try {
                    commitBasal();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.d(TAG, "BasalDosage : " + e.getMessage());
                }
            }
        }
    }

    public void commitBasal() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    updateReservoir(.5); // removes units from reservoir
                    total += .5;

                    // Updates insulin text
                    TextView lblInsulinPercent = findViewById(R.id.lblInsulinPercent);
                    lblInsulinPercent.setText(noZero.format(Math.floor(insulinReservoir)) + "u");
                    if (currentView.equals("percent.xml")) {
                        TextView txtInsulinUnits = findViewById(R.id.txtInsulinUnits);
                        txtInsulinUnits.setText(insulinReservoir + "");
                    }
                } catch (Exception e) {
                    Log.d(TAG, "commitBasal() : " + e.getMessage());
                }
            }
        });
    }

    // Updates the insulin reservoir
    public void updateReservoir(double amount) {
        insulinReservoir -= amount;

        // Disallows negative values
        if (insulinReservoir < 0) {
            insulinReservoir = 0;
            try {
                alert("RESERVOIR EMPTY", "Your insulin reservoir is now empty.");
            } catch (Exception e) {
                // ignore alert exception
            }
        }
    }
}