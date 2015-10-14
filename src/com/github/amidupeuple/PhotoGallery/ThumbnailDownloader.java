package com.github.amidupeuple.PhotoGallery;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dpivovar on 04.06.2015.
 */
public class ThumbnailDownloader<Token> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;
    private LruCache<String, Bitmap> imagesCache = new LruCache<String, Bitmap>(50);

    Handler mHandler;
    Map<Token, String> requestMap = Collections.synchronizedMap(new HashMap<Token, String>());

    Handler mResponseHandler;
    Listener<Token> mListener;

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    public void setListener(Listener<Token> mListener) {
        this.mListener = mListener;
    }

    public void queueThumbnail(Token token, String url, List<GalleryItem> preloadItems) {
        Log.i(TAG, "Got an URL: " + url);
        requestMap.put(token, url);

        mHandler.obtainMessage(MESSAGE_DOWNLOAD, token).sendToTarget();
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected  void onLooperPrepared() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    @SuppressWarnings("unchecked")
                    Token token = (Token) msg.obj;
                    Log.i(TAG, "Got a request for url: " + requestMap.get(token));
                    handleRequest(token);
                }
            }
        };
    }

    private void handleRequest(final Token token) {
        byte[] bitmapBytes = null;
        Bitmap bitmap = null;

        try {
            final String url = requestMap.get(token);

            if (url == null) {
                return;
            }

            if (imagesCache.get(url) == null) {
                bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                Log.i(TAG, "Bitmap created");
                imagesCache.put(url, bitmap);
            } else {
                Log.i(TAG, "Bitmap already exist");
                bitmap = imagesCache.get(url);
            }

            mResponseHandler.post(new TriggerUpdateOfImageView(token, bitmap));
            /*mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (requestMap.get(token) != url) {
                        return;
                    }

                    requestMap.remove(token);
                    mListener.onThumbnailDownloaded(token, bitmap);
                }
            });*/
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    public interface Listener<Token> {
        void onThumbnailDownloaded(Token token, Bitmap thumbnail);
    }

    public void clearQueue() {
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();
    }

    class TriggerUpdateOfImageView implements Runnable {
        private Token token;
        private Bitmap bitmap;

        TriggerUpdateOfImageView(Token token, Bitmap bitmap) {
            this.token = token;
            this.bitmap = bitmap;
        }

        @Override
        public void run() {
            requestMap.remove(token);
            mListener.onThumbnailDownloaded(token, bitmap);
        }
    }
}
