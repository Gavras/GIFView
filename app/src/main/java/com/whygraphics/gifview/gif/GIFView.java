package com.whygraphics.gifview.gif;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.whygraphics.gifview.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A view that can show gifs.
 * <p>
 * The gif starts showing automatically unless starting_on_init or setStartingOnInit()
 * were set to false or a call to stopWhenPossible() occurred while the view was in
 * the middle of setting the gif.
 * <p>
 * After the view is no longer going to be in the
 * presenting layout calling clear() is recommended.
 * <p>
 * XML Attributes:
 * <p>
 * gif_src:
 * A string that represents the gif's source.
 * <p>
 * - If you want to get the gif from a url
 * concatenate the string "url:" with the full url.
 * <p>
 * - if you want to get the gif from the assets directory
 * concatenate the string "asset:" with the full path of the gif
 * within the assets directory. You can exclude the .gif extension.
 * <p>
 * for example if you have a gif in the path "assets/ex_dir/ex_gif.gif"
 * the string should be: "asset:ex_dir/ex_gif"
 * <p>
 * starting_on_init:
 * A boolean that represents if the view starts the gif
 * when its initialization finishes or not. Default is true.
 * <p>
 * on_click_start_or_pause:
 * If sets to true, every click toggles the state of the gif.
 * If the gif is showing stops the gif, and if the gif is not showing starts it.
 * If sets to false clicking the gif does nothing. Default is false.
 * <p>
 * delay_in_millis:
 * A positive integer that represents how many milliseconds
 * should pass between every calculation of the next frame to be set. Default is 33.
 */
public class GIFView extends ImageView {

    /**
     * The "url:" prefix for the gif_src xml and setGifResource() method.
     */
    public static final String RESOURCE_PREFIX_URL = "url:";

    /**
     * The "asset:" prefix for the gif_src xml and setGifResource() method.
     */
    public static final String RESOURCE_PREFIX_ASSET = "asset:";

    private static final int DEF_VAL_DELAY_IN_MILLIS = 33;

    // the gif instance
    private GIF mGif;

    private boolean mStartingOnInit;

    // the delay in millis between frames
    private int mDelayInMillis;

    // the listener for callbacks to invoke when setting a gif
    private OnSettingGifListener mOnSettingGifListener;

    // keeps track if the view is in the middle of setting the gif
    private boolean mSettingGif;

    private OwnOnSettingGifListener mOwnOnSettingGifListener;

    private MovieGIF.OnFrameReadyListener mGifOnFrameReadyListener;

    /**
     * Creates a new instance in the passed context.
     *
     * @param context the context
     */
    public GIFView(Context context) {
        super(context);
        init(null);
    }

    /**
     * Creates a new instance in the passed context with the specified set of attributes.
     *
     * @param context the context
     * @param attrs   the attributes
     */
    public GIFView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    // inits the view
    private void init(AttributeSet attrs) {
        this.mOwnOnSettingGifListener = new OwnOnSettingGifListener();

        this.mGifOnFrameReadyListener = new MovieGIF.OnFrameReadyListener() {
            @Override
            public void onFrameReady(Bitmap bitmap) {
                setImageBitmap(bitmap);
            }
        };

        if (attrs != null) {
            initAttrs(attrs);

        } else {
            setDelayInMillis(DEF_VAL_DELAY_IN_MILLIS);
            setStartingOnInit(true);
            setOnClickStartOrPause(false);
        }
    }

