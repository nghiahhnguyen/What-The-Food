package com.google.firebase.samples.apps.mlkit;
import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.samples.apps.mlkit.R;
import com.squareup.picasso.Picasso;
import com.synnapps.carouselview.CarouselView;
import com.synnapps.carouselview.ImageListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodInfoActivity extends AppCompatActivity {

    CarouselView carouselView;
    final String CUSTOMER_KEY = "cc0fe-996d8-09c4f-24264-ecd27-ffdc2";
    final String CUSTOMER_SECRET = "58c67-ecd9d-c4b88-aed00-839f4-aad86";
    String query;
    final String sort = "popular";
    final String base_url = "https://api.shutterstock.com/v2/images/search?query=";
    private Button button;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_info);

        Intent intent = getIntent();
        this.query = intent.getStringExtra("query");

        // set up carousel view
        carouselView = (CarouselView) findViewById(R.id.carouselView);
        carouselView.setPageCount(5);
        carouselView.setImageListener(imageListener);

        // set title
        TextView title = findViewById(R.id.title);
        title.setText(query.toUpperCase());

        // book now with foody.vn
        button = findViewById(R.id.now_vn);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                String now_url = "https://www.foody.vn/ho-chi-minh/dia-diem?q=" + query + "&ds=Restaurant";
                openWebPage(now_url);
            }
        });
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        // set food info
        final TextView info = findViewById(R.id.info);
        final String TAG = "Test on Database";
        final String food_path = query.toLowerCase().replace(' ', '_');
        db.collection("food").document(food_path)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            info.setText(document.get("info").toString());
                            info.setMovementMethod(new ScrollingMovementMethod());

                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    ImageListener imageListener = new ImageListener() {
        @Override
        public void setImageForPosition(final int position, final ImageView imageView) {
            final List<String> image_urls = new ArrayList<String>();
            RequestQueue queue = Volley.newRequestQueue(getBaseContext());
            final String url = base_url + query;
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // Do something with response
                            try {
                                JSONArray data = response.getJSONArray("data");
                                for (int i = 0; i < 5; i++) {
                                    JSONObject object = (JSONObject) data.get(i);
                                    String image_url = object.getJSONObject("assets").getJSONObject("preview").getString("url");
                                    image_urls.add(image_url);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            String tmp_url = image_urls.get(position);
//            String tmp_url = "https://upload.wikimedia.org/wikipedia/commons/6/66/An_up-close_picture_of_a_curious_male_domestic_shorthair_tabby_cat.jpg";
                            Picasso.get().load(tmp_url).into(imageView);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("Error response", error.toString());
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    String creds = String.format("%s:%s", CUSTOMER_KEY , CUSTOMER_SECRET);
                    String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", auth);
                    return headers;
                }
            };
            queue.add(jsonObjectRequest);
        }
    };

    public void openWebPage(String url) {
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    public String toDocumentId(String name) {
        name = name.toLowerCase();
        name.replace("a", "e");
        return name;
    }
}

