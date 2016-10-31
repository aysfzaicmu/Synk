package edu.cmu.stuco.android.synk;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gimbal.android.Beacon;
import com.gimbal.android.BeaconSighting;
import com.gimbal.android.Gimbal;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Visit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener{
    private static final int BEACON_ARRAY_SIZE = 10;
    private static final String TAG = "MainActivity";
    private Button mStart,mPausePlay;
    private TextView searching_status, available_beacons;
    private PlaceEventListener placeEventListener;
    private Beacon strongestBeacon = null;
    private ArrayList<Beacon> detectableBeacons = new ArrayList<Beacon>(BEACON_ARRAY_SIZE);
    private int excessIndex = 0;
    private HashMap<String,Integer> beaconStrength = new HashMap<String,Integer>();//string is beacon identifier
    private ArrayList<Beacon> allBeacons = new ArrayList<Beacon>();
    Uri songUri;
    boolean hasChanged;
    private Integer currSongID = -1;


    private static final int FINE_LOC_PERMISSION_REQUEST_CODE = 100;
    private static final int COARSE_LOC_PERMISSION_REQUEST_CODE = 101;
    private static final int READ_EXT_STORAGE_PERMISSION_REQUEST_CODE = 102;
    //for music
    MediaPlayer mediaPlayer = new MediaPlayer();;
    ArrayList<Song> songs = new ArrayList<Song>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        searching_status = (TextView) findViewById(R.id.searching_status);
        available_beacons = (TextView) findViewById(R.id.available_beacons);

        //check permissions

        if (ActivityCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
                String[] perms = {"android.permission.ACCESS_FINE_LOCATION"};

                ActivityCompat.requestPermissions(this, perms, FINE_LOC_PERMISSION_REQUEST_CODE);
            }
        if (ActivityCompat.checkSelfPermission(this, "android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
                String[] perms = {"android.permission.ACCESS_COARSE_LOCATION"};

                ActivityCompat.requestPermissions(this,perms, COARSE_LOC_PERMISSION_REQUEST_CODE);
            }
        if (ActivityCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            String[] perms = {"android.permission.READ_EXTERNAL_STORAGE"};
            ActivityCompat.requestPermissions(this, perms, READ_EXT_STORAGE_PERMISSION_REQUEST_CODE);
            Log.d(TAG, "permission denied for reading ext storage");
        } else {
            addAllSongs();
        }


        Gimbal.setApiKey(this.getApplication(), "e258c8ad-a462-4182-bf7a-b72965cb25ec");
        Gimbal.registerForPush("1003786478441");
        //PLACE SETUP
        placeEventListener = new PlaceEventListener() {
            @Override
            public void onVisitEnd(Visit visit) {
                super.onVisitEnd(visit);
            }

            @Override
            public void onVisitStart(Visit visit) {
                super.onVisitStart(visit);
            }

            @Override
            public void onBeaconSighting(BeaconSighting beaconSighting, List<Visit> list) {
                super.onBeaconSighting(beaconSighting, list);
                addBeacon(beaconSighting.getBeacon());
                hasChanged = updateStrongestBeacon(); //returns true if strongest beacon has changed
                if (hasChanged) helper(hasChanged);

            }
        };

        PlaceManager.getInstance().addListener(placeEventListener);
        PlaceManager.getInstance().stopMonitoring(); // stop initially. start after button is pressed


        mPausePlay = (Button) findViewById(R.id.pause_play);
        mPausePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (strongestBeacon != null) { //so it doesnt give error when pressed without any beacon in sight

                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        mPausePlay.setText("Play");
                    } else {
                        mediaPlayer.start();
                        mPausePlay.setText("Pause");
                    }
                }
            }
        });

        mStart = (Button) findViewById(R.id.sync);
        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PlaceManager.getInstance().isMonitoring()) {
                    PlaceManager.getInstance().stopMonitoring();
                    mStart.setText("Sync");
                    searching_status.setText("Stopped searching for beacons.");


                }
                else {
                    PlaceManager.getInstance().startMonitoring();
                    mStart.setText("UnSync");
                    searching_status.setText("Searching for beacons...");

                }

            }
        });

    }



    public void addBeacon(Beacon b){
        //add to detectable Beacons
        if (detectableBeacons.size() < BEACON_ARRAY_SIZE){
            detectableBeacons.add(b);
        }
        else{
            detectableBeacons.set(excessIndex,b);
            excessIndex = excessIndex + 1;
            excessIndex = excessIndex % (BEACON_ARRAY_SIZE);
        }

        //add to allBeacons
        boolean contains = false;
        for (Beacon beacon : allBeacons){
            if (beacon.getIdentifier().equals(b.getIdentifier())){
                contains = true;
            }
        }
        if (!contains) allBeacons.add(b);

    }

    public Beacon getBeaconFromIdentifier(String iden){
        for (Beacon b : allBeacons){
            if (b.getIdentifier().equals(iden)) {
                return b;
            }
        }
        return null;

    }



    public boolean updateStrongestBeacon(){
        beaconStrength.clear();
        for (Beacon b : detectableBeacons){
            if (beaconStrength.containsKey(b.getIdentifier())){
                int i = beaconStrength.get(b.getIdentifier());
                beaconStrength.remove(b.getIdentifier());
                beaconStrength.put(b.getIdentifier(), i+1);
            }
            else {
                beaconStrength.put(b.getIdentifier(), 0);
            }
        }

        //display beacon strengths in text view
        String s = "Beacons Detected:";
        s += System.lineSeparator();
        for (String bS : beaconStrength.keySet()) {
            s += getBeaconFromIdentifier(bS).getName() + " Strength : " + beaconStrength.get(bS);
            s += System.lineSeparator();
        }
        System.out.println();

        available_beacons.setText(s);

        int currMax = 0;
        String maxBeaconIden = null;
        for (String beaconIden : beaconStrength.keySet()){
            if (beaconStrength.get(beaconIden) >= currMax){
                maxBeaconIden = beaconIden;
                currMax = beaconStrength.get(beaconIden);
            }
        }
        Beacon maxBeacon = getBeaconFromIdentifier(maxBeaconIden);

        if (strongestBeacon == null){
            strongestBeacon = maxBeacon;
            return true;
        }
        else if (strongestBeacon.equals(maxBeacon)){
            return false;
        }
        else {
            strongestBeacon = maxBeacon;
            return true;

        }

    }


    public void addAllSongs() {
        //get all songs
        String artist;
        String title;
        String composer;
        String displayName;
        String data;
        Cursor songCursor = getMediaStoreSongs();


        if (songCursor.getCount() > 0) {
            songCursor.moveToFirst();
            do {
                artist = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST));
                data = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA));
                title = songCursor.getString(songCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));
                songs.add(new Song(Song.NUM_SONGS, artist, title,data));
                Song.NUM_SONGS++;

            } while (songCursor.moveToNext());

        }
        songCursor.close();




    }

    public void helper(boolean beaconChanged) {

        if (strongestBeacon != null) {

            if (beaconChanged) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                mediaPlayer = new MediaPlayer();
            }

            Beacon currentBeacon = strongestBeacon;
            int numSongs = songs.size();
            Random random = new Random();
            int rn = random.nextInt(numSongs);
            Integer songID = songs.get(rn).getId();
            while (songID == currSongID) {
                rn = random.nextInt(numSongs);
                songID = songs.get(rn).getId();

            }
            currSongID = songID;
            Song song = null;
            for (Song s : songs) {
                if (s.getId() == songID) {
                    song = s;
                } else {
                }
            }

            if (song != null) songUri = Uri.parse(song.getData());
            else System.out.println("Song is null");

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnPreparedListener(MainActivity.this);

            try {
                mediaPlayer.setDataSource(MainActivity.this, songUri);
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //update text in pause/play button
            mPausePlay = (Button) findViewById(R.id.pause_play);
            mPausePlay.setText("Pause");

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mediaPlayer.reset();
                    //get next random song to play
                    int numSongs = songs.size();
                    Random random = new Random();
                    int rn = random.nextInt(numSongs);
                    int nextSongID = songs.get(rn).getId();
                    while (currSongID == nextSongID) {
                        rn = random.nextInt(numSongs);
                        nextSongID = songs.get(rn).getId();
                    }
                    currSongID = nextSongID;

                    Song song = null;
                    for (Song s : songs) {
                        if (s.getId() == nextSongID) {
                            song = s;

                        } else {
                        }
                    }

                    if (song != null) songUri = Uri.parse(song.getData());


                    try {
                        mediaPlayer.setDataSource(MainActivity.this, songUri);
                        mediaPlayer.prepareAsync();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }



                }
            });

        }
    }

    protected Cursor getMediaStoreSongs() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String mProjection[] =
                {
                        MediaStore.Audio.AudioColumns._ID,
                        MediaStore.Audio.AudioColumns.ALBUM,
                        MediaStore.Audio.AudioColumns.ARTIST,
                        MediaStore.Audio.AudioColumns.COMPOSER,
                        MediaStore.Audio.AudioColumns.DATA,
                        MediaStore.Audio.AudioColumns.DATE_MODIFIED,
                        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
                        MediaStore.Audio.AudioColumns.DURATION,
                        MediaStore.Audio.AudioColumns.TITLE,
                        MediaStore.Audio.AudioColumns.TRACK,
                        MediaStore.Audio.AudioColumns.SIZE,
                        MediaStore.Audio.AudioColumns.YEAR
                };
        String mSelection = MediaStore.Audio.AudioColumns.IS_MUSIC + "=1"
                + " AND " + MediaStore.Audio.AudioColumns.TITLE + " != ''";

        Cursor cursor =
                getContentResolver()
                        .query(
                                uri,
                                mProjection,
                                mSelection,
                                null,
                                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
                        );
        Log.d(TAG,"cursor size in getMediaSongs is " + cursor.getCount());
        return cursor;
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        boolean status;
        String msg;
        switch(permsRequestCode){

            case FINE_LOC_PERMISSION_REQUEST_CODE:

                status = grantResults[0]==PackageManager.PERMISSION_GRANTED;
                msg = status ? "Permission granted" : "Permission denied";
                System.out.println(msg);
                break;


            case COARSE_LOC_PERMISSION_REQUEST_CODE:
                status= grantResults[0]==PackageManager.PERMISSION_GRANTED;
                msg = status ? "Permission granted" : "Permission denied";
                System.out.println(msg);
                break;

            case READ_EXT_STORAGE_PERMISSION_REQUEST_CODE:
                boolean audioAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                msg = audioAccepted ? "Permission granted" : "Permission denied";
                System.out.println(msg);
                if (audioAccepted) addAllSongs();
                break;


        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer.start();
    }
}
