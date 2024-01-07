package com.example.pathfinderplus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.SignInMethodQueryResult;

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
    /*    signUpButton.setOnClickListener(new View.OnClickListener() {
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
        });*/
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSignUpDialog();
            }

            private void showSignUpDialog() {
                // Create a custom layout for the sign-up dialog
                View dialogView = getLayoutInflater().inflate(R.layout.custom_sign_up_dialog, null);

                // Initialize EditTexts and Button from the custom layout
                EditText emailEditText = dialogView.findViewById(R.id.emailEditText);
                EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
                EditText confirmPasswordEditText = dialogView.findViewById(R.id.confirmPasswordEditText);
                Button enterButton = dialogView.findViewById(R.id.enterButton);

                // Build the dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setView(dialogView);
                builder.setTitle("Sign Up");

                // Set up the button click listener
                AlertDialog dialog = builder.create();
                enterButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String email = emailEditText.getText().toString().trim();
                        String password = passwordEditText.getText().toString().trim();
                        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                            showErrorDialog("ישנם שדות חסרים");
                            return;
                        }

                        if (!password.equals(confirmPassword)) {
                            showErrorDialog("הסיסמאות אינן תואמות");
                            return;
                        }

                        // Perform user sign-up using Firebase Authentication
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
                                            // Toast.makeText(LoginActivity.this, "Sign-up failed", Toast.LENGTH_SHORT).show();
                                            try {
                                                throw task.getException();
                                            } catch(FirebaseAuthWeakPasswordException e) {
                                                showErrorDialog("סיסמה קצרה מידי");
                                            } catch(FirebaseAuthUserCollisionException e) {
                                                showErrorDialog("כתובת מייל כבר קיימת במערכת");
                                            }  catch(FirebaseAuthInvalidCredentialsException e) {
                                                showErrorDialog("כתובת או סיסמה אינם תקינים");
                                            }catch(Exception e) {
                                                Log.e("mylog", e.getMessage());
                                            }
                                        }
                                        dialog.dismiss(); // Dismiss the dialog after sign-up attempt
                                    }
                                });
                    }
                });

                // Show the dialog
                dialog.show();
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
        // Set the click listener for the "Forgot Password" link
        TextView forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        forgotPasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });


    }
    private void showForgotPasswordDialog() {
        // Create a custom layout for the forgot password dialog
        View dialogView = getLayoutInflater().inflate(R.layout.custom_forgot_password_dialog, null);

        // Initialize views from the custom layout
        EditText emailEditText = dialogView.findViewById(R.id.forgotPasswordEmailEditText);
        Button resetPasswordButton = dialogView.findViewById(R.id.resetPasswordButton);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Forgot Password");

        // Set up the button click listener
        AlertDialog dialog = builder.create();
        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEditText.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if the email exists in Firebase
                FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
                        .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                            @Override
                            public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                                if (task.isSuccessful()) {
                                    SignInMethodQueryResult result = task.getResult();
                                   // if (!result.getSignInMethods().isEmpty()) {
                                        // Email exists, send password reset email
                                        sendPasswordResetEmail(email);
                                  //  } else {
                                        // Email does not exist
                                  //      Toast.makeText(LoginActivity.this, "Email not registered", Toast.LENGTH_SHORT).show();
                                  //  }
                                } else {
                                    // Failed to check email existence
                                    Toast.makeText(LoginActivity.this, "Failed to check email existence", Toast.LENGTH_SHORT).show();
                                }
                                dialog.dismiss(); // Dismiss the dialog after checking email existence
                            }
                        });
            }
        });

        // Show the dialog
        dialog.show();
    }

    private void sendPasswordResetEmail(String email) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Password reset email sent successfully
                            Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            // Failed to send password reset email
                            Toast.makeText(LoginActivity.this, "Failed to send password reset email", Toast.LENGTH_SHORT).show();
                        }
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
                            try {
                                throw task.getException();
                            } catch(FirebaseAuthWeakPasswordException e) {
                                /*mTxtPassword.setError(getString(R.string.error_weak_password));
                                mTxtPassword.requestFocus();*/
                                showErrorDialog("סיסמה קצרה מידי");
                            } catch(FirebaseAuthUserCollisionException e) {
                                /*mTxtEmail.setError(getString(R.string.error_user_exists));
                                mTxtEmail.requestFocus();*/
                                showErrorDialog("סיסמה שגויה");
                            }  catch(FirebaseAuthInvalidCredentialsException e) {
                                /*mTxtEmail.setError(getString(R.string.error_invalid_email));
                                mTxtEmail.requestFocus();*/
                                showErrorDialog("כתובת או סיסמה אינם תקינים");
                            }catch(Exception e) {
                                Log.e("mylog", e.getMessage());
                            }

                            Log.d("mylog", "login failed: "+task.getException().getMessage());
                        }
                    }
                });
    }

    private void showErrorDialog(String errorMsg) {
        // Create a custom dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.custom_dialog);

        // Set up dialog components
        TextView dialogText = dialog.findViewById(R.id.dialogText);
        Button okButton = dialog.findViewById(R.id.okButton);

        // Set the error message
        dialogText.setText(errorMsg);

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
