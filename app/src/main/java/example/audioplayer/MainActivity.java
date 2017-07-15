package example.audioplayer;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.view.KeyEvent;
import android.os.Handler;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.io.IOException;
import java.math.BigDecimal;

public class MainActivity extends ListActivity implements OnPreparedListener, OnCompletionListener {

    private static final int UPDATE_FREQUENCY = 500;
    private static final int STEP_VALUE = 4000;

    private MediaCursorAdapter mediaAdapter = null;
    private TextView selectedFile = null;
    private SeekBar seekbar = null;
    private MediaPlayer player = null;
    private ImageButton playButton = null;
    private ImageButton prevButton = null;
    private ImageButton nextButton = null;
    private ImageButton mute;
    private ImageButton unmute;

    private boolean isStarted = true;
    private String currentFile = "";
    private boolean isMoveingSeekBar = false;

    private final Handler handler = new Handler();

    private final Runnable updatePositionRunnable = new Runnable() {
        public void run() {
            updatePosition();
        }
    };

    AudioManager am;
    CheckBox loop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        selectedFile = (TextView) findViewById(R.id.selectedfile);
        seekbar = (SeekBar) findViewById(R.id.seekbar);
        playButton = (ImageButton) findViewById(R.id.play);
        prevButton = (ImageButton) findViewById(R.id.prev);
        nextButton = (ImageButton) findViewById(R.id.next);
        mute = (ImageButton) this.findViewById(R.id.mute);
        unmute = (ImageButton) this.findViewById(R.id.unmute);
        loop = (CheckBox) findViewById(R.id.loop);

        player = new MediaPlayer();

        player.setOnCompletionListener(onCompletion);
        player.setOnErrorListener(onError);
        seekbar.setOnSeekBarChangeListener(seekBarChanged);

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();

            mediaAdapter = new MediaCursorAdapter(this, R.layout.listitem, cursor);

            setListAdapter(mediaAdapter);

            playButton.setOnClickListener(onButtonClick);
            prevButton.setOnClickListener(onButtonClick);
            nextButton.setOnClickListener(onButtonClick);

        }

        am = (AudioManager) getSystemService(AUDIO_SERVICE);
        loop.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (player != null)
                    player.setLooping(isChecked);
            }
        });

        int app_volume;

        AudioManager audioManager2 = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        app_volume = audioManager2.getStreamVolume(AudioManager.STREAM_MUSIC);

        if (app_volume == 0) {
            mute.setVisibility(View.INVISIBLE);
            unmute.setVisibility(View.VISIBLE);
        } else {
            mute.setVisibility(View.VISIBLE);
            unmute.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {

            mute.setVisibility(View.VISIBLE);
            unmute.setVisibility(View.INVISIBLE);
            return true;

        }


        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {

            int app_volume;
            AudioManager audioManager2 = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            app_volume = audioManager2.getStreamVolume(AudioManager.STREAM_MUSIC);

            if (app_volume == 0) {

                mute.setVisibility(View.INVISIBLE);
                unmute.setVisibility(View.VISIBLE);

            } else {

                mute.setVisibility(View.VISIBLE);
                unmute.setVisibility(View.INVISIBLE);

            }

        }
        return true;
    }

    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        super.onListItemClick(list, view, position, id);
        currentFile = (String) view.getTag();
        startPlay(currentFile);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacks(updatePositionRunnable);
        player.stop();
        player.reset();
        player.release();

        player = null;
    }

    private void startPlay(String file) {
        Log.i("Selected: ", file);

        selectedFile.setText(file);

        player.stop();
        player.reset();

        try {
            player.setDataSource(file);
            player.prepare();
            player.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        seekbar.setMax(player.getDuration());
        playButton.setImageResource(android.R.drawable.ic_media_pause);

        updatePosition();
        isStarted = true;
    }

    private void stopPlay() {
        player.stop();
        player.reset();
        playButton.setImageResource(android.R.drawable.ic_media_play);
        handler.removeCallbacks(updatePositionRunnable);

        isStarted = false;
    }

    private void updatePosition() {
        handler.removeCallbacks(updatePositionRunnable);


        handler.postDelayed(updatePositionRunnable, UPDATE_FREQUENCY);
    }

    private class MediaCursorAdapter extends SimpleCursorAdapter {
        public MediaCursorAdapter(Context context, int layout, Cursor cursor) {
            super(context, layout, cursor,
                    new String[]{MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.TITLE, MediaStore.Audio.AudioColumns.DURATION},
                    new int[]{R.id.displayname, R.id.title, R.id.duration});
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView title = (TextView) view.findViewById(R.id.title);
            TextView name = (TextView) view.findViewById(R.id.displayname);
            TextView duration = (TextView) view.findViewById(R.id.duration);


            name.setText(cursor.getString(
                    cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)));
            title.setText(cursor.getString(
                    cursor.getColumnIndex(MediaStore.MediaColumns.TITLE)));
            long durationInMs = Long.parseLong(cursor.getString(
                    cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION)));
            double durationInMin = ((double) durationInMs / 1000.0) / 60.0;

            durationInMin = new BigDecimal(Double.toString(durationInMin)).setScale(2, BigDecimal.ROUND_UP).doubleValue();

            duration.setText("" + durationInMin);

            view.setTag(cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA)));

        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.listitem, parent, false);

            bindView(v, context, cursor);

            return v;
        }
    }

    private View.OnClickListener onButtonClick = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.play: {
                    if (player.isPlaying()) {
                        handler.removeCallbacks(updatePositionRunnable);
                        player.pause();
                        playButton.setImageResource(android.R.drawable.ic_media_play);
                    } else {
                        if (isStarted) {
                            player.start();
                            playButton.setImageResource(android.R.drawable.ic_media_pause);
                        } else {
                            startPlay(currentFile);
                        }
                    }
                    break;
                }
                case R.id.next: {
                    int seekto = player.getCurrentPosition() + STEP_VALUE;

                    if (seekto > player.getDuration())
                        seekto = player.getDuration();

                    player.pause();
                    player.seekTo(seekto);
                    player.start();

                    break;
                }
                case R.id.prev: {
                    int seekto = player.getCurrentPosition() - STEP_VALUE;

                    if (seekto < 0)
                        seekto = 0;

                    player.pause();
                    player.seekTo(seekto);
                    player.start();

                    break;
                }
                case R.id.mute: {
                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                    mute.setVisibility(View.INVISIBLE);
                    unmute.setVisibility(View.VISIBLE);
                }
                case R.id.unmute: {
                    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 90, 90);
                    mute.setVisibility(View.VISIBLE);
                    unmute.setVisibility(View.INVISIBLE);
                }
            }
        }
    };

    private MediaPlayer.OnCompletionListener onCompletion = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            stopPlay();
        }
    };

    private MediaPlayer.OnErrorListener onError = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChanged = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isMoveingSeekBar = false;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isMoveingSeekBar = true;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (isMoveingSeekBar) {
                player.seekTo(progress);

                Log.i("OnSeekBarChangeListener", "onProgressChanged");
            }
        }
    };

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }
}










