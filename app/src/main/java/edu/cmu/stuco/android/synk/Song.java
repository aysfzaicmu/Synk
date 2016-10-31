package edu.cmu.stuco.android.synk;

/**
 * Created by aliyousafzai on 4/21/16.
 */
public class Song {
    public static int NUM_SONGS = 0;
    private int id;
    private String artist;
    private String title;
    private String data;

    public Song(int id, String artist, String title, String data){
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.data = data;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }


}
