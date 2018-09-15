package com.dieam.reactnativepushnotification.modules;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.Map;
import java.util.HashMap;

import static com.dieam.reactnativepushnotification.modules.RNPushNotification.LOG_TAG;

public class RNPushNotificationImageFetcher {
    private final Context context;
    private final Map<String, Bitmap> images = new HashMap<>();
    private final Map<String, Boolean> downloaded = new HashMap<>();
    private RNPushNotificationImageFetcherListener listener;

    public RNPushNotificationImageFetcher(Context context, RNPushNotificationImageFetcherListener listener) {
        this.listener = listener;
        this.context = context;
    }

    public void addImage(String uri) {
        images.put(uri, null);
        downloaded.put(uri, false);
    }

    public void fetch() {
        for (String uri : images.keySet()) {
            fetchImage(uri);
        }
    }

    private void checkFinished() {
        boolean finished = true;
        for (String uri : downloaded.keySet()) {
            finished = finished && downloaded.get(uri);
        }

        if (finished) {
            listener.onComplete(images);
        }
    }

    private void fetchImage(final String uri) {
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        ImageRequest imageRequest = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(uri))
                .setRequestPriority(Priority.HIGH)
                .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH)
                .build();
        DataSource<CloseableReference<CloseableImage>> dataSource =
                imagePipeline.fetchDecodedImage(imageRequest, context);
        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            public void onNewResultImpl(@Nullable Bitmap bitmap) {
                if (bitmap == null) {
                    Log.d(LOG_TAG, "Bitmap data source returned success, but bitmap null.");
                    return;
                }

                images.put(uri, bitmap);
                downloaded.put(uri, true);
                checkFinished();
            }

            @Override
            public void onFailureImpl(DataSource dataSource) {
                downloaded.put(uri, true);
                checkFinished();
            }
        }, CallerThreadExecutor.getInstance());

    }

    public interface RNPushNotificationImageFetcherListener {
        void onComplete(Map<String, Bitmap> images);
    }
}
