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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.lang.Object;

public class Main extends AppCompatActivity {

    //create some constant attribute of audio data
    private static final int SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int TRACK_CHANNELS = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFERSIZE = AudioRecord.getMinBufferSize(SAMPLERATE, RECORDER_CHANNELS, AUDIO_ENCODING);

    //create some class wide objects waiting for use
    private AudioRecord recorder = null;
    private AudioTrack track = null;
    private boolean isRecording = false;
    private Thread recordingThread = null;
    private ArrayList<byte[]> tempFile = new ArrayList<byte[]>();
    private int fileIndex = 1;


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

    /**
     * Menu item click listener
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item){

        switch(item.getItemId()){
            case R.id.menu_test_item1:
                playAudio();
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

    /**
     * A Controller to control whether to play Audio or not
     */
    public void playAudio(){

        //check if the user is still recording
        if(!isRecording){

            //create a instance of audio track to record
            track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLERATE, TRACK_CHANNELS, AUDIO_ENCODING, BUFFERSIZE, AudioTrack.MODE_STREAM);
            track.setPlaybackRate(SAMPLERATE);

            //check if the track is not correctly created
            if(track != null){

                try{
                    track.play();

                    //push the audio data to the audio track
                    int index = 0;
                    while(index < tempFile.size()){
                        byte[] audioPieceByte = tempFile.get(index);
                        track.write(audioPieceByte, 0 , BUFFERSIZE);
                        index += 1;
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
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLERATE, RECORDER_CHANNELS, AUDIO_ENCODING, BUFFERSIZE);
        recorder.startRecording();


        //start a new thread to write audio data
        isRecording = true;

        recordingVisibility();
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToBuffer();
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
     * Write the recording audio data to temporary buffer
     */
    private void writeAudioDataToBuffer(){
        while (isRecording) {
            // gets the voice output from microphone to byte format
            byte[] audioPieceByte = new byte[BUFFERSIZE];
            recorder.read(audioPieceByte, 0, BUFFERSIZE);
            tempFile.add(audioPieceByte);

            //test
            System.out.println("Short wirting to file" + audioPieceByte.toString());

        }

    }

    /**
     * Write the audio data to file
     */
    private void writeAudioDataToFile() {
        //TODO comment

        //set up the file name
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        String fileName = String.valueOf(fileIndex) + ". Record Audio" + currentDateTimeString;

        // Create a new File
        File filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File newFile = new File(filePath,fileName );

        try {
            FileOutputStream os = new FileOutputStream(newFile);

            while (!tempFile.isEmpty()) {
                // gets the voice output from microphone to byte format
                byte[] audioPieceByte = tempFile.remove(0);


                try {
                    // stores the voice buffer to file
                    os.write(audioPieceByte, 0, BUFFERSIZE);

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