    // inits the view with the specified attributes
    private void initAttrs(AttributeSet attrs) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(
                attrs, R.styleable.gif_view,
                0, 0);
        try {

            // gets and sets the starting on init
            setStartingOnInit(typedArray.getBoolean(R.styleable.gif_view_starting_on_init
                    , true));

            // gets and sets the delay in millis
            setDelayInMillis(typedArray.getInt(R.styleable.gif_view_delay_in_millis
                    , DEF_VAL_DELAY_IN_MILLIS));

            // gets and sets the 'on click start or pause'
            setOnClickStartOrPause(typedArray.getBoolean(R.styleable.gif_view_on_click_start_or_pause
                    , false));

            // gets the source of the gif and sets it
            String string = typedArray.getString(R.styleable.gif_view_gif_src);
            if (string != null)
                setGifResource(typedArray.getString(R.styleable.gif_view_gif_src));

        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Returns true if the view starts the gif when its initialization finishes, false otherwise.
     * <p>
     * Note: Calling startWhenPossible() or stopWhenPossible() while setting a gif
     * ignores this value.
     * <p>
     * See also the xml tag "starting_on_init".
     *
     * @return true if the view starts the gif when its initialization finishes, false otherwise.
     */
    public boolean isStartingOnInit() {
        return mStartingOnInit;
    }

    /**
     * Sets the value for starting the gif when its initialization finishes.
     * <p>
     * If called while setting a gif and after calling startWhenPossible()
     * or stopWhenPossible(), cancels the changes of these methods.
     * <p>
     * See also the xml tag "starting_on_init".
     *
     * @param startingOnInit true if the gif will start as soon as it is ready, false otherwise
     */
    public void setStartingOnInit(boolean startingOnInit) {
        this.mStartingOnInit = startingOnInit;
        mOwnOnSettingGifListener.mStartOnNextInit = startingOnInit;
    }

    /**
     * Returns the delay in milliseconds between every calculation of the next frame to be set.
     * <p>
     * See also the xml tag "delay_in_millis".
     *
     * @return the delay in milliseconds between every calculation of the next frame to be set
     */
    public int getDelayInMillis() {
        return mDelayInMillis;
    }

    /**
     * Sets the delay in milliseconds between every calculation of the next frame to be set.
     * <p>
     * See also the xml tag "delay_in_millis".
     *
     * @param delayInMillis the delay in millis
     * @throws IllegalArgumentException if delayInMillis is non-positive
     */
    public void setDelayInMillis(int delayInMillis) {
        if (mGif != null && !(mGif instanceof MovieGIF))
            throw new UnsupportedOperationException("this method is unsupported");

        if (delayInMillis <= 0)
            throw new IllegalArgumentException("delayInMillis must be positive: " + delayInMillis);

        this.mDelayInMillis = delayInMillis;

        if (mGif != null)
            ((MovieGIF) mGif).setDelayInMillis(delayInMillis);
    }

    /**
     * Register callbacks to be invoked when the view finished setting a gif.
     *
     * @param onSettingGifListener the listener to attach
     */
    public void setOnSettingGifListener(OnSettingGifListener onSettingGifListener) {
        this.mOnSettingGifListener = onSettingGifListener;
    }

    /**
     * If sets to true, every click toggles the state of the gif.
     * If the gif is showing stops the gif, and if the gif is not showing starts it.
     * If sets to false clicking the gif does nothing.
     * <p>
     * Default is false.
     * <p>
     * See also the xml tag on_click_start_or_pause
     *
     * @param flag to start or pause the gif on click, or not
     */
    public void setOnClickStartOrPause(boolean flag) {
        // if true set the listener
        if (flag)
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (isShowingGif())
                        stopWhenPossible();
                    else
                        startWhenPossible();
                }
            });

            // if false nullify the listener
        else
            setOnClickListener(null);
    }

    /**
     * Returns true if the view is in the process of setting the gif, false otherwise.
     *
     * @return true if the view is in the process of setting the gif, false otherwise
     */
    public boolean isSettingGif() {
        return mSettingGif;
    }

    /**
     * Sets the gif of this view and starts it.
     * <p>
     * If the view has already begun setting another gif, does nothing.
     * You can query this state with isSettingGif().
     * <p>
     * By default the gif starts showing as soon as the view finishes setting the gif.
     * Change this by calling setStartingOnInit().
     * If a request has been made through startWhenPossible()
     * or stopWhenPossible() while the view is setting the gif,
     * the view will ignore the value returned from isStartingOnInit().
     * The view then will start the gif if startWhenPossible() was called or
     * will not start the gif if stopWhenPossible() was called.
     * <p>
     * Note: Every exception while setting the gif is only sent to the
     * OnSettingGifListener instance attached to this view.
     * <p>
     * The string passed must be in the following format:
     * <p>
     * - If you want to get the gif from a url
     * concatenate the string "url:" with the full url.
     * <p>
     * - if you want to get the gif from the assets directory
     * concatenate the string "asset:" with the full path of the gif
     * within the assets directory. You can exclude the .gif extension.
     * <p>
     * You can use the Constants:
     * <p>
     * GIFView.RESOURCE_PREFIX_URL = "url:"
     * GIFView.RESOURCE_PREFIX_ASSET = "asset:"
     * <p>
     * for example if you have a gif in the path "assets/ex_dir/ex_gif.gif"
     * invoke the method like this: setGifResource(GIFView.RESOURCE_PREFIX_ASSET + "ex_dir/ex_gif");
     * <p>
     * See also the xml tag "gif_src".
     * <p>
     * Classes that extend this class and want to provide more ways to initialize
     * a gif from a string should override GIFView.setGifResourceFromString().
     *
     * @param string the string
     * @throws IllegalArgumentException if the string is null or not in the right format
     */
    public void setGifResource(String string) {
        if (string == null)
            throw new IllegalArgumentException("string must not be null");

        if (!beforeSettingGif())
            return;

        if (string.startsWith(RESOURCE_PREFIX_URL)) {
            new AsyncSettingOfGif<String>() {

                @Override
                protected InputStream getGifInputStream(String string) throws Exception {
                    final int URL_START_INDEX = RESOURCE_PREFIX_URL.length();
                    String url = string.substring(URL_START_INDEX);

                    // gets the input stream from the url
                    return (InputStream) new URL(url).getContent();
                }

            }.execute(string);

        } else if (string.startsWith(RESOURCE_PREFIX_ASSET)) {
            new AsyncSettingOfGif<String>() {

                @Override
                protected InputStream getGifInputStream(String string) throws Exception {
                    // gets the path of the gif
                    final int ASSET_START_INDEX = RESOURCE_PREFIX_ASSET.length();
                    final String GIF_EXTENSION = ".gif";

                    String assetPath = string.substring(ASSET_START_INDEX)
                            .replaceAll("[\\\\/]", File.separator); // replacing file separators
                    if (!assetPath.endsWith(GIF_EXTENSION))
                        assetPath += GIF_EXTENSION;

                    // gets the input stream from the assets directory
                    return GIFView.this.getResources().getAssets().open(assetPath);
                }

            }.execute(string);

        } else {
            // for extending classes that want to provide more ways to init a gif
            boolean isStringFormatLegal = setGifResourceFromString(string);

            if (!isStringFormatLegal) {
                mSettingGif = false;
                throw new IllegalArgumentException("string format is invalid: " + string);
            }
        }
    }

    /**
     * * Sets the gif of this view and starts it.
     * <p>
     * If the view has already begun setting another gif, does nothing.
     * You can query this state with isSettingGif().
     * <p>
     * By default the gif starts showing as soon as the view finishes setting the gif.
     * Change this by calling setStartingOnInit().
     * If a request has been made through startWhenPossible()
     * or stopWhenPossible() while the view is setting the gif,
     * the view will ignore the value returned from isStartingOnInit().
     * The view then will start the gif if startWhenPossible() was called or
     * will not start the gif if stopWhenPossible() was called.
     * <p>
     * Note: Every exception while setting the gif is only sent to the
     * OnSettingGifListener instance attached to this view.
     * <p>
     * Note: When the view finishes setting the gif, the passed input stream is closed.
     *
     * @param in the input stream
     * @throws IllegalArgumentException if in is null
     */
    public void setGifResource(InputStream in) {
        if (in == null)
            throw new IllegalArgumentException("in must not be null");

        if (!beforeSettingGif())
            return;

        new AsyncSettingOfGif<InputStream>() {

            @Override
            protected InputStream getGifInputStream(InputStream in) {
                // the input stream for the gif is simply the passed input stream
                return in;
            }

        }.execute(in);
    }

    /**
     * Sets the gif from the specified string.
     * <p>
     * Returns true if the string is in the right format, false otherwise.
     * <p>
     * To override this method you can create an instance of
     * GIFView.AsyncSettingOfGif and execute it.
     * <p>
     * If you want to initialize a different implementation of GIF
     * you should call GIFView.onFinishSettingGif() in the end.
     * <p>
     * For example if you want to add support for raw resource directory:
     * <pre>
     * if (!string.startsWith("raw:"))
     *     return false;
     *
     * new AsyncSettingOfGif< String>() {
     *     protected InputStream getGifInputStream(String resource) throws Exception {
     *         // return the input stream
     *     }
     * }.execute(string);
     *
     * return true;
     * </pre>
     *
     * @param string the string
     * @return true if the string is in the right format, false otherwise
     */
    protected boolean setGifResourceFromString(String string) {
        /*
        * For extending classes only.
        * Normally the string is initialized from GIFView.setGifResource().
        * */
        return false;
    }

    /**
     * Prepares the view before setting the gif.
     * <p>
     * Returns true if it ok to initialize a gif, false otherwise.
     * For example when GIFView.isSettingGif() is true this method returns false.
     * <p>
     * If extending class is initializing a different implementation of GIF
     * (for instance when overriding or overloading GIFView.setGifResource())
     * it has to call this method first.
     *
     * @return true if it ok to initialize a gif, false otherwise
     */
    protected final boolean beforeSettingGif() {
        if (mSettingGif)
            return false;

        // notifies setting gif has started
        mSettingGif = true;

        return true;
    }

    /**
     * Called when the view finished initializing the gif.
     * <p>
     * If extending class is initializing a different implementation of GIF
     * (for instance when overriding or overloading GIFView.setGifResource())
     * it has to call this method.
     * <p>
     * If the gif is not null the method sets the gif
     * and call GIFView.OnSettingGifListener.onSuccess().
     * If the gif is null the method call GIFView.OnSettingGifListener.onFailure()
     * <p>
     * the exception can be GIFView.CannotInitGifException().
     *
     * @param gif the gif
     * @param e   an Exception
     */
    protected final void onFinishSettingGif(GIF gif, Exception e) {
        // stops the gif if it is running
        if (mGif != null)
            mGif.stop();

        mGif = gif;

        // notifies setting the gif has finished
        mSettingGif = false;

        if (mGif != null)
            onSuccess(e);
        else
            onFailure(e);
    }

    // on finish setting the gif
    private void onSuccess(Exception e) {
        if (mGif instanceof MovieGIF) {
            MovieGIF movieGIF = ((MovieGIF) mGif);
            movieGIF.setOnFrameReadyListener(mGifOnFrameReadyListener, getHandler());
            movieGIF.setDelayInMillis(mDelayInMillis);
            setImageBitmap(movieGIF.getThumbnail());
        }

        if (mStartingOnInit)
            start();

        mOwnOnSettingGifListener.onSuccess(this, e);

        if (mOnSettingGifListener != null)
            mOnSettingGifListener.onSuccess(this, e);
    }

    // when an exception has occurred while trying to set the gif
    private void onFailure(Exception e) {
        mOwnOnSettingGifListener.onFailure(this, e);

        if (mOnSettingGifListener != null)
            mOnSettingGifListener.onFailure(this, e);
    }

    /**
     * Returns true if a gif has been initialized, false otherwise.
     *
     * @return true if a gif has been initialized, false otherwise
     */
    public boolean isGifInitialized() {
        return mGif != null && !mSettingGif;
    }

    /**
     * Returns true if the gif is currently showing, false otherwise.
     *
     * @return true if the gif is currently showing, false otherwise
     */
    public boolean isShowingGif() {
        return mGif != null && mGif.isShowing();
    }

    /**
     * Returns the current gif duration in seconds.
     *
     * @return the current gif duration in seconds
     * @throws IllegalStateException if the gif has not been initialized yet
     */
    public double getGifDuration() {
        if (!isGifInitialized())
            throw new IllegalStateException("the gif has not been initialized yet");

        return mGif.getDuration();
    }

    /**
     * Returns the current second in the gif.
     *
     * @return the current second in the gif
     * @throws IllegalStateException if the gif has not been initialized yet
     */
    public double getCurrentSecond() {
        if (!isGifInitialized())
            throw new IllegalStateException("the gif has not been initialized yet");

        return mGif.getCurrentSecond();
    }

    /**
     * Sets the time in the gif.
     *
     * @param seconds the time in the gif
     * @throws IllegalStateException if the gif has not been initialized yet
     */
    public void setTimeInGif(double seconds) {
        if (!isGifInitialized())
            throw new IllegalStateException("the gif has not been initialized yet");

        if (seconds < 0 || seconds > getGifDuration())
            throw new IllegalArgumentException("seconds must be in the range of the gif: 0-"
                    + getGifDuration() + ": " + seconds);

        mGif.setTime(seconds);
    }

    /**
     * Returns the thumbnail bitmap.
     * <p>
     * If initializing the thumbnail has failed Returns null.
     *
     * @return the thumbnail bitmap.
     * @throws IllegalStateException if the gif has not been initialized yet
     */
    public Bitmap getThumbnail() {
        if (!isGifInitialized())
            throw new IllegalStateException("the gif has not been initialized yet");

        if (!(mGif instanceof MovieGIF))
            throw new UnsupportedOperationException("this method is unsupported");

        return ((MovieGIF) mGif).getThumbnail();
    }

    /**
     * Starts the gif.
     * If the gif is running does nothing.
     * <p>
     * This method throws an exception if the gif has not been initialized or isSettingGif() is true
     * and thus serves as an alternative to startWhenPossible().
     *
     * @throws IllegalStateException if the gif has not been initialized yet
     */
    public void start() {
        if (!isGifInitialized())
            throw new IllegalStateException("the gif has not been initialized yet");

        mGif.start();
    }

    /**
     * Starts the gif when possible.
     * If the gif is running does nothing.
     * If isSettingGif() is true the gif will start when it finishes.
     * <p>
     * Note: it does not change the result of isStartingOnInit().
     */
    public void startWhenPossible() {
        if (mSettingGif)
            mOwnOnSettingGifListener.mStartOnNextInit = true;
        else if (mGif != null)
            mGif.start();
    }

    /**
     * Restarts the gif.
     *
     * @throws IllegalStateException if the gif has not been initialized yet
     */
    public void restart() {
        if (!isGifInitialized())
            throw new IllegalStateException("the gif has not been initialized yet");

        mGif.restart();
    }

    /**
     * Stops the gif.
     * If the gif is not running does nothing.
     * <p>
     * This method throws an exception if the gif has not been initialized or isSettingGif() is true
     * and thus serves as an alternative to stopWhenPossible().
     *
     * @throws IllegalStateException if the gif has not been initialized yet
     */
    public void stop() {
        if (!isGifInitialized())
            throw new IllegalStateException("the gif has not been initialized yet");

        mGif.stop();
    }

    /**
     * Stops the gif when possible.
     * If the gif is not running does nothing.
     * If isSettingGif() is true the gif does not start when it finishes.
     * <p>
     * Note: it does not change the result of isStartingOnInit().
     */
    public void stopWhenPossible() {
        if (mSettingGif)
            mOwnOnSettingGifListener.mStartOnNextInit = false;
        else if (isShowingGif())
            mGif.stop();
    }

    /**
     * Clears the view and returns it to the default state.
     * If isSettingGif() is true the view clears after it finishes setting the gif.
     */
    public void clear() {
        if (mSettingGif)
            mOwnOnSettingGifListener.mClear = true;
        else
            clearView();
    }

    // clears the view
    private void clearView() {
        if (isShowingGif())
            mGif.stop();

        mGif = null;
        setStartingOnInit(true);
        setDelayInMillis(DEF_VAL_DELAY_IN_MILLIS);
        setOnSettingGifListener(null);
        setOnClickStartOrPause(false);
        setImageBitmap(null);
    }

    /**
     * Interface definition for callbacks to be invoked when setting a gif.
     */
    public interface OnSettingGifListener {

        /**
         * Called when a gif has successfully been set.
         * <p>
         * If one or more exceptions occurred but the gif has successfully been set
         * e will store the exceptions.
         *
         * @param view the GIFView
         */
        void onSuccess(GIFView view, Exception e);

        /**
         * Called when a gif cannot be set.
         *
         * @param view the GIFView
         * @param e    the Exception
         */
        void onFailure(GIFView view, Exception e);
    }

    /**
     * Definition of an Exception class to throw when the view cannot initialize the gif.
     */
    protected static class CannotInitGifException extends Exception {

        /**
         * Creates a new instance.
         */
        public CannotInitGifException() {
            super();
        }

        /**
         * * Creates a new instance with a message.
         *
         * @param message the message
         */
        public CannotInitGifException(String message) {
            super(message);
        }
    }

    /*
     * A class for handling situations when the view needs to do something
     * and the gif is not ready yet. Saves the procedures and do them when
     * the gif is ready.
     */
    private class OwnOnSettingGifListener implements OnSettingGifListener {

        boolean mStartOnNextInit = true;

        boolean mClear = false;

        @Override
        public void onSuccess(GIFView view, Exception e) {
            handleStartOnNextInit();
            handleClear();
        }

        @Override
        public void onFailure(GIFView view, Exception e) {
            handleClear();
        }

        // handles start on next init
        private void handleStartOnNextInit() {
            // proceed just if mStartOnNextInit is different from the current behaviour of this view
            if (mStartOnNextInit == isStartingOnInit())
                return;

            if (mStartOnNextInit)
                start();
            else
                stop();

            // returns it to the original state
            mStartOnNextInit = isStartingOnInit();
        }

        // handles clear
        private void handleClear() {
            if (mClear) {
                clearView();

                // returns it ot the original state
                mClear = false;
            }
        }
    }

    /**
     * A sub-class of AsyncTask to easily perform an async task of setting a gif.
     * <p>
     * The default implementation of AsyncSettingOfGif.doInBackground() is to try and initialize
     * the gif from the concrete class MovieGIF from the input stream
     * returned from AsyncSettingOfGif.getGifInputStream(),
     * passing to it the resource from AsyncTask.execute(), and notify
     * the view when it finishes, sending to it the initialized gif
     * and the exception from AsyncSettingOfGif.prepareExceptionToSend(), if occurred.
     * <p>
     * Implementations of this class should override AsyncSettingOfGif.getGifInputStream()
     * to return the right input stream for the gif based on the resource passed to it.
     */
    protected abstract class AsyncSettingOfGif<T> extends AsyncTask<T, Void, GIF> {

        // the exception to pass to the listener
        private Exception e;

        /**
         * This method tries to initialize a gif based on the specified resource
         * and pass it to onPostExecute() with any exceptions that occur.
         *
         * @param resource the resource of the gif
         * @return the gif
         */
        @Override
        protected final GIF doInBackground(T... resource) {
            InputStream in = null;
            try {
                // gets the input stream for the gif from method manipulateResource()
                in = getGifInputStream(resource[0]);
                // tries to init mGif
                return new MovieGIF(in);

            } catch (Exception e) {
                this.e = prepareExceptionToSend(e);

            } finally {
                if (in != null) {
                    try {
                        in.close();

                    } catch (IOException e) {

                        // if the exception to pass is null make new one
                        if (this.e == null)
                            this.e = prepareExceptionToSend(e);

                            /*
                            * If the exception already exists adds the current exception to the suppressed array.
                            * Note: Works only in API 19 and above
                            * */
                        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            this.e.addSuppressed(e);
                        }
                    }
                }

            }

            return null;
        }

        /**
         * Returns an input stream of a gif.
         * <p>
         * Override this method to return the right input stream
         * for the gif based on the specified resource.
         *
         * @param resource the resource
         * @return an InputStream of a gif
         * @throws Exception if an exception has occurred
         */
        protected abstract InputStream getGifInputStream(T resource) throws Exception;

        /**
         * Called when an exception has occurred.
         * <p>
         * Override this method to prepare an exception to send to onPostExecute()
         * or to handle it differently.
         * Can return GIFView.CannotInitGifException.
         *
         * @param e the Exception
         * @return an Exception
         */
        protected Exception prepareExceptionToSend(Exception e) {
            // prepares the message of the exception
            String message = e.getMessage();
            if (e instanceof FileNotFoundException)
                message = "file not found: " + message;

            Exception returnedException = new CannotInitGifException(message);

            /*
            * Adds the suppressed exceptions, if exist.
            * Note: Works only in API 19 and above
            * */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                for (Throwable throwable : e.getSuppressed())
                    returnedException.addSuppressed(throwable);

            return returnedException;
        }

        /**
         * This method returns to the view with any exception occurred in doInBackground().
         *
         * @param gif the gif passed from doInBackground()
         */
        @Override
        protected final void onPostExecute(GIF gif) {
            onFinishSettingGif(gif, e);
        }
    }
}
