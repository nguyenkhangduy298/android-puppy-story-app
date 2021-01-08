package com.example.admin.puppyapp;

import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.core.view.View;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static android.media.MediaRecorder.VideoSource.CAMERA;

public class DetailCollectionActivity extends AppCompatActivity implements android.view.View.OnClickListener, BottomSheetToAddImage.ActionListener,
                                                                            SensorEventListener{
    private String receiverCollectionID, userId;
    private TextView dogHeaderName, dogHeaderDescription;
    private CircularImageView dogProfileImage;
    private ImageView pickedImage;
    private Uri fileUri;
    private ImageButton addImage, editProfile;
    private BottomSheetToAddImage modalBottomSheet;
    private RecyclerView recyclerView;

    private DatabaseReference dogRef, dogImageRef, mDataReference;
    private StorageReference imageReference, fileRef;
    private FirebaseUser user;
    private FirebaseRecyclerOptions<UploadImageInfo> options;
    private FirebaseRecyclerAdapter<UploadImageInfo, DogImageViewHolder> adapter;

    private static final String TAG = "StorageActivity";
    private int GALLERY_REQUEST = 1;
    private ProgressDialog progressDialog;

    private boolean isDay=false;
    private LinearLayout informationLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_collection);

        recyclerView = findViewById(R.id.dog_image_recycler);

        receiverCollectionID = getIntent().getExtras().get("dog_collection_id").toString();
        pickedImage = findViewById(R.id.image_picked);
        findViewById(R.id.btn_delete).setOnClickListener(this);
        informationLayout = findViewById(R.id.information_layout);

        // light sensor register
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener((SensorEventListener) this,lightSensor,sensorManager.SENSOR_DELAY_FASTEST);


        user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user.getUid();
        Log.e("id", userId);

        dogRef = FirebaseDatabase.getInstance().getReference().child("DogCollection").child(userId);

        dogProfileImage = findViewById(R.id.dog_profile_image);
        dogHeaderName = findViewById(R.id.dog_header_name);
        dogHeaderDescription = findViewById(R.id.dog_header_description);
        addImage = findViewById(R.id.add_image_btn);
        editProfile = findViewById(R.id.edit_profile_btn);

        retrieveDogCollection();

        mDataReference = FirebaseDatabase.getInstance().getReference("DogCollection/" +
                userId + "/" + receiverCollectionID+ "/" + "images");
        imageReference = FirebaseStorage.getInstance().getReference().child("DogCollection/" +
                userId + "/" + receiverCollectionID+ "/" + "images");
        fileRef = null;
        progressDialog = new ProgressDialog(this);

        addImage.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                modalBottomSheet = new BottomSheetToAddImage();
                modalBottomSheet.setActionListener(DetailCollectionActivity.this);
                modalBottomSheet.show(getSupportFragmentManager(), "addImage");
            }
        });

        editProfile.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                Intent intent = new Intent(DetailCollectionActivity.this, EditCollectionActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("collectionId", receiverCollectionID);
                startActivity(intent);
            }
        });

        retrieveAllImages();


    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_LIGHT){
            int grayShade = (int)event.values[0];
            if (grayShade>20000){
                isDay=true;
                dogHeaderDescription.setTextColor(Color.BLUE);
                dogHeaderName.setTextColor(Color.BLUE);
                informationLayout.setBackgroundResource(R.drawable.light_dog1);

            }else if (grayShade<=20000&&isDay==true){
                isDay=false;
                dogHeaderDescription.setTextColor(Color.BLUE);
                dogHeaderName.setTextColor(Color.BLUE);
                informationLayout.setBackgroundResource(R.drawable.dog1);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void retrieveDogCollection() {
        dogRef.child(receiverCollectionID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.hasChild("url") && dataSnapshot.hasChild("name")
                        && dataSnapshot.hasChild("description")){
                    String dogImage = dataSnapshot.child("url").getValue().toString();
                    String dogName = dataSnapshot.child("name").getValue().toString();
                    String dogDescription = dataSnapshot.child("description").getValue().toString();

                    Picasso.get().load(dogImage)
                            .resize(200,200)
                            .centerCrop()
                            .placeholder(R.drawable.dogprofile)
                            .into(dogProfileImage);
                    dogHeaderName.setText(dogName);
                    dogHeaderDescription.setText(dogDescription);
                }
                else {
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void uploadFile() {
        if (fileUri != null) {
            File f = new File(fileUri.getPath());
            String imageName = f.getName();

            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            fileRef = imageReference.child(imageName);
            fileRef.putFile(fileUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();

                            fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    UploadImageInfo imageUpload = new UploadImageInfo(uri.toString());

                                    String uploadId = mDataReference.push().getKey();
                                    mDataReference.child(uploadId).setValue(imageUpload);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                }
                            });

                            Toast.makeText(DetailCollectionActivity.this, "File Uploaded ", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressDialog.dismiss();

                            Toast.makeText(DetailCollectionActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            // progress percentage
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                            // percentage in progress dialog
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    })
                    .addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                            System.out.println("Upload is paused!");
                        }
                    });
        } else {
            Toast.makeText(DetailCollectionActivity.this, "No File!", Toast.LENGTH_LONG).show();
        }
    }

    public void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY_REQUEST) {
            if (data != null) {
                fileUri = data.getData();
                modalBottomSheet.getPickedImage(fileUri);
            } else {
            }

        }
    }

    @Override
    public void onClick(android.view.View v) {
        int i = v.getId();

        if (i == R.id.btn_delete) {
            deleteDialog();
        }
    }

    @Override
    public void onButtonClick(int id) {
        if(id == R.id.choose_image) {
            choosePhotoFromGallary();
        } else if (id == R.id.upload_image) {
            uploadFile();
            modalBottomSheet.dismiss();
        }
    }

    private void retrieveAllImages(){
        dogImageRef = FirebaseDatabase.getInstance().getReference().child("DogCollection").child(userId).
        child(receiverCollectionID).child("images");

        Log.e("data", dogImageRef.toString());

        options = new FirebaseRecyclerOptions.Builder<UploadImageInfo>().setQuery(dogImageRef, UploadImageInfo.class).build();
        adapter = new FirebaseRecyclerAdapter<UploadImageInfo, DogImageViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final DogImageViewHolder holder, final int position, @NonNull UploadImageInfo model) {
                Picasso.get().load(model.getUrl())
                        .resize(130,130)
                        .centerCrop()
                        .into(holder.imageView, new Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

                holder.itemView.setOnClickListener(new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(android.view.View v) {
                        Pair[] pairs = new Pair[1];
                        pairs[0] = new Pair<android.view.View, String>(holder.imageView, "uploadedImageTransition");
                        String dog_image_id = getRef(position).getKey();
                        Intent goDetail = new Intent(DetailCollectionActivity.this, ShowImageActivity.class);
                        goDetail.putExtra("dogImageId", dog_image_id);
                        goDetail.putExtra("userID", userId);
                        goDetail.putExtra("collectionID", receiverCollectionID);
                        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(DetailCollectionActivity.this, pairs);
                        startActivity(goDetail, options.toBundle());
                    }
                });
            }

            @Override
            public DogImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                android.view.View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_image_item, parent, false);
                return new DogImageViewHolder(view);
            }
        };

        GridLayoutManager gridLayoutManager = new GridLayoutManager(DetailCollectionActivity.this, 3);
        recyclerView.setLayoutManager(gridLayoutManager);
        adapter.startListening();
        recyclerView.setAdapter(adapter);

    }

    private void deleteDialog(){
        final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        deleteDialog.setTitle("Are you sure to delete the collection?");
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
        Query collectionQuery = dogRef.child(receiverCollectionID);

        collectionQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot collectionSnapshot: dataSnapshot.getChildren()) {
                    collectionSnapshot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });
    }

}

