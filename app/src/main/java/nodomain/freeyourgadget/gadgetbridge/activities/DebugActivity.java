package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.PropertyReader;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAdapter;
import nodomain.freeyourgadget.gadgetbridge.database.ActivityDatabaseHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.*;

public class DebugActivity extends GBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(DebugActivity.class);

    private static final String EXTRA_REPLY = "reply";
    private static final String ACTION_REPLY
            = "nodomain.freeyourgadget.gadgetbridge.DebugActivity.action.reply";

    private Button sendSMSButton;
    private Button sendEmailButton;
    private Button incomingCallButton;
    private Button outgoingCallButton;
    private Button startCallButton;
    private Button endCallButton;
    private Button testNotificationButton;
    private Button setMusicInfoButton;
    private Button setTimeButton;
    private Button rebootButton;
    private Button HeartRateButton;
    private Button exportDBButton;
    private Button importDBButton;
    private Button importOldActivityDataButton;
    private Button deleteDBButton;

    private EditText editContent;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case GBApplication.ACTION_QUIT: {
                    finish();
                    break;
                }
                case ACTION_REPLY: {
                    Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                    CharSequence reply = remoteInput.getCharSequence(EXTRA_REPLY);
                    LOG.info("got wearable reply: " + reply);
                    GB.toast(context, "got wearable reply: " + reply, Toast.LENGTH_SHORT, GB.INFO);
                    break;
                }
                case DeviceService.ACTION_HEARTRATE_MEASUREMENT: {
                    int hrValue = intent.getIntExtra(DeviceService.EXTRA_HEART_RATE_VALUE, -1);
                    GB.toast(DebugActivity.this, "Heart Rate measured: " + hrValue, Toast.LENGTH_LONG, GB.INFO);
                    transmitData(hrValue);
                    break;
                }
            }
        }
    };

    private PropertyReader propertyReader;
    private Properties properties;

    protected void transmitData(int hrValue){
        //send the heart rate to the server.
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(DebugActivity.this);

        propertyReader = new PropertyReader(DebugActivity.this);
        properties = propertyReader.getMyProperties("app.properties");
        String url = properties.getProperty("url");
        System.out.println(url);

        try {
            JSONObject data;

            data = new JSONObject("{\n" +
                    "      \"device_id\": "+ properties.getProperty("deviceid") +", \n" +
                    "      \"heart_rate\": "+ hrValue +",\n" +
                    "      \"time\": "+ System.currentTimeMillis()/1000 +"\n" +
                    "}");
            System.out.println(data);

// Request a string response from the provided URL.
            JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST, url, data,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // Display the first 500 characters of the response string.
                            GB.toast(DebugActivity.this, "Data synced" , Toast.LENGTH_LONG, GB.INFO);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.println(error.getMessage());
                    System.out.println(error.getStackTrace());
                    GB.toast(DebugActivity.this, "Data Synced", Toast.LENGTH_LONG, GB.INFO);
                }
            }){

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String,String> params = new HashMap<String, String>();
                    params.put("Content-Type","application/json");
                    return params;
                }
            };

