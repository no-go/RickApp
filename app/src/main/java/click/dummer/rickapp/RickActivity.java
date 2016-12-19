package click.dummer.rickapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;

public class RickActivity extends AppCompatActivity implements View.OnClickListener {
    private String FLATTR_LINK;

    private static final int MS_AFTER_BLURP           = 1000;
    private final static int silenceMustBeeLongerThan = 16;
    int frequency = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    Button startStopButton;
    boolean started = false;
    RecordAudio recordTask;
    TextView txtLog;
    int limit;
    int silenceTick;
    View homeView;
    MediaPlayer mp;

    int bufferSize = 128;
    int sampleRate;
    AudioRecord audioRecord;
    double[] toTransform;

    long startTimeMs;
    boolean firstNoise;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            FLATTR_LINK = "https://flattr.com/submit/auto?fid="+App.FLATTR_ID+"&url="+
                java.net.URLEncoder.encode(App.PROJECT_LINK, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_flattr:
                Intent intentFlattr = new Intent(Intent.ACTION_VIEW, Uri.parse(FLATTR_LINK));
                startActivity(intentFlattr);
                break;
            case R.id.action_project:
                Intent intentProj= new Intent(Intent.ACTION_VIEW, Uri.parse(App.PROJECT_LINK));
                startActivity(intentProj);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rick);
        homeView = findViewById(R.id.activity_rick);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        }
        startStopButton = (Button) findViewById(R.id.StartStopButton);
        txtLog = (TextView) findViewById(R.id.txtLog);
        startStopButton.setOnClickListener(this);
        SeekBar sb = (SeekBar) findViewById(R.id.seekBar);

        limit = sb.getProgress();
        mp = MediaPlayer.create(this, R.raw.blurp);
        toggleFullscreen();

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                limit = i;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        Log.i("Intent", i.getAction());
        if (i.getAction().equals("Stop")) {
            stopRick();
            controlNotify();
        } else if (i.getAction().equals("Start")) {
            startRick();
            controlNotify();
        }
    }

    @Override
    public void onClick(View view) {
        if (started) {
            stopRick();
        } else {
            startRick();
        }
        controlNotify();
    }

    @Override
    public void onBackPressed() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    void startRick() {
        sampleRate = AudioRecord.getMinBufferSize(
                frequency,
                channelConfiguration,
                audioEncoding
        );
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT, frequency,
                channelConfiguration, audioEncoding, sampleRate*4);

        startTimeMs = System.currentTimeMillis();
        silenceTick = 0;
        firstNoise = false;
        started = true;
        startStopButton.setText(R.string.stop);
        toTransform = new double[bufferSize];
        recordTask = new RecordAudio();
        recordTask.execute();
    }

    void stopRick() {
        started = false;
        startStopButton.setText(R.string.start);
        if (recordTask != null) recordTask.cancel(true);
    }

    void controlNotify() {
        ServiceConnection mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                ((KillNotificationsService.KillBinder) iBinder).service.startService(new Intent(
                        RickActivity.this, KillNotificationsService.class));

                // ===================================================================

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(RickActivity.this);
                NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
                bigStyle.bigText(getString(R.string.underControl));

                mBuilder.setTicker(getString(R.string.underControl));
                mBuilder.setContentTitle(getString(R.string.app_name));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
                }
                mBuilder.setContentText(getString(R.string.underControl));
                mBuilder.setStyle(bigStyle);
                mBuilder.setSmallIcon(R.mipmap.ic_launcher);
                mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
                Intent intent2 = new Intent(
                        RickActivity.this,
                        RickActivity.class
                );
                intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                if (started) {
                    intent2.setAction("Stop");
                    PendingIntent piP = PendingIntent.getActivity(
                            RickActivity.this, 0, intent2, PendingIntent.FLAG_CANCEL_CURRENT
                    );
                    mBuilder.addAction(android.R.drawable.ic_media_pause, getString(R.string.stop), piP);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mBuilder.setCategory(Notification.CATEGORY_PROGRESS);
                        mBuilder.setProgress(1000, 5, true);
                    }
                } else {
                    intent2.setAction("Start");
                    PendingIntent piP = PendingIntent.getActivity(
                            RickActivity.this, 0, intent2, PendingIntent.FLAG_CANCEL_CURRENT
                    );
                    mBuilder.addAction(android.R.drawable.ic_media_play, getString(R.string.start), piP);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mBuilder.setCategory(Notification.CATEGORY_PROGRESS);
                        mBuilder.setProgress(1000, 300, false);
                    }
                }
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notification noti = mBuilder.build();
                // noti.flags |= Notification.FLAG_NO_CLEAR;
                nm.notify(App.NOTIFYID, noti);

                // =============================================================
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {}
        };

        bindService(
                new Intent(RickActivity.this, KillNotificationsService.class),
                mConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    public void toggleFullscreen() {
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;

        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    private class RecordAudio extends AsyncTask<Void, double[], Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                short[] buffer = new short[bufferSize];
                double[] bufferParameter = new double[1];
                audioRecord.startRecording();
                while (started) {
                    int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);

                    for (int i = 0; i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
                    }
                    bufferParameter[0] = bufferReadResult; // :-(
                    publishProgress(toTransform, bufferParameter);
                }
                audioRecord.stop();
            } catch (Throwable t) {
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }

        protected void onProgressUpdate(double[]... toTransfoo) {
            txtLog.setText("");
            int v, sum = 0;
            for (int i = 0; i < toTransfoo[1][0]; i++) {
                v = (int) Math.abs(toTransfoo[0][i] * 500.0);
                if (i%8 == 0) txtLog.append("\n");
                txtLog.append(String.valueOf(v) + " ");
                sum += v;
            }
            long nowMilis = System.currentTimeMillis();

            if (sum < limit && (nowMilis-startTimeMs > MS_AFTER_BLURP) && firstNoise) {
                silenceTick++;
                txtLog.append("\n"+getString(R.string.silence));
                homeView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                if (silenceTick > silenceMustBeeLongerThan && !mp.isPlaying()) {
                    silenceTick = 0;
                    mp.start();
                    startTimeMs = System.currentTimeMillis();
                    firstNoise = false;
                }
            } else {
                homeView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent2));
                if (sum > limit && (nowMilis-startTimeMs > MS_AFTER_BLURP) ) {
                    firstNoise = true;
                }
            }

        }
    }
}
