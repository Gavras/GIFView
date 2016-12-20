package com.whygraphics.gifview;

import android.app.Activity;
import android.os.Bundle;

import com.whygraphics.gifview.gif.GIFView;

public class MainActivity extends Activity {

    // the GIFView instance
    private GIFView mGifView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // retrieving the GIFView
        mGifView = (GIFView) findViewById(R.id.main_activity_gif_vie);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // when going to foreground start the gif when possible
        mGifView.startGifWhenPossible();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // when going to background stop the gif when possible
        mGifView.stopGifWhenPossible();
    }
}