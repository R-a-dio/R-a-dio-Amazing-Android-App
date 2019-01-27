package io.r_a_d.radio;

import java.net.URLDecoder;

/**
 * Global player state for the app
 *
 * Used for synchronizing the state between the Activity and the Service
 *
 * Created by resttime on 1/3/2017.
 *
 * 2018/04/02, Kethsar: Properly use getters and setters rather than public variables
 *                      Split NowPlaying into artist and title
 */

public final class PlayerState {
    private static boolean m_playing = false;
    private static boolean m_serivceStarted = false;
    private static String m_artist = "";
    private static String m_title = "";

    public static boolean isPlaying() { return m_playing; }
    public static void setPlayingStatus(boolean status) { m_playing = status; }
    public static boolean isServiceStarted() { return m_serivceStarted; }
    public static void setServiceStatus(boolean status) { m_serivceStarted = status; }
    public static String getArtist() { return m_artist; }
    public static String getTitle() { return m_title; }

    public static void setNowPlaying(String nowPlaying) {
        int hyphenPos = nowPlaying.indexOf(" - ");

        if (hyphenPos == -1) {
            m_title = nowPlaying;
            m_artist = "";
        } else {
            try {
                m_title = URLDecoder.decode(nowPlaying.substring(hyphenPos + 3), "UTF-8");
                m_artist = URLDecoder.decode(nowPlaying.substring(0, hyphenPos), "UTF-8");
            } catch (Exception e) {
            }
        }
    }
}
