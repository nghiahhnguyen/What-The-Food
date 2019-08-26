package com.google.firebase.samples.apps.mlkit.common;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

public class CustomModelActivity extends AppCompatActivity {
    private final String TAG = "CustomModelActivity";
    public CustomModelActivity(){
        configureLocalModelSource();
        configureHostedModelSource();
    }

    private void configureHostedModelSource() {
        // [START mlkit_cloud_model_source]
        FirebaseModelDownloadConditions.Builder conditionsBuilder =
                new FirebaseModelDownloadConditions.Builder().requireWifi();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Enable advanced conditions on Android Nougat and newer.
            conditionsBuilder = conditionsBuilder
                    .requireCharging()
                    .requireDeviceIdle();
        }
        FirebaseModelDownloadConditions conditions = conditionsBuilder.build();

        // Build a remote model source object by specifying the name you assigned the model
        // when you uploaded it in the Firebase console.
        FirebaseRemoteModel cloudSource = new FirebaseRemoteModel.Builder("my_cloud_model")
                .enableModelUpdates(true)
                .setInitialDownloadConditions(conditions)
                .setUpdatesDownloadConditions(conditions)
                .build();
        FirebaseModelManager.getInstance().registerRemoteModel(cloudSource);
        // [END mlkit_cloud_model_source]
    }

    private void configureLocalModelSource() {
        // [START mlkit_local_model_source]
        FirebaseLocalModel localSource =
                new FirebaseLocalModel.Builder("my_local_model")  // Assign a name to this model
                        .setAssetFilePath("inception_v3.tflite")
                        .build();
        FirebaseModelManager.getInstance().registerLocalModel(localSource);
        // [END mlkit_local_model_source]
    }

    public FirebaseModelInterpreter createInterpreter() throws FirebaseMLException {
        // [START mlkit_create_interpreter]
        FirebaseModelOptions options = new FirebaseModelOptions.Builder()
                .setRemoteModelName("my_cloud_model")
                .setLocalModelName("my_local_model")
                .build();
        FirebaseModelInterpreter firebaseInterpreter =
                FirebaseModelInterpreter.getInstance(options);
        // [END mlkit_create_interpreter]

        return firebaseInterpreter;
    }

    private FirebaseModelInputOutputOptions createInputOutputOptions() throws FirebaseMLException {
        // [START mlkit_create_io_options]
        FirebaseModelInputOutputOptions inputOutputOptions =
                new FirebaseModelInputOutputOptions.Builder()
                        .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 229, 229, 3})
                        .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 51})
                        .build();
        // [END mlkit_create_io_options]

        return inputOutputOptions;
    }

    private float[][][][] bitmapToInputArray(Bitmap bitmap) {
        // [START mlkit_bitmap_input]
//        Bitmap bitmap = getYourInputImage();
        bitmap = Bitmap.createScaledBitmap(bitmap, 229, 229, true);
//        saveBitmapToStorage(bitmap);
        int batchNum = 0;
        float[][][][] input = new float[1][229][229][3];
        for (int x = 0; x < 229; x++) {
            for (int y = 0; y < 229; y++) {
                int pixel = bitmap.getPixel(x, y);
                // Normalize channel values to [-1.0, 1.0]. This requirement varies by
                // model. For example, some models might require values to be normalized
                // to the range [0.0, 1.0] instead.
//                input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 128.0f;
//                input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 128.0f;
//                input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 128.0f;

                input[batchNum][x][y][0] = Color.red(pixel);
                input[batchNum][x][y][1] = Color.green(pixel);
                input[batchNum][x][y][2] = Color.blue(pixel);
            }
        }
        // [END mlkit_bitmap_input]

        return input;
    }

    public void runInference(Bitmap bitmap) throws FirebaseMLException {
        FirebaseModelInterpreter firebaseInterpreter = createInterpreter();
        float[][][][] input = bitmapToInputArray(bitmap);
        FirebaseModelInputOutputOptions inputOutputOptions = createInputOutputOptions();
        // [START mlkit_run_inference]
        FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                .add(input)  // add() as many input arrays as your model requires
                .build();
        firebaseInterpreter.run(inputs, inputOutputOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseModelOutputs>() {
                            @Override
                            public void onSuccess(FirebaseModelOutputs result) {
                                // [START_EXCLUDE]
                                // [START mlkit_read_result]
                                float[][] output = result.getOutput(0);
                                float[] probabilities = output[0];
                                Log.d(TAG, Arrays.toString(probabilities));
                                // [END mlkit_read_result]
                                // [END_EXCLUDE]
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                                Log.e(TAG, e.toString());
                                e.printStackTrace();
                            }
                        });
        // [END mlkit_run_inference]
    }

    public void useInferenceResult(float[] probabilities) throws IOException {
        // [START mlkit_use_inference_result]
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open("retrained_labels.txt")));
        for (int i = 0; i < probabilities.length; i++) {
            String label = reader.readLine();
            Log.i("MLKit", String.format("%s: %1.4f", label, probabilities[i]));
        }
        // [END mlkit_use_inference_result]
    }

    private Bitmap getYourInputImage() {
        // This method is just for show
        return Bitmap.createBitmap(0, 0, Bitmap.Config.ALPHA_8);
    }

    private void saveBitmapToStorage(Bitmap bitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/req_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        Log.i(TAG, "" + file);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}