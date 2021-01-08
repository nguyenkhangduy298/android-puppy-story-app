package com.example.admin.puppyapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "LoginActivity";

    private TextView txtStatus;
    private EditText edtEmail;
    private EditText edtPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtStatus = findViewById(R.id.status);
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);

        findViewById(R.id.btn_email_sign_in).setOnClickListener(this);
        findViewById(R.id.btn_email_create_account).setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void updateUI(FirebaseUser user) {
        if(user != null) {
            findViewById(R.id.status).setVisibility(View.GONE);
            Intent moveHome = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(moveHome);
        } else {
            txtStatus.setText("Signed Out. Please Sign In");
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();

        if(i == R.id.btn_email_create_account) {
            createAccount(edtEmail.getText().toString(), edtPassword.getText().toString());

        } else if(i == R.id.btn_email_sign_in) {
            signIn(edtEmail.getText().toString(), edtPassword.getText().toString());
        }
    }

    private void signIn(String email, String password) {
        Log.e(TAG, "signIn" + email);
        if(!validateForm(email, password)) {
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            Log.e(TAG, "SignIn: Success!");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                            Intent moveHome = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(moveHome);
                        } else {
                            Log.e(TAG, "signIn: Fail", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        if (!task.isSuccessful()) {
                            txtStatus.setText("Authentication failed!");
                        }
                    }
                });
    }

    private void createAccount(String email, String password) {
        Log.e(TAG, "create Account: " + email);
        if(!validateForm(email, password)) {
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            Log.e(TAG, "createAccount: Success!");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                            Intent moveHome = new Intent(LoginActivity.this, HomeActivity.class);
                            startActivity(moveHome);
                        }
                    }
                });
    }

    private boolean validateForm(String email, String password) {
        if(TextUtils.isEmpty(email)){
            Toast.makeText(LoginActivity.this, "Enter email address",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
//
//        if(Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//            Toast.makeText(LoginActivity.this, "Invalid email address",
//                    Toast.LENGTH_SHORT).show();
//            return false;
//        }

        if(TextUtils.isEmpty(password)){
            Toast.makeText(LoginActivity.this, "Enter email password",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if(password.length() < 6) {
            Toast.makeText(LoginActivity.this, "Password too short, enter minimum 6 characters",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}
