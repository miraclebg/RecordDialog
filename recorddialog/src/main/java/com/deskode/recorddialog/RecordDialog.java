package com.deskode.recorddialog;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import omrecorder.AudioChunk;
import omrecorder.AudioRecordConfig;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.PullableSource;
import omrecorder.Recorder;

public class RecordDialog extends DialogFragment {

    private String titleStr;
    private String messageStr;
    private String positiveButtonText;
    private FloatingActionButton recordButton;
    private String buttonState = "INIT";
    private String audioSavePathInDevice;
    private TextView timerView;
    private Button sendButton;
    private Timer timer;
    private int recorderSecondsElapsed;
    private int playerSecondsElapsed;

    private ClickListener clickListener;
    private Recorder recorder;
    private MediaPlayer mediaPlayer;
//    private MediaPlayer mPlayer;

    public RecordDialog() {

    }

    public static RecordDialog newInstance(String title) {
        RecordDialog frag = new RecordDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //noinspection ConstantConditions
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams
                .SOFT_INPUT_STATE_HIDDEN);
        setupRecorder();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @NonNull
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Getting the layout inflater to inflate the view in an alert dialog.
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View rootView = inflater.inflate(R.layout.record_dialog, null);

        //titleStr = getContext().getString(R.string.record_audio_title);

        timerView = rootView.findViewById(R.id._txtTimer);
        timerView.setText(messageStr);

        sendButton = rootView.findViewById(R.id.btnSend);
        sendButton.setVisibility(View.GONE);
        sendButton.setOnClickListener(v -> {
            if (buttonState.equals("RECORD")) {
                try {
                    recorder.stopRecording();
                    stopTimer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            clickListener.OnClickListener(audioSavePathInDevice);
            dismiss();
        });

        recordButton = rootView.findViewById(R.id.btnRecord);
        recordButton.setOnClickListener(v -> {
            scaleAnimation();
            switch (buttonState) {
                case "INIT":
                    recordButton.setImageResource(R.drawable.ic_stop);
                    buttonState = "RECORD";
                    try {
//                            mPlayer = MediaPlayer.create(getContext(), R.raw.hangouts_message);
//                            mPlayer.start();
//                            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                                @Override
//                                public void onCompletion(MediaPlayer mp) {
//                                    recorder.startRecording();
//                                    startTimer();
//                                }
//                            });
                        recorder.startRecording();
                        startTimer();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    break;
                case "RECORD":
                    try {
                        recorder.stopRecording();
//                            mPlayer = MediaPlayer.create(getContext(), R.raw.pop);
//                            mPlayer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    sendButton.setVisibility(View.VISIBLE);
                    recordButton.setImageResource(R.drawable.ic_play);
                    buttonState = "STOP";
                    timerView.setText("00:00:00");
                    recorderSecondsElapsed = 0;
                    break;
                case "STOP":
                    startMediaPlayer();
                    break;
                case "PLAY":
                    pauseMediaPlayer();
                    break;
                case "PAUSE":
                    resumeMediaPlayer();
                    break;
            }
        });

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setView(rootView);

        alertDialog.setTitle(titleStr);

        recorderSecondsElapsed = 0;
        playerSecondsElapsed = 0;

        final AlertDialog dialog = alertDialog.create();
        //noinspection ConstantConditions
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams
                .SOFT_INPUT_STATE_HIDDEN);

        return dialog;
    }

    public void setMessage(String strMessage) {
        if (!Objects.equals(messageStr, strMessage)) {
            messageStr = strMessage;

            if (timerView != null) {
                timerView.setText(messageStr);
            }
        }
    }

    public void setPositiveButton(String strPositiveButtonText, ClickListener onClickListener) {
        positiveButtonText = strPositiveButtonText;
        clickListener = onClickListener;
    }

    private void setupRecorder() {
        recorder = OmRecorder.wav(
                new PullTransport.Default(mic(), audioChunk -> {
                }), file());
    }

    private PullableSource mic() {
        return new PullableSource.Default(
                new AudioRecordConfig.Default(
                        MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT,
                        AudioFormat.CHANNEL_IN_MONO, 44100
                )
        );
    }

    @NonNull
    private File file() {
        //noinspection ConstantConditions
        File dir = getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File file = new File(dir, timeStamp + ".wav");
        audioSavePathInDevice = file.getPath();
        return file;
    }

    public String getAudioPath() {
        return audioSavePathInDevice;
    }

    private void startMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioSavePathInDevice);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> stopMediaPlayer());
        } catch (IOException e) {
            e.printStackTrace();
        }
        recordButton.setImageResource(R.drawable.ic_pause);
        buttonState = "PLAY";
        playerSecondsElapsed = 0;
        startTimer();
        mediaPlayer.start();
    }

    private void resumeMediaPlayer() {
        recordButton.setImageResource(R.drawable.ic_pause);
        buttonState = "PLAY";
        mediaPlayer.start();
    }

    private void pauseMediaPlayer() {
        recordButton.setImageResource(R.drawable.ic_play);
        buttonState = "PAUSE";
        mediaPlayer.pause();
    }

    @SuppressLint("SetTextI18n")
    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
            recordButton.setImageResource(R.drawable.ic_play);
            buttonState = "STOP";
            timerView.setText("00:00:00");
            stopTimer();
        }
    }

    private void startTimer() {
        stopTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        }, 0, 1000);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    private void updateTimer() {
        // here you check the value of getActivity() and break up if needed
        if (getActivity() == null)
            return;

        getActivity().runOnUiThread(() -> {
            if (buttonState.equals("RECORD")) {
                recorderSecondsElapsed++;
                timerView.setText(Util.formatSeconds(recorderSecondsElapsed));
            } else if (buttonState.equals("PLAY")) {
                playerSecondsElapsed++;
                timerView.setText(Util.formatSeconds(playerSecondsElapsed));
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void scaleAnimation() {
        final Interpolator interpolador = AnimationUtils.loadInterpolator(getContext(),
                android.R.interpolator.fast_out_slow_in);
        recordButton.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setInterpolator(interpolador)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        recordButton.animate().scaleX(1f).scaleY(1f).start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    public interface ClickListener {
        void OnClickListener(String path);
    }
}
