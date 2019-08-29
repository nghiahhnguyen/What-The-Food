package com.google.firebase.samples.apps.mlkit.java.objectdetection;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.google.firebase.samples.apps.mlkit.common.CameraImageGraphic;
import com.google.firebase.samples.apps.mlkit.common.CustomModelActivity;
import com.google.firebase.samples.apps.mlkit.common.FrameMetadata;
import com.google.firebase.samples.apps.mlkit.common.GraphicOverlay;
import com.google.firebase.samples.apps.mlkit.java.LivePreviewActivity;
import com.google.firebase.samples.apps.mlkit.java.VisionProcessorBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A processor to run object detector.
 */
public class ObjectDetectorProcessor extends VisionProcessorBase<List<FirebaseVisionObject>> {

    private static final String TAG = "ObjectDetectorProcessor";

    private final FirebaseVisionObjectDetector detector;
    LivePreviewActivity livePreviewActivity;
    private CustomModelActivity customModelActivity;

    public ObjectDetectorProcessor(FirebaseVisionObjectDetectorOptions options, LivePreviewActivity livePreviewActivity) throws FirebaseMLException {
        detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options);
        customModelActivity = new CustomModelActivity(livePreviewActivity);
        this.livePreviewActivity = livePreviewActivity;
    }

    @Override
    public void stop() {
        super.stop();
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close object detector!", e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionObject>> detectInImage(FirebaseVisionImage image) {
        return detector.processImage(image);
    }

    @Override
    protected void onSuccess(
            @Nullable Bitmap originalCameraImage,
            @NonNull List<FirebaseVisionObject> results,
            @NonNull FrameMetadata frameMetadata,
            @NonNull final GraphicOverlay graphicOverlay) {
        List<FirebaseVisionObject> foodObjects = new ArrayList<>();
        for (FirebaseVisionObject object : results) {
            if (object.getClassificationCategory() == FirebaseVisionObject.CATEGORY_FOOD) {
                foodObjects.add(object);
            }
        }
        results = foodObjects;
        graphicOverlay.clear();
        graphicOverlay.setContext(livePreviewActivity);
        graphicOverlay.setObjects(results);
        graphicOverlay.initializeLabels();
//        livePreviewActivity.setObjects(results);
//        livePreviewActivity.initializeLabels();
        if (originalCameraImage != null) {
            CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
            graphicOverlay.add(imageGraphic);
        }
        Log.d(TAG, "Number of Object" + results.size());

        CompletionService completionService = new ExecutorCompletionService(Executors.newFixedThreadPool(5));
        int remainingFutures = 0;
        for (int i = 0; i < results.size(); ++i) {
            final FirebaseVisionObject object = results.get(i);
            ++remainingFutures;
            // Crop the bounding box of the detected object
            Rect croppedRectangle = object.getBoundingBox();
            assert originalCameraImage != null;
            final Bitmap croppedBitmap = Bitmap.createBitmap(originalCameraImage,
                    croppedRectangle.left, croppedRectangle.top, croppedRectangle.width(), croppedRectangle.height());

            CallableInference callableInference = new CallableInference(croppedBitmap, graphicOverlay, object, i);
            completionService.submit(callableInference);
        }
        while (remainingFutures > 0) {
            try {
                Future completedFuture = completionService.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            --remainingFutures;
        }
        graphicOverlay.postInvalidate();
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Object detection failed!", e);
    }

    class CallableInference implements Callable<Void> {
        private final Bitmap croppedBitmap;
        private final GraphicOverlay graphicOverlay;
        private final FirebaseVisionObject object;
        private final int index;

        CallableInference(Bitmap croppedBitmap, final GraphicOverlay graphicOverlay, final FirebaseVisionObject object, int index) {
            this.croppedBitmap = croppedBitmap;
            this.graphicOverlay = graphicOverlay;
            this.object = object;
            this.index = index;
        }

        @Override
        public Void call() {
            try {
                customModelActivity.runInference(croppedBitmap)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<String>>() {
                                    @Override
                                    public void onSuccess(List<String> strings) {
                                        String label = strings.get(0);
                                        if(index < graphicOverlay.getSize()) {
                                            graphicOverlay.setLabel(index, label);
                                        }
                                        ObjectGraphic objectGraphic = new ObjectGraphic(graphicOverlay, object, label);
                                        graphicOverlay.add(objectGraphic);
                                    }
                                }
                        )
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "Custom classifier failed: " + e);
                                        e.printStackTrace();
                                    }
                                }
                        );
            } catch (FirebaseMLException e) {
                Log.d(TAG, e.toString());
                e.printStackTrace();
            }
            return null;
        }
    }
}
