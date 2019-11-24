package io.r_a_d.radio;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Implements the layout where a list of songs can be requested is displayed
 *
 * Created by Kethsar on 1/2/2017.
 */

public class SongAdapter extends ArrayAdapter<Song> {
    private Context mContext;
    private ArrayList<Song> mSongs;
    private ActivityMain mActivity;

    public SongAdapter(Context context, int resource, ArrayList<Song> songs) {
        super(context, resource, songs);
        mContext = context;
        mSongs = songs;
        mActivity = (ActivityMain)context;
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

        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                mActivity.makeRequest(Integer.parseInt(b.getHint().toString()));
            }
        });

        return view;
    }
}
