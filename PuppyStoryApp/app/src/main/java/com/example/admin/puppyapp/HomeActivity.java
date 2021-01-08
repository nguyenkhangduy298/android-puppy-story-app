package com.example.admin.puppyapp;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,View.OnClickListener,SensorEventListener{

    private DrawerLayout drawer;
    private TextView userEmail;
    private TextView userName;
    private ImageView userImage;
    private ImageButton addCollection, stop, start;

    private FirebaseUser user;
    private FirebaseAuth mAuth;

    RecyclerView recyclerView;
    DatabaseReference databaseReference;
    FirebaseRecyclerOptions<DogModel> options;
    FirebaseRecyclerAdapter<DogModel, DogCollectionViewHolder> adapter;

    private String userId;
    private Intent playIntent;
    private boolean isDay=false;
    private DrawerLayout drawerLayout;
    private LinearLayout linearLayout;
    private ArrayList<DogCollectionViewHolder> cHolder= new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        addCollection = findViewById(R.id.add_collection);
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        userName = navigationView.getHeaderView(0).findViewById(R.id.nav_header_name);
        userEmail = navigationView.getHeaderView(0).findViewById(R.id.nav_header_email);
        userImage = navigationView.getHeaderView(0).findViewById(R.id.nav_header_image);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user.getUid();
        Log.e("id", userId);


        retrieveFirebaseData();
        getUserInfo();

        addCollection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addCollection = new Intent(HomeActivity.this, AddCollectionActivity.class);
                startActivity(addCollection);
            }
        });

        stop=findViewById(R.id.button);
        start=findViewById(R.id.button2);

        stop.setOnClickListener(this);
        start.setOnClickListener(this);

        start.setVisibility(View.INVISIBLE);
        startService(new Intent(this,MyService.class));

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this,lightSensor,sensorManager.SENSOR_DELAY_FASTEST);

        drawerLayout = findViewById(R.id.drawer_layout);
    }

    @Override
    public void onClick(View view) {
        if (view==stop) {
            stopService(new Intent(this, MyService.class));
            stop.setVisibility(View.INVISIBLE);
            start.setVisibility(View.VISIBLE);
        }
        else if (view==start){
            startService(new Intent(this,MyService.class));
            stop.setVisibility(View.VISIBLE);
            start.setVisibility(View.INVISIBLE);
        }
    }

    public void getUserInfo(){
        user = mAuth.getCurrentUser();

        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                // Name, email address, and profile photo Url
                userName.setText(profile.getDisplayName());
                userEmail.setText(profile.getEmail());
                Uri photoUrl = profile.getPhotoUrl();

                Picasso.get().load(photoUrl).into(userImage);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.nav_logout:
                mAuth.signOut();
                Intent backLogIn = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(backLogIn);
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)){
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }

    }


    public void retrieveFirebaseData(){
        recyclerView = findViewById(R.id.recyclerview);
        databaseReference = FirebaseDatabase.getInstance().getReference().child("DogCollection")
                .child(userId);

        options = new FirebaseRecyclerOptions.Builder<DogModel>().setQuery(databaseReference,
                DogModel.class).build();

        adapter = new FirebaseRecyclerAdapter<DogModel, DogCollectionViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final DogCollectionViewHolder holder, final int position, @NonNull DogModel model) {
                Picasso.get().load(model.getUrl())
                        .resize(120, 120)
                        .centerCrop()
                        .into(holder.profileImage, new Callback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
                cHolder.add(holder);
                holder.name.setText(model.getName());
                holder.breed.setText(model.getBreed());
                holder.description.setText(model.getDescription());

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Pair[] pairs = new Pair[3];
                        pairs[0] = new Pair<View, String>(holder.profileImage, "imageTransition");
                        pairs[1] = new Pair<View, String>(holder.name, "nameTransition");
                        pairs[2] = new Pair<View, String>(holder.description, "desTransition");

                        String dog_collection_id = getRef(position).getKey();
                        Intent goDetail = new Intent(HomeActivity.this, DetailCollectionActivity.class);
                        goDetail.putExtra("dog_collection_id", dog_collection_id);
                        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(HomeActivity.this, pairs);
                        startActivity(goDetail, options.toBundle());
                        //Toast.makeText(HomeActivity.this, dog_collection_id, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @NonNull
            @Override
            public DogCollectionViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(
                        R.layout.layout_collection_item, viewGroup, false
                );
                linearLayout = findViewById(R.id.linear_layout);
                return new DogCollectionViewHolder(view);
            }
        };

        recyclerView.setLayoutManager(new LinearLayoutManager(HomeActivity.this));
        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()== Sensor.TYPE_LIGHT){
            int grayShade = (int)event.values[0];
            if (grayShade > 20000){
                isDay=true;
                drawerLayout.setBackgroundResource(R.drawable.light_dog2);
//                linearLayout.setBackgroundColor(Color.LTGRAY);
                for (int u=0;u<cHolder.size();u++) {
                    cHolder.get(u).name.setTextColor(Color.BLUE);
                    cHolder.get(u).breed.setTextColor(Color.BLUE);
                    cHolder.get(u).description.setTextColor(Color.BLUE);
                }

            }else if (grayShade <=20000 && isDay==true) {
                isDay=false;
                drawerLayout.setBackgroundResource(R.drawable.dog2);
                for (int u=0;u<cHolder.size();u++) {
                    cHolder.get(u).name.setTextColor(Color.WHITE);
                    cHolder.get(u).breed.setTextColor(Color.WHITE);
                    cHolder.get(u).description.setTextColor(Color.WHITE);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }



}