// Add the request to the RequestQueue.
            queue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        IntentFilter filter = new IntentFilter();
        filter.addAction(GBApplication.ACTION_QUIT);
        filter.addAction(ACTION_REPLY);
        filter.addAction(DeviceService.ACTION_HEARTRATE_MEASUREMENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
        registerReceiver(mReceiver, filter); // for ACTION_REPLY

        editContent = (EditText) findViewById(R.id.editContent);
        sendSMSButton = (Button) findViewById(R.id.sendSMSButton);
        sendSMSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationSpec notificationSpec = new NotificationSpec();
                notificationSpec.phoneNumber = editContent.getText().toString();
                notificationSpec.body = editContent.getText().toString();
                notificationSpec.type = NotificationType.SMS;
                notificationSpec.id = -1;
                GBApplication.deviceService().onNotification(notificationSpec);
            }
        });
        sendEmailButton = (Button) findViewById(R.id.sendEmailButton);
        sendEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationSpec notificationSpec = new NotificationSpec();
                notificationSpec.sender = getResources().getText(R.string.app_name).toString();
                notificationSpec.subject = editContent.getText().toString();
                notificationSpec.body = editContent.getText().toString();
                notificationSpec.type = NotificationType.EMAIL;
                notificationSpec.id = -1;
                GBApplication.deviceService().onNotification(notificationSpec);
            }
        });

        incomingCallButton = (Button) findViewById(R.id.incomingCallButton);
        incomingCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_INCOMING;
                callSpec.number = editContent.getText().toString();
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });
        outgoingCallButton = (Button) findViewById(R.id.outgoingCallButton);
        outgoingCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_OUTGOING;
                callSpec.number = editContent.getText().toString();
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });

        startCallButton = (Button) findViewById(R.id.startCallButton);
        startCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_START;
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });
        endCallButton = (Button) findViewById(R.id.endCallButton);
        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_END;
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });

        exportDBButton = (Button) findViewById(R.id.exportDBButton);
        exportDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDB();
            }
        });
        importDBButton = (Button) findViewById(R.id.importDBButton);
        importDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importDB();
            }
        });

        importOldActivityDataButton = (Button) findViewById(R.id.mergeOldActivityData);
        importOldActivityDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mergeOldActivityDbContents();
            }
        });

        deleteDBButton = (Button) findViewById(R.id.emptyDBButton);
        deleteDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteActivityDatabase();
            }
        });

        rebootButton = (Button) findViewById(R.id.rebootButton);
        rebootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onReboot();
            }
        });

        final ScheduledExecutorService hrScheduler = Executors.newScheduledThreadPool(1);
        final Runnable hrPing = new Runnable() {
            public void run() {
                GB.toast("Measuring heart rate, please wait...", Toast.LENGTH_LONG, GB.INFO);
                GBApplication.deviceService().onHeartRateTest();
            }
        };


        HeartRateButton = (Button) findViewById(R.id.HearRateButton);
        HeartRateButton.setOnClickListener(new View.OnClickListener() {
            boolean hrPingStatus = false;
            ScheduledFuture hrPingHandle;
            @Override
            public void onClick(View v) {
                GB.toast("Measuring heart rate", Toast.LENGTH_LONG, GB.INFO);
                GBApplication.deviceService().onHeartRateTest();
                if(!hrPingStatus) {
                    hrPingStatus = true;
                    //start ping
                    GB.toast("Starting heart rate service", Toast.LENGTH_LONG, GB.INFO);
                    hrPingHandle = hrScheduler.scheduleAtFixedRate(hrPing,60 , 60, SECONDS);
                }else{
                    GB.toast("Ending heart rate service", Toast.LENGTH_LONG, GB.INFO);
                    hrPingStatus = false;
                    hrPingHandle.cancel(true);
                }
            }
        });

        setMusicInfoButton = (Button) findViewById(R.id.setMusicInfoButton);
        setMusicInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicSpec musicSpec = new MusicSpec();
                musicSpec.artist = editContent.getText().toString() + "(artist)";
                musicSpec.album = editContent.getText().toString() + "(album)";
                musicSpec.track = editContent.getText().toString() + "(track)";
                musicSpec.duration = 10;
                musicSpec.trackCount = 5;
                musicSpec.trackNr = 2;

                GBApplication.deviceService().onSetMusicInfo(musicSpec);

                MusicStateSpec stateSpec = new MusicStateSpec();
                stateSpec.position = 0;
                stateSpec.state = 0x01; // playing
                stateSpec.playRate = 100;
                stateSpec.repeat = 1;
                stateSpec.shuffle = 1;

                GBApplication.deviceService().onSetMusicState(stateSpec);
            }
        });

        setTimeButton = (Button) findViewById(R.id.setTimeButton);
        setTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onSetTime();
            }
        });

        testNotificationButton = (Button) findViewById(R.id.testNotificationButton);
        testNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testNotification();
            }
        });
    }

    private void exportDB() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DBHelper helper = new DBHelper(this);
            File dir = FileUtils.getExternalFilesDir();
            File destFile = helper.exportDB(dbHandler, dir);
            GB.toast(this, "Exported to: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception ex) {
            GB.toast(this, "Error exporting DB: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    private void importDB() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Import Activity Data?")
                .setMessage("Really overwrite the current activity database? All your activity data (if any) will be lost.")
                .setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try (DBHandler dbHandler = GBApplication.acquireDB()) {
                            DBHelper helper = new DBHelper(DebugActivity.this);
                            File dir = FileUtils.getExternalFilesDir();
                            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
                            File sourceFile = new File(dir, sqLiteOpenHelper.getDatabaseName());
                            helper.importDB(dbHandler, sourceFile);
                            helper.validateDB(sqLiteOpenHelper);
                            GB.toast(DebugActivity.this, "Import successful.", Toast.LENGTH_LONG, GB.INFO);
                        } catch (Exception ex) {
                            GB.toast(DebugActivity.this, "Error importing DB: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void mergeOldActivityDbContents() {
        final DBHelper helper = new DBHelper(getBaseContext());
        final ActivityDatabaseHandler oldHandler = helper.getOldActivityDatabaseHandler();
        if (oldHandler == null) {
            GB.toast(this, "No old activity database found, nothing to import.", Toast.LENGTH_LONG, GB.ERROR);
            return;
        }
        selectDeviceForMergingActivityDatabaseInto(new DeviceSelectionCallback() {
            @Override
            public void invoke(final GBDevice device) {
                if (device == null) {
                    GB.toast(DebugActivity.this, "No connected device to associate old activity data with.", Toast.LENGTH_LONG, GB.ERROR);
                    return;
                }
                try (DBHandler targetHandler = GBApplication.acquireDB()) {
                    final ProgressDialog progress = ProgressDialog.show(DebugActivity.this, "Merging Activity Data", "Please wait while merging your activity data...", true, false);
                    new AsyncTask<Object,ProgressDialog,Object>() {
                        @Override
                        protected Object doInBackground(Object[] params) {
                            helper.importOldDb(oldHandler, device, targetHandler);
                            progress.dismiss();
                            return null;
                        }
                    }.execute((Object[]) null);
                } catch (Exception ex) {
                    GB.toast(DebugActivity.this, "Error importing old activity data into new database.", Toast.LENGTH_LONG, GB.ERROR, ex);
                }
            }
        });
    }

    private void selectDeviceForMergingActivityDatabaseInto(final DeviceSelectionCallback callback) {
        GBDevice connectedDevice = GBApplication.getDeviceManager().getSelectedDevice();
        if (connectedDevice == null) {
            callback.invoke(null);
            return;
        }
        final List<GBDevice> availableDevices = Collections.singletonList(connectedDevice);
        GBDeviceAdapter adapter = new GBDeviceAdapter(getBaseContext(), availableDevices);

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Associate old Data with Device")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GBDevice device = availableDevices.get(which);
                        callback.invoke(device);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // ignore, just return
                    }
                })
                .show();
    }

    private void deleteActivityDatabase() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Delete Activity Data?")
                .setMessage("Really delete the entire activity database? All your activity data will be lost.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (GBApplication.deleteActivityDatabase()) {
                            GB.toast(DebugActivity.this, "Activity database successfully deleted.", Toast.LENGTH_SHORT, GB.INFO);
                        } else {
                            GB.toast(DebugActivity.this, "Activity database deletion failed.", Toast.LENGTH_SHORT, GB.INFO);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void testNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), DebugActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);

        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_REPLY)
                .build();

        Intent replyIntent = new Intent(ACTION_REPLY);

        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(this, 0, replyIntent, 0);

        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(android.R.drawable.ic_input_add, "Reply", replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender().addAction(action);

        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.test_notification))
                .setContentText(getString(R.string.this_is_a_test_notification_from_gadgetbridge))
                .setTicker(getString(R.string.this_is_a_test_notification_from_gadgetbridge))
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .extend(wearableExtender);

        nManager.notify((int) System.currentTimeMillis(), ncomp.build());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        unregisterReceiver(mReceiver);
    }

    public interface DeviceSelectionCallback {
        void invoke(GBDevice device);
    }
}
