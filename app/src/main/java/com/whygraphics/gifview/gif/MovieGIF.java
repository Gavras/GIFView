package com.whygraphics.gifview.gif;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Handler;
import android.os.SystemClock;

import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class that represents a gif instance.
 * <p>
 * This class is based on the Movie class.
 * All the computations are compute in different thread.
 * <p>
 * Makes it possible to get the current bitmap of the gif from
 * a listener. Also if passed to setOnFrameReadyListener() a handler,
 * the bitmap is sent to the passed handler.
 */
public class MovieGIF
        implements GIF {

    private static final Bitmap.Config DEF_VAL_CONFIG = Bitmap.Config.RGB_565;

    private static final int DEF_VAL_DELAY_IN_MILLIS = 33;

    // the gif's frames are stored in a movie instance
    private Movie mMovie;

    // the canvas of this gif
    private Canvas mCanvas;

    // the bitmap of this gif
    private Bitmap mBitmap;

    // the start time of the gif
    private long mGifStartTime;

    // the executor of the gif's thread
    private ScheduledExecutorService mExecutor;

    // the main runnable of the gif
    private Runnable mMainRunnable;

    // the delay in millis between frames
    private int mDelayInMillis;

    // the listener for callbacks to invoke when the frame has changed
    private OnFrameReadyListener mOnFrameReadyListener;

    // the handler to post the callbacks of the listener
    private Handler mListenerHandler;

    private Runnable mListenerRunnable;

    /**
     * Creates Gif instance based on the passed InputStream.
     *
     * @param in the InputStream
     * @throws InputStreamIsNull                        if in is null
     * @throws InputStreamIsEmptyOrUnavailableException if in is empty or unavailable
     */
    public MovieGIF(InputStream in) {
        this(in, DEF_VAL_CONFIG);
    }

    /**
     * Creates Gif instance based on the passed InputStream and the config.
     *
     * @param in     the InputStream
     * @param config the Config
     * @throws NullPointerException                     if config is null
     * @throws InputStreamIsNull                        if in is null
     * @throws InputStreamIsEmptyOrUnavailableException if in is empty or unavailable
     */
    public MovieGIF(InputStream in, Bitmap.Config config) {
        if (in == null)
            throw new InputStreamIsNull("the input stream is null");

        this.mMovie = Movie.decodeStream(in);

        if (mMovie == null)
            throw new InputStreamIsEmptyOrUnavailableException("the input steam is empty or unavailable");

        this.mBitmap = Bitmap.createBitmap(mMovie.width(), mMovie.height(), config);

        // associates mCanvas with mBitmap
        this.mCanvas = new Canvas(mBitmap);

        this.mMainRunnable = new Runnable() {
            @Override
            public void run() {
                draw();
                invokeListener();
            }
        };

        setDelayInMillis(DEF_VAL_DELAY_IN_MILLIS);
    }

    /**
     * Register a callback to be invoked when the gif changed a frame.
     * Invokes methods from a special thread.
     *
     * @param onFrameReadyListener the listener to attach
     */
    public void setOnFrameReadyListener(OnFrameReadyListener onFrameReadyListener) {
        setOnFrameReadyListener(onFrameReadyListener, null);
    }

    /**
     * Register a callback to be invoked when the gif changed a frame.
     * Invokes methods from the specified handler.
     *
     * @param onFrameReadyListener the listener to attach
     * @param handler              the handler
     */
    public void setOnFrameReadyListener(OnFrameReadyListener onFrameReadyListener, Handler handler) {
        this.mOnFrameReadyListener = onFrameReadyListener;
        mListenerHandler = handler;

        if (mListenerHandler != null)
            mListenerRunnable = new Runnable() {
                @Override
                public void run() {
                    MovieGIF.this.mOnFrameReadyListener.onFrameReady(mBitmap);
                }
            };

        else
            mListenerRunnable = null;
    }

    /**
     * Sets the delay in millis between every calculation of the next frame to be set.
     *
     * @param delayInMillis the delay in millis
     * @throws IllegalArgumentException if mDelayInMillis is non-positive
     */
    public void setDelayInMillis(int delayInMillis) {
        if (delayInMillis <= 0)
            throw new IllegalArgumentException("mDelayInMillis must be positive");

        this.mDelayInMillis = delayInMillis;
    }

    /**
     * Starts the gif.
     * If the gif is running does nothing.
     */
    @Override
    public void startGif() {
        if (mExecutor != null)
            return;

        mExecutor = Executors.newSingleThreadScheduledExecutor();

        final int INITIAL_DELAY = 0;
        mExecutor.scheduleWithFixedDelay(mMainRunnable, INITIAL_DELAY,
                mDelayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the gif.
     * If the gif is not running does nothing.
     */
    @Override
    public void stopGif() {
        if (mExecutor == null)
            return;

        mExecutor.shutdown();

        // waits until the thread is finished
        while (true) {
            try {
                mExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
                break;
            } catch (InterruptedException ignored) {
            }
        }

        mExecutor = null;
    }

    /**
     * Returns true if the gif is currently showing, false otherwise.
     *
     * @return true if the gif is currently showing, false otherwise
     */
    @Override
    public boolean isShowing() {
        return mExecutor != null;
    }

    // calculates the frame and draws it to mBitmap through mCanvas
    private void draw() {
        // if mGifStartTime == 0 inits it for the first time
        if (mGifStartTime == 0)
            mGifStartTime = SystemClock.uptimeMillis();

        long timeElapsed = SystemClock.uptimeMillis() - mGifStartTime;

        int timeInGif = (int) (timeElapsed % mMovie.duration());
        mMovie.setTime(timeInGif);

        mMovie.draw(mCanvas, 0, 0);
    }

    // invokes the listener
    private void invokeListener() {
        if (mOnFrameReadyListener == null)
            return;

        // if handler was given invokes from it, otherwise invokes from this thread
        if (mListenerHandler != null)
            mListenerHandler.post(mListenerRunnable);
        else
            mOnFrameReadyListener.onFrameReady(mBitmap);
    }

    /**
     * Interface definition for a callback to be invoked when the gif changed a frame.
     */
    public interface OnFrameReadyListener {
        /**
         * Called when the gif changed a frame.
         * <p>
         * Note: If a handler was given with the listener this method
         * invokes from the handler, otherwise this method
         * invokes from a special thread.
         * <p>
         * Note: This bitmap is mutable and used by the gif instance
         * thus it is not recommended to mutate it.
         *
         * @param bitmap the new bitmap of the gif
         */
        void onFrameReady(Bitmap bitmap);
    }

    /**
     * Definition of a runtime exception class to throw when the inputStream is null.
     */
    public static class InputStreamIsNull extends NullPointerException {

        /**
         * Creates a new instance.
         */
        public InputStreamIsNull() {
            super();
        }

        /**
         * * Creates a new instance with a message.
         *
         * @param message the message
         */
        public InputStreamIsNull(String message) {
            super(message);
        }
    }

    /**
     * Definition of a runtime exception class to throw when the inputStream is empty or unavailable.
     */
    public static class InputStreamIsEmptyOrUnavailableException extends RuntimeException {

        /**
         * Creates a new instance.
         */
        public InputStreamIsEmptyOrUnavailableException() {
            super();
        }

        /**
         * * Creates a new instance with a message.
         *
         * @param message the message
         */
        public InputStreamIsEmptyOrUnavailableException(String message) {
            super(message);
        }
    }
}
