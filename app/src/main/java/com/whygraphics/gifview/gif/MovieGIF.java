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

    // the stop time of the gif
    private long mGifStopTime;

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
                MovieGIF.this.run();
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
                    invokeListenerOnFrameReady();
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
            throw new IllegalArgumentException("mDelayInMillis must be positive: " + delayInMillis);

        this.mDelayInMillis = delayInMillis;
    }

    /**
     * Returns the gif duration in seconds.
     *
     * @return the gif duration in seconds
     */
    @Override
    public double getDuration() {
        final double MILLIS_IN_ONE_SECOND = 1000.0;

        return mMovie.duration() / MILLIS_IN_ONE_SECOND;
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

    /**
     * Returns the current second in the gif.
     *
     * @return the current second in the gif
     */
    @Override
    public double getCurrentSecond() {
        final double MILLIS_IN_ONE_SECOND = 1000.0;

        int movieDuration = mMovie.duration();

        if (isShowing())
            return timeInGif() / MILLIS_IN_ONE_SECOND;
        else if (mGifStopTime == 0)
            return (mGifStartTime % movieDuration) / MILLIS_IN_ONE_SECOND;
        else
            return ((mGifStopTime - mGifStartTime) % movieDuration) / MILLIS_IN_ONE_SECOND;
    }

    /**
     * Sets the time in the gif.
     *
     * @param seconds the time in the gif
     */
    @Override
    public void setTime(double seconds) {
        if (seconds < 0 || seconds > getDuration())
            throw new IllegalArgumentException("seconds must be in the range of the gif: 0-"
                    + getDuration() + ": " + seconds);

        final long MILLIS_IN_ONE_SECOND = 1000;

        long millisTimeInGif = (long) (seconds * MILLIS_IN_ONE_SECOND);

        // to start in x it's like we stopped just now and started in now-x
        mGifStartTime = SystemClock.uptimeMillis() - millisTimeInGif;
        mGifStopTime = SystemClock.uptimeMillis();
    }

    /**
     * Starts the gif.
     * If the gif is running does nothing.
     */
    @Override
    public void start() {
        if (mExecutor != null)
            return;

        // start gif time is based on the last stopped time, if exists
        mGifStartTime = SystemClock.uptimeMillis() - (mGifStopTime - mGifStartTime);

        mExecutor = Executors.newSingleThreadScheduledExecutor();

        final int INITIAL_DELAY = 0;
        mExecutor.scheduleWithFixedDelay(mMainRunnable, INITIAL_DELAY,
                mDelayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Restarts the gif.
     */
    @Override
    public void restart() {
        mGifStartTime = SystemClock.uptimeMillis();
        mGifStopTime = SystemClock.uptimeMillis();

        if (!isShowing())
            start();
    }

    /**
     * Stops the gif.
     * If the gif is not running does nothing.
     */
    @Override
    public void stop() {
        if (mExecutor == null)
            return;

        // set the stopped time
        mGifStopTime = SystemClock.uptimeMillis();

        mExecutor.shutdown();

        // waits until the thread is finished
        while (true) {
            try {
                mExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                break;
            } catch (InterruptedException ignored) {
            }
        }

        mExecutor = null;
    }

    // returns the millis time in the gif.
    private int timeInGif() {
        // avoids returning negative time in rare cases with this local variable
        long gifStartTime = mGifStartTime;

        return (int) ((SystemClock.uptimeMillis() - gifStartTime) % mMovie.duration());
    }

    // the main run method
    private void run() {
        draw();
        invokeListener();
    }

    // calculates the frame and draws it to mBitmap through mCanvas
    private void draw() {
        mMovie.setTime(timeInGif());
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
            invokeListenerOnFrameReady();
    }

    // invokes the method onFrameReady() of the listener
    private void invokeListenerOnFrameReady() {
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
