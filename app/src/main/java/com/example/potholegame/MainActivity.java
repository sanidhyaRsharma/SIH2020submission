package com.example.potholegame;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";
    private ModelRenderable chestRenderable;
    private ArFragment fragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        ModelRenderable.builder()
                .setSource(this, R.raw.model)
                .build()
                .thenAccept(renderable -> chestRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Log.e(TAG, "Unable to load Renderable.", throwable);
                            return null;
                        }
                );
        fragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Frame frame = fragment.getArSceneView().getArFrame();
            if (frame != null){
                Iterator<Plane> i = frame.getUpdatedTrackables(Plane.class).iterator();
                while(i.hasNext()){
                    Plane plane = i.next();
                    if(plane.getTrackingState() == TrackingState.TRACKING){
                        fragment.getPlaneDiscoveryController().hide();
                        Iterator<Anchor> iterableAnchor = frame.getUpdatedAnchors().iterator();
                        if(!iterableAnchor.hasNext()){
                            List<HitResult> hitTest= frame.hitTest(screenCenter().x, screenCenter().y);

                            Iterator<HitResult> hitTestIterator = hitTest.iterator();
                            while(hitTestIterator.hasNext()){
                                HitResult hitResult = hitTestIterator.next();

                                Anchor modelAnchor = plane.createAnchor(hitResult.getHitPose());
                                AnchorNode anchorNode = new AnchorNode(modelAnchor);
                                anchorNode.setParent(fragment.getArSceneView().getScene());

                                TransformableNode transformableNode = new TransformableNode(fragment.getTransformationSystem());
                                transformableNode.setParent(anchorNode);
                                transformableNode.setRenderable(MainActivity.this.chestRenderable);
                                transformableNode.setWorldPosition(new Vector3(modelAnchor.getPose().tx(), (modelAnchor.getPose()).compose(Pose.makeTranslation(0f, 0.05f, 0f)).ty(), modelAnchor.getPose().tz()));
                                transformableNode.setOnTapListener(new Node.OnTapListener() {
                                    @Override
                                    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
                                        Node nodeToRemove = hitTestResult.getNode();
                                        anchorNode.removeChild(nodeToRemove);
                                        Log.d("BITMAP","before try");
//
//                                        try{
//
//                                            Image image = fragment.getArSceneView().getArFrame().acquireCameraImage();
//                                            Log.d("IMAGE","before bitmap" + image.toString());
//
////                                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
////                                            byte[] bytes = new byte[buffer.capacity()];
////                                            buffer.get(bytes);
////                                            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
//
//                                            if(image != null)
//                                            {
//                                            int width = image.getWidth();
//                                            int height = image.getHeight();
//                                            final Image.Plane[] planes = image.getPlanes();
//                                            final ByteBuffer buffer = planes[0].getBuffer();
//                                            int pixelStride = planes[0].getPixelStride();
//                                            int rowStride = planes[0].getRowStride();
//                                            int rowPadding = rowStride - pixelStride * width;
//                                            Bitmap bitmapImage = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
//                                            bitmapImage.copyPixelsFromBuffer(buffer);
//                                            bitmapImage = Bitmap.createBitmap(bitmapImage, 0, 0, width, height);
//                                            image.close();
//
//
//                                            Log.d("IMAGE","after bitmap" + bitmapImage.toString());
//                                            HashMap<String,String> params =  new HashMap<>();
//                                            params.put("user_id","jayant@gmail.com");
//                                            params.put("image", imagetoString(bitmapImage));
//                                            JSONObject parameter =  new JSONObject(params);
//                                            String url = "http://192.168.43.26:5000/api/sendImage";
//                                            Log.d("IMAGE","before");
//                                            JsonObjectRequest jsonObject = new JsonObjectRequest(Request.Method.POST, url, parameter, new Response.Listener<JSONObject>() {
//                                                @Override
//                                                public void onResponse(JSONObject response) {
//                                                    Log.d("response", response.toString());
//                                                    try {
//                                                        Log.d("IMAGE","inside try");
//                                                        String result = response.getJSONObject("result").get("message").toString();
//
//                                                    } catch (JSONException e) {
//                                                        e.printStackTrace();
//                                                    }
//
//                                                }
//                                            }, new Response.ErrorListener() {
//                                                @Override
//                                                public void onErrorResponse(VolleyError error) {
//                                                    Log.d("IMAGE","error listener");
//
//                                                }
//                                            });
//
//
//                                            RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
//                                            queue.add(jsonObject);
//                                        }
//                                        }
//                                        catch (NotYetAvailableException e){
//                                            e.printStackTrace();
//                                        }
                                        try{
                                            Image image = fragment.getArSceneView().getArFrame().acquireCameraImage();
                                            Log.d("BITMAP","after image "+ image.getWidth()+" "+image.getHeight());

                                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                                            byte[] bytes = new byte[buffer.remaining()];
                                            buffer.get(bytes);
                                            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                                            ByteArrayOutputStream bao = new ByteArrayOutputStream();
                                            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 90, bao);
                                            byte [] ba = bao.toByteArray();
                                            String ba1 = Base64.encodeToString(ba, Base64.DEFAULT);
                                            try{
                                                RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                                                String URL = "http://192.168.43.26:5000/api/sendImage";
                                                JSONObject jsonObject = new JSONObject();
                                                jsonObject.put("Image", ba1);
                                                final String requestBody = jsonObject.toString();

                                                StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                                                    @Override
                                                    public void onResponse(String response) {
                                                        Log.i("VOLLEY", response);
                                                    }
                                                }, new Response.ErrorListener() {
                                                    @Override
                                                    public void onErrorResponse(VolleyError error) {
                                                        Log.e("VOLLEY", error.toString());
                                                    }
                                                }) {
                                                    @Override
                                                    public String getBodyContentType() {
                                                        return "application/json; charset=utf-8";
                                                    }

                                                    @Override
                                                    public byte[] getBody() throws AuthFailureError {
                                                        try {
                                                            return requestBody == null ? null : requestBody.getBytes("utf-8");
                                                        } catch (UnsupportedEncodingException uee) {
                                                            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                                                            return null;
                                                        }
                                                    }

                                                    @Override
                                                    protected Response<String> parseNetworkResponse(NetworkResponse response) {
                                                        String responseString = "";
                                                        if (response != null) {
                                                            responseString = String.valueOf(response.statusCode);
                                                            // can get more details such as response.headers
                                                        }
                                                        return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                                                    }
                                                };

                                            }
                                            catch (JSONException e){
                                                e.printStackTrace();
                                            }
//                                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                                            byte[] bytes = new byte[buffer.capacity()];
//                                            buffer.get(bytes);
//                                            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
//                                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                                            byte[] bytes = new byte[buffer.remaining()];
//                                            buffer.get(bytes);
//                                            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes,0,bytes.length,null);
//                                            Log.d("BITMAP","after bitmap "+bitmapImage.getWidth() + " " + bitmapImage.getHeight());
//                                            String imageString = imagetoString(bitmapImage);
//                                            Log.d("BITMAP","after bitmap "+imageString);
//                                            try{
//                                                RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
//                                                String URL = "http://192.168.43.26:5000/api/sendImage";
//                                                JSONObject jsonBody = new JSONObject();
//                                                jsonBody.put("image",imageString);
//                                                final String requestBody = jsonBody.toString();
//                                                StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
//                                                    @Override
//                                                    public void onResponse(String response) {
//                                                        Log.i("VOLLEY", response);
//                                                    }
//                                                }, new Response.ErrorListener() {
//                                                    @Override
//                                                    public void onErrorResponse(VolleyError error) {
//                                                        Log.e("VOLLEY", error.toString());
//                                                    }
//                                                }) {
//                                                    @Override
//                                                    public String getBodyContentType() {
//                                                        return "application/json; charset=utf-8";
//                                                    }
//
//                                                    @Override
//                                                    public byte[] getBody() throws AuthFailureError {
//                                                        try {
//                                                            return requestBody == null ? null : requestBody.getBytes("utf-8");
//                                                        } catch (UnsupportedEncodingException uee) {
//                                                            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
//                                                            return null;
//                                                        }
//                                                    }
//
//                                                    @Override
//                                                    protected Response<String> parseNetworkResponse(NetworkResponse response) {
//                                                        String responseString = "";
//                                                        if (response != null) {
//                                                            responseString = String.valueOf(response.statusCode);
//                                                            // can get more details such as response.headers
//                                                        }
//                                                        return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
//                                                    }
//                                                };
//
//                                                queue.add(stringRequest);
//
//                                            }
//                                            catch (JSONException e){
//                                                e.printStackTrace();
//                                            }
                                        }
                                        catch (NotYetAvailableException e){
                                            Log.d("BITMAP", "Exception");
                                        }
                                    }
                                });

                            }
                        }
                    }
                }

            }
        });
    }

    private String imagetoString(Bitmap bitmapImage) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // In case you want to compress your image, here it's at 40%
        bitmapImage.compress(Bitmap.CompressFormat.JPEG, 40, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Vector3 screenCenter(){
        View v = findViewById(android.R.id.content);
        return new Vector3(v.getMeasuredWidth()/2f, v.getMeasuredHeight()/2f, 0f);
    }
}
