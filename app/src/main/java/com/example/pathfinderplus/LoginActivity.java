package com.example.pathfinderplus;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private Button newListButton;
    private Button existingList;
    private String email;
    private String password;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        Button loginButton = findViewById(R.id.loginButton);
        newListButton = findViewById(R.id.newListButton);
        Button signUpButton = findViewById(R.id.signUpButton);
        existingList = findViewById(R.id.existingListButton);


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { signUpUser();}

            public void signUpUser(){
                // Perform user sign-up using Firebase Authentication
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign-up success
                                    Toast.makeText(LoginActivity.this, "Sign-up successful", Toast.LENGTH_SHORT).show();
                                    enableLists();
                                } else {
                                    // Sign-up failed
                                    Toast.makeText(LoginActivity.this, "Sign-up failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
        newListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("PASSWORD_EXTRA", password);
                startActivity(intent);

                // Show a toast message (optional)
                Toast.makeText(LoginActivity.this, "Create a new list", Toast.LENGTH_SHORT).show();
            }
        });
        existingList.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, HistoryList.class);
                intent.putExtra("PASSWORD_EXTRA", password);
                startActivity(intent);

                // Show a toast message (optional)
                Toast.makeText(LoginActivity.this, "retrieving list from history", Toast.LENGTH_SHORT).show();

            }

        });



    }
    

    private void loginUser() {
        Log.d("mylog", "loginUser: ");
        email = emailEditText.getText().toString().trim();
        password = passwordEditText.getText().toString().trim();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            enableLists();
                        } else {
                            // Login failed, display an error message
                            //Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            showErrorDialog();
                            Log.d("mylog", "login failed: "+task.getException().getMessage());
                        }
                    }
                });
    }

    private void showErrorDialog() {
        // Create a custom dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_dialog);

        // Set up dialog components
        TextView dialogText = dialog.findViewById(R.id.dialogText);
        Button okButton = dialog.findViewById(R.id.okButton);

        // Set the error message
        dialogText.setText("Email or password is incorrect");

        // Set up the OK button click listener
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dismiss the dialog when the OK button is clicked
                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();
    }

    public void enableLists(){
        newListButton.setBackgroundColor(0xFFFF0000);
        newListButton.setEnabled(true);
        existingList.setBackgroundColor(0xFFFF0000);
        existingList.setEnabled(true);
    }
}
