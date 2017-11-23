package com.whygraphics.gifview.gif;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The GIFCache class takes care of downloading and caching of GIF files.
 *
 */
class GIFCache {
    private static final String HASH_MD5 = "MD5";
    private static final int BUFFER_SIZE = 65536;

    private final String url;
    private final File cachedGIF;

    /**
     * <p>Creates a new GIFCache object for GIF images defined by the given {@code url}.</p>
     *
     * @param context The {@link Context} is required to access the app's cache directory
     * @param url The URL that points to the GIF image.
     */
    GIFCache(Context context, String url) {
        this.url = url;

        try {
            cachedGIF = new File(context.getCacheDir() + File.separator + bytesToHex(computeHash(url)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * <p>Returns the InputStream to the cached GIF image file.</p>
     * <p>If the GIF is not cached, it will be downloded automatically. The InputStream returned
     * is always the InputStream to the cached file.</p>
     *
     * @return The InputStream to the cached file.
     *
     * @throws IOException Thrown if an I/O error occurs.
     */
    InputStream getInputStream() throws IOException {
        if(!cachedGIF.exists()) {
            downloadGIF();
        }
        return openCachedGIFInputStream();
    }

    /**
     * <p>Download the GIF image into the cache.</p>
     *
     * @throws IOException Thrown in case of an I/O Error.
     */
    private void downloadGIF() throws IOException {
        InputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = (InputStream) new URL(url).getContent();
            bos = new BufferedOutputStream(new FileOutputStream(cachedGIF));

            byte[] bytes = new byte[BUFFER_SIZE];
            int bytesRead;
            boolean hasBytes;
            do {
                bytesRead = bis.read(bytes);
                hasBytes = bytesRead != -1;
                if(hasBytes) {
                    bos.write(bytes, 0, bytesRead);
                }
            } while (hasBytes);

            bos.flush();
        } finally {
            if(bis != null) {
                bis.close();
            }
            if(bos != null) {
                bos.close();
            }
        }
    }

    /**
     * <p>Returns a freshly opened InputStream for the cached file.</p>
     *
     * @return A freshly opened {@link InputStream} for the cached GIF file.
     * @throws FileNotFoundException Thrown in case the GIF file cannot be found.
     */
    private InputStream openCachedGIFInputStream() throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(cachedGIF));
    }

    /**
     * <p>Compute an MD5 hash for the given {@code text}.</p>
     *
     * <p>Please note that the two exceptions thrown by this method are theoretically possible,
     * but very unlikely in most cases.</p>
     *
     * @param text The text to calculate the hash from.
     * @return The MD5 hash of the given {@code text} as a hexadecimal {@link String}.
     *
     * @throws NoSuchAlgorithmException Thrown in case the MF5 algorithm cannot be found
     * @throws UnsupportedEncodingException Thrown if the encoding of {@code text} is not
     * supported.
     */
    private static byte[] computeHash(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance(HASH_MD5);
        md.update(text.getBytes("UTF-8"), 0, text.length());
        return md.digest();
    }

    /**
     * Convert an array of arbitrary bytes into a String of hexadecimal number-pairs with each pair representing on byte
     * of the array.
     *
     * @param bytes the array to convert into hexadecimal string
     * @return the String containing the hexadecimal representation of the array
     */
    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length << 1];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            int baseIndex = i << 1;
            hexChars[baseIndex] = hexArray[value >>> 4];
            hexChars[baseIndex + 1] = hexArray[value & 0x0F];
        }
        return new String(hexChars);
    }
}
