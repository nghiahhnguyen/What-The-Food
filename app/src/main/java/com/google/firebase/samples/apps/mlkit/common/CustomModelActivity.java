package com.google.firebase.samples.apps.mlkit.common;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CustomModelActivity extends AppCompatActivity {
    private final String TAG = "CustomModelActivity";
    private final int IMG_DIM = 64;
    private final int IMG_COLOR_CHANNEL = 3;
    private final int NUMBER_CLASSES = 101;
    Activity context;
    private ArrayList<String> labelList = new ArrayList<>();

    public CustomModelActivity(Activity livePreviewActivity) {
        configureLocalModelSource();
        configureHostedModelSource();
        context = livePreviewActivity;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("classes.txt")));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                labelList.add(mLine);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
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
        FirebaseRemoteModel cloudSource = new FirebaseRemoteModel.Builder("food-recognition")
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
//                        .setAssetFilePath("inception_v3.tflite")
                        .setAssetFilePath("model_conv_net.tflite")
                        .build();
        FirebaseModelManager.getInstance().registerLocalModel(localSource);
        // [END mlkit_local_model_source]
    }

    public FirebaseModelInterpreter createInterpreter() throws FirebaseMLException {
        // [START mlkit_create_interpreter]
        FirebaseModelOptions options = new FirebaseModelOptions.Builder()
                .setRemoteModelName("food-recognition")
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
                        .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, IMG_DIM, IMG_DIM, IMG_COLOR_CHANNEL})
                        .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, NUMBER_CLASSES})
                        .build();
        // [END mlkit_create_io_options]

        return inputOutputOptions;
    }

    private float[][][][] bitmapToInputArray(Bitmap bitmap) {
        // [START mlkit_bitmap_input]
//        Bitmap bitmap = getYourInputImage();
        bitmap = Bitmap.createScaledBitmap(bitmap, IMG_DIM, IMG_DIM, true);
//        saveBitmapToStorage(bitmap);
        int batchNum = 0;
        float[][][][] input = new float[1][IMG_DIM][IMG_DIM][IMG_COLOR_CHANNEL];
        for (int x = 0; x < IMG_DIM; x++) {
            for (int y = 0; y < IMG_DIM; y++) {
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

    public Task<List<String>> runInference(Bitmap bitmap) throws FirebaseMLException {

        final FirebaseModelInterpreter firebaseInterpreter = createInterpreter();
        float[][][][] input = bitmapToInputArray(bitmap);
        FirebaseModelInputOutputOptions inputOutputOptions = createInputOutputOptions();
        // [START mlkit_run_inference]
        FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                .add(input)  // add() as many input arrays as your model requires
                .build();
        return firebaseInterpreter.run(inputs, inputOutputOptions)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                                Log.e(TAG, e.toString());
                                e.printStackTrace();
                            }
                        })
                .continueWith(
                        new Continuation<FirebaseModelOutputs, List<String>>() {
                            @Override
                            public List<String> then(Task<FirebaseModelOutputs> task) throws Exception {
                                float[][] output = task.getResult().getOutput(0);
                                float[] probabilities = output[0];
//                                Log.d(TAG, Arrays.toString(probabilities));
                                int indexHighestProbability = getIndexWithHighestProbability(probabilities);
                                String label = labelList.get(indexHighestProbability);
                                Log.d(TAG, label + " " + probabilities[indexHighestProbability]);
                                List<String> result = new ArrayList<>();
                                result.add(label);
                                return result;
                            }
                        });

//                        .addOnSuccessListener(
//                        new OnSuccessListener<FirebaseModelOutputs>() {
//                            @Override
//                            public void onSuccess(FirebaseModelOutputs result) {
//                                float[][] output = result.getOutput(0);
//                                float[] probabilities = output[0];
////                                Log.d(TAG, Arrays.toString(probabilities));
//                                int indexHighestProbability = getIndexWithHighestProbability(probabilities);
//                                String label = labelList.get(indexHighestProbability);
//                                Log.d(TAG, label + " " + probabilities[indexHighestProbability]);
//                            }
//                        })
//        // [END mlkit_run_inference]
//        wait(1000000);
//        Log.d(TAG, ret[0]);
//        return ret;
    }

    private int getIndexWithHighestProbability(float[] probabilitiesArray) {
        float highestValue = probabilitiesArray[0];
        int index = 0;
        for (int i = 1; i < NUMBER_CLASSES; ++i) {
            if (probabilitiesArray[i] > highestValue) {
                highestValue = probabilitiesArray[i];
                index = i;
            }
        }
        return index;
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