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
 * Takes care of downloading and caching of GIF files
 *
 * @author seifert
 * @version 1.0
 * @since 1.0
 */
public class GIFCache {
    private static final String HASH_MD5 = "MD5";
    private static final int BUFFER_SIZE = 65536;

    private final String url;
    private final File cachedGIF;

    public GIFCache(Context context, String url) {
        this.url = url;

        try {
            cachedGIF = new File(context.getCacheDir() + File.pathSeparator + bytesToHex(computeHash(url)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public InputStream getInputStream() throws IOException {
        if(!cachedGIF.exists()) {
            downloadGIF();
        }
        return openCachedGIFInputStream();
    }

    private void downloadGIF() throws IOException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream((InputStream) new URL(url).getContent());
            bos = new BufferedOutputStream(new FileOutputStream(cachedGIF));

            byte[] bytes = new byte[BUFFER_SIZE];
            int bytesRead;

            do {
                bytesRead = bis.read(bytes);
                bos.write(bytes, 0, bytesRead);
            } while (bytesRead > 0);

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

    private InputStream openCachedGIFInputStream() throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(cachedGIF));
    }

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
