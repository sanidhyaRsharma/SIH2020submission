package com.example.potholegame;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Collection;
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

                            }
                        }
                    }
                }
            }
        });
    }
    private Vector3 screenCenter(){
        View v = findViewById(android.R.id.content);
        return new Vector3(v.getMeasuredWidth()/2f, v.getMeasuredHeight()/2f, 0f);
    }
}
