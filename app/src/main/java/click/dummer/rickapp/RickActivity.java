package click.dummer.rickapp;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
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
    private static final String PROJECT_LINK = "https://github.com/no-go/RickApp";
    private static final String FLATTR_ID = "o6wo7q";
    private String FLATTR_LINK;
    private static final int MS_AFTER_BLURP           = 1000;
    private final static int silenceMustBeeLongerThan = 7;

    int frequency = 4000;
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    int blockSize = 128;
    Button startStopButton;
    boolean started = false;
    RecordAudio recordTask;
    TextView txtLog;
    int limit;
    int silenceTick;
    View homeView;
    MediaPlayer mp;

    int bufferSize;
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

        bufferSize = AudioRecord.getMinBufferSize(
                frequency,
                channelConfiguration,
                audioEncoding);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT, frequency,
                channelConfiguration, audioEncoding, bufferSize);

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
            toTransform = new double[blockSize];
            recordTask = new RecordAudio();
            recordTask.execute();
        }
    }

    private class RecordAudio extends AsyncTask<Void, double[], Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                short[] buffer = new short[blockSize];
                audioRecord.startRecording();
                while (started) {
                    int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; // signed 16 bit
                    }

                    publishProgress(toTransform);
                }
                audioRecord.stop();
            } catch (Throwable t) {
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }

        protected void onProgressUpdate(double[]... toTransfor) {
            txtLog.setText("");
            int x1, x2, x3, x4;
            int c1, c2, c3, c4;
            int sum = 0;
            for (int i = 0; i < toTransfor[0].length; i += 8) {
                x1 = (int) Math.abs(toTransfor[0][i] * 500.0);
                x2 = (int) Math.abs(toTransfor[0][i + 1] * 500.0);
                x3 = (int) Math.abs(toTransfor[0][i + 2] * 500.0);
                x4 = (int) Math.abs(toTransfor[0][i + 3] * 500.0);
                c1 = (int) Math.abs(toTransfor[0][i + 4] * 500.0);
                c2 = (int) Math.abs(toTransfor[0][i + 5] * 500.0);
                c3 = (int) Math.abs(toTransfor[0][i + 6] * 500.0);
                c4 = (int) Math.abs(toTransfor[0][i + 7] * 500.0);
                txtLog.append(
                        String.valueOf(x1) + " " + String.valueOf(x2) + " " +
                                String.valueOf(x3) + " " + String.valueOf(x4) + " " +
                                String.valueOf(c1) + " " + String.valueOf(c2) + " " +
                                String.valueOf(c3) + " " + String.valueOf(c4) + "\n"
                );
                sum += x1 + x2 + x3 + x4;
                sum += c1 + c2 + c3 + c4;
            }
            long nowMilis = System.currentTimeMillis();
            if (sum < limit && (nowMilis-startTimeMs > MS_AFTER_BLURP) && firstNoise) {
                silenceTick++;
                txtLog.append(getString(R.string.silence));
                homeView.setBackgroundColor(getColor(R.color.colorAccent));
                if (silenceTick > silenceMustBeeLongerThan && !mp.isPlaying()) {
                    silenceTick = 0;
                    mp.start();
                    startTimeMs = System.currentTimeMillis();
                    firstNoise = false;
                }
            } else {
                homeView.setBackgroundColor(getColor(R.color.colorAccent2));
                if (sum > limit && (nowMilis-startTimeMs > MS_AFTER_BLURP) ) {
                    firstNoise = true;
                }
            }

        }
    }
}
