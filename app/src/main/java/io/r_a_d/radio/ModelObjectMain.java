package io.r_a_d.radio;

/**
 * Created by Kilim on 12/28/2016.
 */

public enum ModelObjectMain {

    NOWPLAYING(R.string.now_playing, R.layout.now_playing),
    REQUESTS(R.string.requests, R.layout.requests),
    NEWS(R.string.news, R.layout.news),
    CHAT(R.string.chat, R.layout.chat);

    private int mTitleResId;
    private int mLayoutResId;

    ModelObjectMain(int titleResId, int layoutResId) {
        mTitleResId = titleResId;
        mLayoutResId = layoutResId;
    }

    public int getTitleResId() {
        return mTitleResId;
    }

    public int getLayoutResId() {
        return mLayoutResId;
    }

}
