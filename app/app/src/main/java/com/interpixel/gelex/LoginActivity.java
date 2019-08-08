package com.interpixel.gelex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private Button loginButton;
    private EditText loginUsername, loginPassword;
    private FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();
        loginButton = findViewById(R.id.login_button);
        loginUsername = findViewById(R.id.login_username);
        loginPassword = findViewById(R.id.login_password);

        loginButton.setOnClickListener(view -> {
            auth.signInWithEmailAndPassword(loginUsername.getText().toString().concat("@gelex.id"), loginPassword.getText().toString()).addOnCompleteListener(task ->{
               if(!task.isSuccessful()){
                   Toast.makeText(this, "GAGAL LOGIN", Toast.LENGTH_SHORT).show();
                   return;
               }
               Intent intent = new Intent(this, MainActivity.class);
               intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
               startActivity(intent);
            });
        });
    }
}
