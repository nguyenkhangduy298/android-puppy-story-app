package com.example.admin.puppyapp;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EditCollectionActivity extends AppCompatActivity implements SensorEventListener {
    private int GALLERY = 1;
    private Uri mImageUri;
    private String userId, collectionID;

    private ImageButton mChooseEditImage;
    private Button mButtonDone;
    private EditText mEditBreed, mEditName, mEditDescription;
    private TextView breed, name, description,profile;
    private ImageView editImageView;

    private DatabaseReference collectionRef, mDataRef;
    private StorageReference imageReference, fileRef;
    private LinearLayout editLayout;
    private boolean isDay=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_collection);

        userId = getIntent().getExtras().get("userId").toString();
        collectionID = getIntent().getExtras().get("collectionId").toString();

        mChooseEditImage = findViewById(R.id.choose_edit_image);
        mButtonDone = findViewById(R.id.button_done);
        mEditName = findViewById(R.id.name_changed);
        mEditBreed = findViewById(R.id.breed_changed);
        mEditDescription = findViewById(R.id.description_changed);
        name = findViewById(R.id.text_view_name);
        breed = findViewById(R.id.text_view_breed);
        description = findViewById(R.id.text_view_description);
        profile = findViewById(R.id.profile);

        editImageView = findViewById(R.id.edit_image_view);
        editLayout = findViewById(R.id.edit_layout);

        //light sensor register
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener((SensorEventListener) this,lightSensor,sensorManager.SENSOR_DELAY_FASTEST);

        collectionRef = FirebaseDatabase.getInstance().getReference().child("DogCollection").child(userId)
        .child(collectionID);
        fileRef = null;

        mChooseEditImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choosePhotoFromGallary();
            }
        });

        retrieveDogCollection();
        mButtonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = mEditName.getText().toString();
                String breed = mEditBreed.getText().toString();
                String description = mEditDescription.getText().toString();

                mDataRef = FirebaseDatabase.getInstance().getReference("DogCollection/" +
                        userId + "/" + collectionID);
                imageReference = FirebaseStorage.getInstance().getReference().child("DogCollection/" +
                        userId + "/" + collectionID);
                editImage();

                collectionRef.child("name").setValue(name);
                collectionRef.child("breed").setValue(breed);
                collectionRef.child("description").setValue(description);
                finish();
            }
        });

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
                Picasso.get().load(mImageUri).into(editImageView);
            } else if (data == null){
            }

        }
    }

    private void editImage() {
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
                                    imageUpdates.put("/url/", imageUpload);

                                    mDataRef.updateChildren(imageUpdates);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                }
                            });

                            Toast.makeText(EditCollectionActivity.this, "Submit Successful ", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {

                            Toast.makeText(EditCollectionActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
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
                            System.out.println("Edit is paused!");
                        }
                    });
        } else {
        }
    }

    private void retrieveDogCollection() {
        collectionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.hasChild("url") && dataSnapshot.hasChild("name")
                        && dataSnapshot.hasChild("description") && dataSnapshot.hasChild("breed")){
                    mEditName.setText(dataSnapshot.child("name").getValue().toString());
                    mEditBreed.setText(dataSnapshot.child("breed").getValue().toString());
                    mEditDescription.setText(dataSnapshot.child("description").getValue().toString());
                    String profileImage = dataSnapshot.child("url").getValue().toString();

                    Picasso.get().load(profileImage)
                            .resize(200,200)
                            .centerCrop()
                            .placeholder(R.drawable.dogprofile)
                            .into(editImageView);
                }
                else {
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_LIGHT){
            int grayShade = (int)event.values[0];
            if (grayShade > 20000){
                isDay=true;
                editLayout.setBackgroundResource(R.drawable.light_dog2);
                mEditName.setTextColor(Color.BLACK);
                mEditBreed.setTextColor(Color.BLACK);
                mEditDescription.setTextColor(Color.BLACK);
                name.setTextColor(Color.BLACK);
                breed.setTextColor(Color.BLACK);
                description.setTextColor(Color.BLACK);
                profile.setTextColor(Color.BLACK);
            }else if (grayShade <=20000 && isDay==true) {
                isDay = false;
                editLayout.setBackgroundResource(R.drawable.dog2);
                mEditName.setTextColor(Color.WHITE);
                mEditBreed.setTextColor(Color.WHITE);
                mEditDescription.setTextColor(Color.WHITE);
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
}
