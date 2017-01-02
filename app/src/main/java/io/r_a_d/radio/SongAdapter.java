package io.r_a_d.radio;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Kethsar on 1/2/2017.
 */

public class SongAdapter extends ArrayAdapter<Song> {
    private Context mContext;
    private ArrayList<Song> mSongs;

    public SongAdapter(Context context, int resource, ArrayList<Song> songs) {
        super(context, resource, songs);
        mContext = context;
        mSongs = songs;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if(view == null){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            view = inflater.inflate(R.layout.request_cell, null);
        }

        Song song = mSongs.get(position);
        TextView titleView = (TextView) view.findViewById(R.id.songTitle);
        TextView artistView = (TextView) view.findViewById(R.id.artistName);
        Button requestButton = (Button) view.findViewById(R.id.requestButton);

        titleView.setText(song.getSongTitle());
        artistView.setText(song.getArtistName());
        requestButton.setEnabled(song.IsRequestable());
        requestButton.setHint(song.getSongID().toString());

        return view;
    }
}
