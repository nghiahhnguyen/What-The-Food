package com.google.firebase.samples.apps.mlkit.java.objectdetection;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.google.firebase.samples.apps.mlkit.common.CameraImageGraphic;
import com.google.firebase.samples.apps.mlkit.common.CustomModelActivity;
import com.google.firebase.samples.apps.mlkit.common.FrameMetadata;
import com.google.firebase.samples.apps.mlkit.common.GraphicOverlay;
import com.google.firebase.samples.apps.mlkit.java.VisionProcessorBase;

import java.io.IOException;
import java.util.List;

/**
 * A processor to run object detector.
 */
public class ObjectDetectorProcessor extends VisionProcessorBase<List<FirebaseVisionObject>> {

    private static final String TAG = "ObjectDetectorProcessor";

    private final FirebaseVisionObjectDetector detector;

    private CustomModelActivity customModelActivity = null;

    private FirebaseModelInterpreter mInterpreter = null;

    public ObjectDetectorProcessor(FirebaseVisionObjectDetectorOptions options) throws FirebaseMLException {
        detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options);
        customModelActivity = new CustomModelActivity();
        mInterpreter = customModelActivity.createInterpreter();
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
            @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        if (originalCameraImage != null) {
            CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
            graphicOverlay.add(imageGraphic);
        }
        Log.d(TAG, "Number of Object" + results.size());
        for (FirebaseVisionObject object : results) {
            ObjectGraphic objectGraphic = new ObjectGraphic(graphicOverlay, object);
            Rect croppedRectangle = object.getBoundingBox();
            assert originalCameraImage != null;
            final Bitmap croppedBitmap = Bitmap.createBitmap(originalCameraImage,
                    croppedRectangle.left, croppedRectangle.top, croppedRectangle.width(), croppedRectangle.height());
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        customModelActivity.runInference(croppedBitmap);
                    } catch (FirebaseMLException e) {
                        e.printStackTrace();
                    }
                }
            });
            graphicOverlay.add(objectGraphic);
        }

        graphicOverlay.postInvalidate();
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Object detection failed!", e);
    }
}
