package com.whygraphics.gifview.gif;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.whygraphics.gifview.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

/**
 * A view that can show gifs.
 * <p>
 * The gif starts showing automatically unless starting_on_init or setStartingOnInit()
 * were set to false or a call to startGifWhenPossible() occurred while the view is in
 * the middle of setting the gif.
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
 * when its initialization finishes or not.
 * <p>
 * delay_in_millis:
 * A positive integer that represents how many milliseconds
 * should pass between every calculation of the next frame to be set.
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

    private boolean mRegularlyStartOnInit;

    private boolean mStartOnNextInit;

    // the delay in millis between frames
    private int mDelayInMillis;

    // the listener for callbacks to invoke when setting a gif
    private OnSettingGifListener mOnSettingGifListener;

    // keeps track if the view is in the middle of setting the gif
    private boolean mSettingGif;

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
     * Note: Calling startGifWhenPossible() or stopGifWhenPossible() while setting a gif
     * ignores this value.
     *
     * @return true if the view starts the gif when its initialization finishes, false otherwise.
     */
    public boolean isStartingOnInit() {
        return mRegularlyStartOnInit;
    }

    /**
     * Sets the value for starting the gif when its initialization finishes.
     * <p>
     * If called while setting a gif and after calling startGifWhenPossible()
     * or stopGifWhenPossible(), cancels the changes of these methods.
     *
     * @param startingOnInit true if the gif will start as soon as it is ready, false otherwise
     */
    public void setStartingOnInit(boolean startingOnInit) {
        this.mStartOnNextInit = startingOnInit;
        this.mRegularlyStartOnInit = startingOnInit;
    }

    /**
     * Sets the delay in millis between every calculation of the next frame to be set.
     *
     * @param delayInMillis the delay in millis
     * @throws IllegalArgumentException if delayInMillis is non-positive
     */
    public void setDelayInMillis(int delayInMillis) {
        if (delayInMillis <= 0)
            throw new IllegalArgumentException("delayInMillis must be positive");

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
     * If a request has been made through startGifWhenPossible()
     * or stopGifWhenPossible() while the view is setting the gif,
     * the view will ignore the value returned from isStartingOnInit().
     * The view then will start the gif if startGifWhenPossible() was called or
     * will not start the gif if stopGifWhenPossible() was called.
     * <p>
     * Note that every exception while setting the gif is only sent to the
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
     *
     * @param string the string
     * @throws IllegalArgumentException if the string format is invalid
     */
    public void setGifResource(String string) {
        if (mSettingGif)
            return;

        // stops the gif if it is running
        if (mGif != null)
            mGif.stopGif();

        if (!isGifResourceFormatValid(string))
            throw new IllegalArgumentException("string format is invalid");

        // notifies setting gif has started
        mSettingGif = true;

        setGifFromString(string);
    }

    /**
     * Returns true if the format is valid, false otherwise.
     * <p>
     * Override this method to provide more ways to initialize a gif.
     * For it to work setGifFromString() must be overridden too.
     * <p>
     * If setting a gif from a url or from the assets directory is still supported
     * the overridden method should consider the results of the super method.
     * <p>
     * Example:
     * <pre>
     * return super.isGifResourceFormatValid(string) ||
     *         string.startsWith("other prefix:");</pre>
     *
     * @param string the string passed to setGifResource()
     * @return true if the format is valid, false otherwise
     */
    protected boolean isGifResourceFormatValid(String string) {
        return string.startsWith(RESOURCE_PREFIX_URL) ||
                string.startsWith(RESOURCE_PREFIX_ASSET);
    }

    /**
     * Sets the gif according to the specified format.
     * <p>
     * Override this method to provide more ways to initialize a gif.
     * For it to work isGifResourceFormatValid() must be overridden too
     * and a AsyncSettingOfGif should be initialized and executed.
     * <p>
     * If setting a gif from a url or from the assets directory is still supported
     * the overridden method should consider the super method.
     * <p>
     * Example:
     * <pre>
     * if (string.startsWith("other prefix:"))
     *     new AsyncSettingOfGif(){}.execute(string);
     * else
     *     super.setGifFromString(string);</pre>
     *
     * @param string the string passed to setGifResource()
     */
    protected void setGifFromString(String string) {
        if (string.startsWith(RESOURCE_PREFIX_URL))
            new AsyncSettingOfGif() {
                @Override
                protected String formatString(String string) {
                    final int URL_START_INDEX = RESOURCE_PREFIX_URL.length();
                    return string.substring(URL_START_INDEX);
                }

                @Override
                protected InputStream getGifInputStream(String url) throws Exception {
                    // gets the input stream from the url
                    return (InputStream) new URL(url).getContent();
                }
            }.execute(string);

        else if (string.startsWith(RESOURCE_PREFIX_ASSET))
            new AsyncSettingOfGif() {
                @Override
                protected String formatString(String string) {
                    final int ASSET_START_INDEX = RESOURCE_PREFIX_ASSET.length();
                    final String GIF_EXTENSION = ".gif";

                    String assetPath = string.substring(ASSET_START_INDEX)
                            .replaceAll("[\\\\/]", File.separator); // replacing file separators
                    if (!assetPath.endsWith(GIF_EXTENSION))
                        assetPath += GIF_EXTENSION;

                    return assetPath;
                }

                @Override
                protected InputStream getGifInputStream(String assetPath) throws Exception {
                    // gets the input stream from the assets directory
                    return GIFView.this.getResources().getAssets().open(assetPath);
                }
            }.execute(string);
    }

    // called when the view finished setting the gif
    private void onFinishSettingGif(Exception e) {
        // notifies setting the gif has finished
        mSettingGif = false;

        if (mGif != null)
            onSuccess();
        else
            onFailure(e);

        // after onFinishSettingGif() sets this one-time boolean to the value of mRegularlyStartOnInit
        mStartOnNextInit = mRegularlyStartOnInit;
    }

    // on finish setting the gif
    private void onSuccess() {
        MovieGIF movieGIF = ((MovieGIF) mGif);

        movieGIF.setOnFrameReadyListener(mGifOnFrameReadyListener, getHandler());
        movieGIF.setDelayInMillis(mDelayInMillis);

        if (mStartOnNextInit)
            startGif();

        if (mOnSettingGifListener != null)
            mOnSettingGifListener.onSuccess(this);
    }

    // when an exception has occurred while trying to set the gif
    private void onFailure(Exception e) {
        if (mOnSettingGifListener != null)
            mOnSettingGifListener.onFailure(this, e);
    }

    /**
     * Starts the gif.
     * If the gif is running does nothing.
     * <p>
     * This method throws an exception if the gif has not been initialized or isSettingGif() is true
     * and thus serves as an alternative to startGifWhenPossible().
     *
     * @throws IllegalStateException if the gif has not been initialized yet
     */
    public void startGif() {
        if (mGif == null || mSettingGif)
            throw new IllegalStateException("the gif has not been initialized yet");

        mGif.startGif();
    }

    /**
     * Starts the gif when possible.
     * If the gif is running does nothing.
     * If isSettingGif() is true the gif will start when it finishes.
     * <p>
     * Note: it does not change the result of isStartingOnInit().
     */
    public void startGifWhenPossible() {
        if (mSettingGif)
            mStartOnNextInit = true;
        else if (mGif != null)
            mGif.startGif();
    }

    /**
     * Stops the gif.
     * If the gif is not running does nothing.
     * <p>
     * This method throws an exception if the gif has not been initialized or isSettingGif() is true
     * and thus serves as an alternative to stopGifWhenPossible().
     *
     * @throws IllegalStateException if the gif has not been initialized yet
     */
    public void stopGif() {
        if (mGif == null || mSettingGif)
            throw new IllegalStateException("the gif has not been initialized yet");

        mGif.stopGif();
    }

    /**
     * Stops the gif when possible.
     * If the gif is not running does nothing.
     * If isSettingGif() is true the gif does not start when it finishes.
     * <p>
     * Note: it does not change the result of isStartingOnInit().
     */
    public void stopGifWhenPossible() {
        if (mSettingGif)
            mStartOnNextInit = false;
        else if (isShowingGif())
            mGif.stopGif();
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
     * Interface definition for callbacks to be invoked when setting a gif.
     */
    public interface OnSettingGifListener {

        /**
         * Called when a gif has successfully been set.
         *
         * @param view the GIFView
         */
        void onSuccess(GIFView view);

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
    public static class CannotInitGifException extends Exception {

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

    /**
     * A sub-class of AsyncTask to easily perform an async task of setting a gif.
     * <p>
     * The default implementation of AsyncSettingOfGif.doInBackground() is to try and initialize the gif
     * from the input stream returned from AsyncSettingOfGif.getGifInputStream(),
     * passing to it the string from AsyncSettingOfGif.formatString(), and notify
     * the view when it finishes, sending to it the exception from
     * AsyncSettingOfGif.prepareExceptionToSend(), if occurred.
     * <p>
     * Implementations of this class should override AsyncSettingOfGif.getGifInputStream()
     * to return the right input stream for the gif based on the string argument.
     * The string argument can be, for example, a url to retrieve the input stream from.
     */
    protected abstract class AsyncSettingOfGif extends AsyncTask<String, Void, Exception> {

        /**
         * This method tries to initialize the gif based on the specified string
         * and sends to onPostExecute() any exceptions that occur.
         *
         * @param string a string that represents a gif resource
         * @return exceptions caught while executing
         */
        @Override
        protected Exception doInBackground(String... string) {
            Exception exceptionToSend = null;

            String formattedString = formatString(string[0]);

            try (InputStream in = getGifInputStream(formattedString)) {
                // tries to init mGif
                mGif = new MovieGIF(in);

            } catch (Exception e) {
                exceptionToSend = prepareExceptionToSend(e);
            }

            return exceptionToSend;
        }

        /**
         * Returns a string with which getGifInputStream() can get a gif resource.
         * <p>
         * Override this method to return the right string getGifInputStream() needs
         * in order to get the gif's resource.
         *
         * @param string a string that represents a gif resource
         * @return the formatted string for getting the resource
         */
        protected abstract String formatString(String string);

        /**
         * Returns an input stream of a gif.
         * <p>
         * Override this method to return the right input stream for the gif based on the string argument.
         * The string argument can be, for example, a url to retrieve the input stream from.
         *
         * @param string the string
         * @return an InputStream of a gif
         * @throws Exception if an exception has occurred
         */
        protected abstract InputStream getGifInputStream(String string) throws Exception;

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

            // adds the suppressed exceptions, if exist
            for (Throwable throwable : e.getSuppressed())
                returnedException.addSuppressed(throwable);

            return returnedException;
        }

        /**
         * This method returns to the view with the exception passed by doInBackground().
         *
         * @param e the Exception
         */
        @Override
        protected void onPostExecute(Exception e) {
            onFinishSettingGif(e);
        }
    }
}