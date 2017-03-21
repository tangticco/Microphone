package edu.fandm.ztang.microphone;

import android.content.Intent;
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
import android.view.Menu;
import android.view.MenuInflater;
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
    private int myBUfferSizeRecord = AudioRecord.getMinBufferSize(SAMPLERATE, RECORDER_CHANNELS, AUDIO_ENCODING);
    private int myBufferSizeTrack = AudioTrack.getMinBufferSize(SAMPLERATE, TRACK_CHANNELS, AUDIO_ENCODING);
    private ArrayList<byte[]> tempFile = new ArrayList<byte[]>();


    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //request audio input permission and external storage permission

        getPermissions();


    }

    /**
     * Create a menu
     * @param m
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu m){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_demo, m);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item){



        switch(item.getItemId()){
            case R.id.menu_test_item1:
                break;
            case R.id.menu_test_item2:
                break;
            case R.id.menu_test_item3:
                break;

        }

        return true;
    }

    /**
     * A controller to control whether to record or not
     * @param v
     */
    public void recordAudio(View v){


        if (!isRecording){
            startRecording();
        }else{
            stopRecording();
        }

    }




    public void playAudio(View v){

        //check if the user is still recording Audio
        if(!isRecording){
            //create a instance of audio track to record
            track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLERATE, TRACK_CHANNELS, AUDIO_ENCODING, myBUfferSizeRecord, AudioTrack.MODE_STREAM);
            track.setPlaybackRate(SAMPLERATE);


            if(track != null){

                //play the audio using the tempFile and the AudioTrack
                try{
                    track.play();
                    Log.d("Progress: ", "it should play now");
                    while(!tempFile.isEmpty()){
                        byte[] audioPieceByte = tempFile.get(0);
                        track.write(audioPieceByte, 0 , myBUfferSizeRecord);
                    }

                }catch ( IllegalStateException r){
                    Log.d("Error: ", "The AudioTrack is not properly set to play");
                }

            }
        }else{
            Log.d("Error:" , "The app is still recording");
        }
    }

    /**
     * A method to start recordinbg
     */
    private void startRecording(){

        //create a instance of recorder and start recording

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLERATE, RECORDER_CHANNELS,
                AUDIO_ENCODING, myBUfferSizeRecord);

        recorder.startRecording();


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


        FileOutputStream os;
        try {
            os = new FileOutputStream(newFile);

            while (isRecording) {
                // gets the voice output from microphone to byte format
                byte[] audioPieceByte = new byte[myBUfferSizeRecord];
                recorder.read(audioPieceByte, 0,myBUfferSizeRecord);

                try {
                    // // writes the data to file from buffer
                    // // stores the voice buffer

                    System.out.println("Short wirting to file" + audioPieceByte.toString());
                    tempFile.add(audioPieceByte);
                    os.write(audioPieceByte, 0, myBUfferSizeRecord);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }



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
    public void getPermissions(){

        String[] perms = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
        ActivityCompat.requestPermissions(this, perms, 1);

    }


}
