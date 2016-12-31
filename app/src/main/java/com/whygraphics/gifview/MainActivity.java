package com.whygraphics.gifview;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

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

        mGifView.setOnSettingGifListener(new GIFView.OnSettingGifListener() {
            @Override
            public void onSuccess(GIFView view, Exception e) {
                Log.d("stam", "onSuccess()");

                if (e != null) {
                    e.printStackTrace();
                    Log.d("stam", e.toString());
                }
            }

            @Override
            public void onFailure(GIFView view, Exception e) {
                e.printStackTrace();
                Log.d("stam", e.toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // when going to foreground start the gif when possible
        mGifView.startWhenPossible();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // when going to background stop the gif when possible
        mGifView.stopWhenPossible();
    }
}