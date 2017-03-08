package edu.fandm.ztang.microphone;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Main extends AppCompatActivity {

    private static final int SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int TRACK_CHANNELS = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private static int BytesPerElement = 2; // 2 bytes in 16bit format
    private AudioRecord recorder = null;
    private AudioTrack track = null;
    private boolean isRecording = false;
    private Thread recordingThread = null;




    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //request audio input permission
        writeToExternal();

    }

    /**
     * A controller to control whether to record or not
     * @param v
     */
    public void recordAudio(View v){

        getAudioPermission();

        if (!isRecording){
            startRecording();
        }else{
            stopRecording();
        }

    }

    public void playAudio(View v){
        if(!isRecording){
            if(track != null){
                Log.d("Progress: ", "it should play now");
                track.play();
            }
        }
    }

    /**
     * A method to start recordinbg
     */
    private void startRecording(){

        //create a instance of recorder and start recording
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLERATE, RECORDER_CHANNELS,
                AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();

        //create a instance of audio track to record
        int myBufferSizeTrack = AudioTrack.getMinBufferSize(SAMPLERATE, TRACK_CHANNELS, AUDIO_ENCODING);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLERATE, TRACK_CHANNELS, AUDIO_ENCODING, myBufferSizeTrack, AudioTrack.MODE_STREAM);
        track.setPlaybackRate(SAMPLERATE);

        if(track.getState() == AudioTrack.STATE_INITIALIZED){
            Log.d("Error: ", "AudioTrack Uninitialized");
        }

        //start a new thread to write audio data
        isRecording = true;

        recordingVisibility();
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();


    }

    /**
     * A method used to stop recording
     */
    private void stopRecording(){
        if(recorder != null){
            //reset isRecording to false
            isRecording = false;

            recordingVisibility();
            //stop and reset recorder and recorder thread
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    /**
     * Write the audio data to file
     */
    private void writeAudioDataToFile() {
        // Write the output audio in byte

        File filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File newFile = new File(filePath, "Test");
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os;
        try {
            os = new FileOutputStream(newFile);

            while (isRecording) {
                // gets the voice output from microphone to byte format

                recorder.read(sData, 0, BufferElements2Rec);

                try {
                    // // writes the data to file from buffer
                    // // stores the voice buffer

                    byte bData[] = short2byte(sData);
                    System.out.println("Short wirting to file" + bData.toString());
                    track.write(bData, 0, BufferElements2Rec * BytesPerElement);
                    os.write(bData, 0, BufferElements2Rec * BytesPerElement);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            Log.d("audiotrack is : ", track.toString());
        } catch (FileNotFoundException e) {
            Log.d("Error: " , " NO os was created.");
        }


    }




    //Helper functions

    /**
     * A method to convert short to byte array
     * @param sData
     * @return a byte array
     */
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    /**
     * Set the recording indicator's visibility
     */
    private void recordingVisibility(){
        TextView recording = (TextView)findViewById(R.id.recording);
        if(isRecording){
            recording.setVisibility(View.VISIBLE);
        }else{
            recording.setVisibility(View.INVISIBLE);
        }

    }

    /**
     * Get the permission of writing to external storage
     */
    public void writeToExternal(){

        String[] perms = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, perms, 1);
    }

    /**
     * Get the permission of get audio input
     */
    public void getAudioPermission(){

        String[] perms = new String[]{Manifest.permission.RECORD_AUDIO};
        ActivityCompat.requestPermissions(this, perms, 1);
    }

}
