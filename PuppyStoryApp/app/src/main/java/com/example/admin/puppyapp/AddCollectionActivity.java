package com.example.admin.puppyapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddCollectionActivity extends AppCompatActivity implements SensorEventListener{
    private int GALLERY = 1;
    private Uri mImageUri;
    private String userId,uploadId;

    private ImageButton mButtonChooseImage;
    private Button mButtonCreate;
    private EditText mBreed, mName, mDescription;
    private TextView breed,name,description,profile;
    private ImageView imageView;
    private boolean isDay;

    private DatabaseReference collectionRef, mDataRef;
    private FirebaseUser user;
    private LinearLayout addLayout;
    private StorageReference imageReference, fileRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_collection);

        mName = findViewById(R.id.edit_text_name);
        mBreed = findViewById(R.id.edit_text_breed);
        mDescription = findViewById(R.id.edit_text_description);
        name = findViewById(R.id.text_view_name);
        breed = findViewById(R.id.text_view_breed);
        description = findViewById(R.id.text_view_description);
        profile = findViewById(R.id.text_view_profile);

        mButtonChooseImage = findViewById(R.id.button_choose_image);
        mButtonCreate = findViewById(R.id.button_create);
        imageView = findViewById(R.id.image_view);
        addLayout= findViewById(R.id.add_layout);

        //light sensor register
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener((SensorEventListener) this,lightSensor,sensorManager.SENSOR_DELAY_FASTEST);

        // get user ID
        user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user.getUid();
        Log.e("id", userId);

        collectionRef = FirebaseDatabase.getInstance().getReference().child("DogCollection").child(userId);
        fileRef = null;

        mButtonChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePhotoFromGallary();
            }
        });

        mButtonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = mName.getText().toString();
                String breed = mBreed.getText().toString();
                String description = mDescription.getText().toString();

                if(!name.isEmpty() && !breed.isEmpty() && !description.isEmpty()) {
                    uploadId = collectionRef.push().getKey();
                    mDataRef = FirebaseDatabase.getInstance().getReference("DogCollection/" +
                            userId + "/" );
                    imageReference = FirebaseStorage.getInstance().getReference().child("DogCollection/" +
                            userId + "/" );
                    uploadFile();

                    Map<String, Object> infoUpdates = new HashMap<>();
                    infoUpdates.put(uploadId + "/name", name);
                    infoUpdates.put(uploadId + "/breed", breed);
                    infoUpdates.put(uploadId + "/description", description);

                    collectionRef.updateChildren(infoUpdates);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "Name, Breed, and Description cannot be null!", Toast
                    .LENGTH_SHORT).show();
                }
            }
        });


    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()== Sensor.TYPE_LIGHT){
            int grayShade = (int)event.values[0];
            if (grayShade > 20000){
                isDay=true;
                addLayout.setBackgroundResource(R.drawable.light_dog3);
                mName.setHintTextColor(Color.BLACK);
                mBreed.setHintTextColor(Color.BLACK);
                mDescription.setHintTextColor(Color.BLACK);
                name.setTextColor(Color.BLACK);
                breed.setTextColor(Color.BLACK);
                description.setTextColor(Color.BLACK);
                profile.setTextColor(Color.BLACK);

            }else if (grayShade <=20000 && isDay==true){
                isDay=false;
                addLayout.setBackgroundResource(R.drawable.dog3);
                mName.setHintTextColor(Color.WHITE);
                mBreed.setHintTextColor(Color.WHITE);
                mDescription.setHintTextColor(Color.WHITE);
                name.setTextColor(Color.WHITE);
                breed.setTextColor(Color.WHITE);
                description.setTextColor(Color.WHITE);
                profile.setTextColor(Color.WHITE);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                mImageUri = data.getData();
                System.out.println("mImageUri: " +mImageUri);
                Picasso.get().load(mImageUri).into(imageView);
            } else if (data == null){
            }

        }
    }

    private void uploadFile() {
        if (mImageUri != null) {
            File f = new File(mImageUri.getPath());
            final String imageName = f.getName();

            fileRef = imageReference.child(imageName);
            fileRef.putFile(mImageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {

                            fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String imageUpload = uri.toString();
                                    Map<String, Object> imageUpdates = new HashMap<>();
                                    imageUpdates.put(uploadId + "/url/", imageUpload);

                                    mDataRef.updateChildren(imageUpdates);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                }
                            });

                            Toast.makeText(AddCollectionActivity.this, "Submit Successful ", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {

                            Toast.makeText(AddCollectionActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            // progress percentage
                        }
                    })
                    .addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                            System.out.println("Upload is paused!");
                        }
                    });
        } else {
            mImageUri = Uri.parse("android.resource://com.example.admin.puppyapp/" + R.drawable.dogprofile);
            uploadFile();
        }
    }



}
