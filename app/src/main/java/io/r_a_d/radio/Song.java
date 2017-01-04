package io.r_a_d.radio;

/**
 * Created by Kethsar on 1/2/2017.
 */

public class Song {
    private String mArtistName;
    private String mSongTitle;
    private boolean mRequestable;
    private Integer mSongID;

    public Song() {}

    public Song(String artist, String title, Integer songID, boolean requestable){
        mSongTitle = title;
        mArtistName = artist;
        mSongID = songID;
        mRequestable = requestable;
    }

    public String getArtistName(){
        return mArtistName;
    }

    public void setArtistName(String artistName){
        mArtistName = artistName;
    }

    public String getSongTitle(){
        return mSongTitle;
    }

    public void setSongTitle(String songTitle){
        mSongTitle = songTitle;
    }

    public Integer getSongID(){
        return mSongID;
    }

    public void setSongID(Integer songID){
        mSongID = songID;
    }

    public boolean IsRequestable(){
        return mRequestable;
    }

    public void setRequestable(boolean requestable){
        mRequestable = requestable;
    }
}
