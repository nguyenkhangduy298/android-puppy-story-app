package com.example.admin.puppyapp;

import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

public class ShowImageActivity extends AppCompatActivity implements SensorEventListener {
    private ImageView uploadedImage;
    private String imageID, userId, collectionID, imageUrl;
    private ImageButton deleteImage;
    private ImageButton downloadImage;

    private float Accel, PrevAccel,shakeValue = 0;
    private ArrayList<String> myKeyList = new ArrayList<>();
    private int i;

    private DatabaseReference dogImageRef,dogRef;
    private static final String TAG = "ShowImageActivity";
    private FirebaseStorage storage;

    private String DOWNLOAD_DIR = Environment.getExternalStoragePublicDirectory
            (Environment.DIRECTORY_DOWNLOADS).getPath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_show_image);
        uploadedImage = findViewById(R.id.uploaded_image);
        deleteImage = findViewById(R.id.delete_dog_image);
        downloadImage = findViewById(R.id.download_dog_image);

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this,accSensor,SensorManager.SENSOR_DELAY_FASTEST);

        imageID = getIntent().getExtras().get("dogImageId").toString();
        userId  = getIntent().getExtras().get("userID").toString();
        collectionID = getIntent().getExtras().get("collectionID").toString();
        dogImageRef = FirebaseDatabase.getInstance().getReference().child("DogCollection")
                .child(userId).child(collectionID).child("images");
        dogRef = FirebaseDatabase.getInstance().getReference().child("DogCollection")
                .child(userId).child(collectionID);
        storage = FirebaseStorage.getInstance();

        retrieveDogImage();
        retrieveImageKey();

        deleteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDialog();
            }
        });

        downloadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getImageUrl();
            }
        });

    }

    private void getImageUrl(){
        dogImageRef.child(imageID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.hasChild("url")){
                    imageUrl = dataSnapshot.child("url").getValue().toString();
                    getDownloadImage(imageUrl);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getDownloadImage(String imageUrl) {

        StorageReference httpsReference = storage.getReferenceFromUrl(imageUrl);

        final File localFile = new File(DOWNLOAD_DIR + "/" + imageID + ".jpg");

        httpsReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d("File", "downloaded the file");
                Toast.makeText(getApplicationContext(),
                        "Downloaded the file",
                        Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e("File", "Failed to download the file");
                Toast.makeText(getApplicationContext(),
                        "Couldn't be downloaded. Open Settings and allow Storage for the app.",
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    private void retrieveDogImage() {
        dogImageRef.child(imageID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String dogImage = dataSnapshot.child("url").getValue().toString();

                    Picasso.get().load(dogImage)
                            .resize(800, 700)
                            .centerCrop()
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .into(uploadedImage);
                }
                else {

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void retrieveImageKey() {
        dogRef.child("images").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot childSnapshot: dataSnapshot.getChildren()){
                        String key = childSnapshot.getKey();
                        myKeyList.add(key);
                    }
                    // Compare imageID to change images accordingly later with the sensor
                    for (int u=0;u<myKeyList.size();u++){
                        String key = myKeyList.get(u);
                        if(key.equals(imageID)){
                            i=u;
                        }
                    }
                }
                else {

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void deleteDialog(){
        final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        deleteDialog.setTitle("Are you sure to delete the image?");
        deleteDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        deleteDialog.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteCollection();
                dialog.dismiss();
                finish();
            }
        });
        deleteDialog.show();
    }

    public void deleteCollection(){
        Query imageQuery = dogImageRef.child(imageID);

        imageQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot imageSnapshot: dataSnapshot.getChildren()) {
                    imageSnapshot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()== Sensor.TYPE_ACCELEROMETER){
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            PrevAccel = Accel;
            float result = x*x + y*y + z*z;
            Accel = (float) Math.sqrt((double)result);
            float  delta = Accel - PrevAccel;
            shakeValue = shakeValue * 0.9f +delta;
            if (shakeValue>18){
                shakeValue = 0;
                i++;
                if (i==myKeyList.size()){
                    i=0;
                }
                dogImageRef.child(myKeyList.get(i)).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            String dogImage = dataSnapshot.child("url").getValue().toString();

                            Picasso.get().load(dogImage)
                                    .resize(500, 400)
                                    .centerCrop()
                                    .networkPolicy(NetworkPolicy.OFFLINE)
                                    .into(uploadedImage);
                        }
                        else {

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
