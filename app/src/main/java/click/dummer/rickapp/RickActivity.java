package click.dummer.rickapp;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
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
    private static final String PROJECT_LINK = "https://no-go.github.io/RickApp/";
    private static final String FLATTR_ID = "o6wo7q";
    private String FLATTR_LINK;
    private static final int MS_AFTER_BLURP           = 1000;
    private final static int silenceMustBeeLongerThan = 8;

    int frequency = 4000;
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
            FLATTR_LINK = "https://flattr.com/submit/auto?fid="+FLATTR_ID+"&url="+
                java.net.URLEncoder.encode(PROJECT_LINK, "ISO-8859-1");
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
                Intent intentProj= new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_LINK));
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

        sampleRate = AudioRecord.getMinBufferSize(
                frequency,
                channelConfiguration,
                audioEncoding
        );

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT, frequency,
                channelConfiguration, audioEncoding, sampleRate*2);

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
    }

    @Override
    public void onClick(View view) {
        if (started) {
            started = false;
            startStopButton.setText(R.string.start);
            recordTask.cancel(true);
        } else {
            startTimeMs = System.currentTimeMillis();
            silenceTick = 0;
            firstNoise = false;
            started = true;
            startStopButton.setText(R.string.stop);
            toTransform = new double[bufferSize];
            recordTask = new RecordAudio();
            recordTask.execute();
        }
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
