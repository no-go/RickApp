package click.dummer.notburpapp;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Process;
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
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private String FLATTR_LINK;

    private static final int MS_AFTER_BLURP           = 3500;
    private final static int silenceMustBeeLongerThan = 33;
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
    MediaPlayer mp[];
    int burps = 28;
    int lastBurp;

    int analyseSize = 128;
    int bufferSize;
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
        lastBurp = 0;
        mp = new MediaPlayer[burps];
        mp[0] = MediaPlayer.create(this, R.raw.petti1);
        mp[1] = MediaPlayer.create(this, R.raw.petti2);
        mp[2] = MediaPlayer.create(this, R.raw.petti3);
        mp[3] = MediaPlayer.create(this, R.raw.petti4);
        mp[4] = MediaPlayer.create(this, R.raw.petti5);
        mp[5] = MediaPlayer.create(this, R.raw.petti6);
        mp[6] = MediaPlayer.create(this, R.raw.petti7);
        mp[7] = MediaPlayer.create(this, R.raw.petti8);
        mp[8] = MediaPlayer.create(this, R.raw.petti9);
        mp[9] = MediaPlayer.create(this, R.raw.petti10);

        mp[10] = MediaPlayer.create(this, R.raw.petti11);
        mp[11] = MediaPlayer.create(this, R.raw.petti12);
        mp[12] = MediaPlayer.create(this, R.raw.petti13);
        mp[13] = MediaPlayer.create(this, R.raw.petti14);
        mp[14] = MediaPlayer.create(this, R.raw.petti15);
        mp[15] = MediaPlayer.create(this, R.raw.petti16);
        mp[16] = MediaPlayer.create(this, R.raw.petti17);
        mp[17] = MediaPlayer.create(this, R.raw.petti18);
        mp[18] = MediaPlayer.create(this, R.raw.petti19);
        mp[19] = MediaPlayer.create(this, R.raw.petti20);

        mp[20] = MediaPlayer.create(this, R.raw.petti21);
        mp[21] = MediaPlayer.create(this, R.raw.petti22);
        mp[22] = MediaPlayer.create(this, R.raw.petti23);
        mp[23] = MediaPlayer.create(this, R.raw.petti24);
        mp[24] = MediaPlayer.create(this, R.raw.petti25);
        mp[25] = MediaPlayer.create(this, R.raw.petti26);
        mp[26] = MediaPlayer.create(this, R.raw.petti27);
        mp[27] = MediaPlayer.create(this, R.raw.petti28);

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

        bufferSize = AudioRecord.getMinBufferSize(
                frequency,
                channelConfiguration,
                audioEncoding
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (bufferSize < (512 * 1024)) bufferSize = 512 * 1024;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                frequency,
                channelConfiguration,
                audioEncoding,
                bufferSize
        );

        if(audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(App.TAG, "Audio: INITIALIZATION ERROR");
            Toast.makeText(this, "Audio: INITIALIZATION ERROR", Toast.LENGTH_LONG).show();
            audioRecord.release();
            audioRecord = null;
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    @Override
    public void onClick(View view) {
        if (started) {
            stopRick();
        } else {
            startRick();
        }
    }

    @Override
    public void onBackPressed() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    void startRick() {
        startTimeMs = System.currentTimeMillis();
        silenceTick = 0;
        firstNoise = false;
        started = true;
        startStopButton.setText(R.string.stop);
        toTransform = new double[analyseSize];
        recordTask = new RecordAudio();
        recordTask.execute();
    }

    void stopRick() {
        started = false;
        startStopButton.setText(R.string.start);
        if (recordTask != null) recordTask.cancel(true);
    }

    private class RecordAudio extends AsyncTask<Void, double[], Void> {
        String text = "";

        @Override
        protected Void doInBackground(Void... params) {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
            try {
                short[] buffer = new short[analyseSize];
                double[] bufferParameter = new double[1];
                audioRecord.startRecording();
                while (started) {
                    int bufferReadResult = audioRecord.read(buffer, 0, analyseSize);

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
            text = "";
            int v, sum = 0;
            for (int i = 0; i < toTransfoo[1][0]; i++) {
                v = (int) Math.abs(toTransfoo[0][i] * 500.0);
                if (i%8 == 0) text += "\n";
                text += String.valueOf(v) + " ";
                sum += v;
            }
            txtLog.setText(text);
            long nowMilis = System.currentTimeMillis();

            if (sum < limit && (nowMilis-startTimeMs > MS_AFTER_BLURP) && firstNoise) {
                silenceTick++;
                startStopButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
                if (
                        silenceTick > silenceMustBeeLongerThan &&
                        !mp[lastBurp].isPlaying()
                ) {
                    silenceTick = 0;
                    Random r = new Random();
                    int rnd = r.nextInt(mp.length);
                    lastBurp = rnd;
                    mp[rnd].start();
                    startTimeMs = System.currentTimeMillis();
                    firstNoise = false;
                }
            } else {
                startStopButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent2));
                if (sum > limit && (nowMilis-startTimeMs > MS_AFTER_BLURP) ) {
                    firstNoise = true;
                }
            }

        }
    }
}
