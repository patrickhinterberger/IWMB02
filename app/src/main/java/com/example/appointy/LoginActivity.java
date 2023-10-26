package com.example.appointy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    //Texte + Button + FireBaseAuth
    TextInputEditText editTextEmail, editTextPassword;
    Button buttonLogin;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;

    //zuerst checken, ob der User aktuell bereits angemeldet ist
    @Override
    public void onStart() {
        super.onStart();
        //FirebaseApp.initializeApp();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){

            // wenn er bereits angemeldet ist: jump to MainActivity
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Auth-Objekt initialisieren
        mAuth = FirebaseAuth.getInstance();

        //hier initialisieren wir

        editTextEmail      = findViewById(R.id.email);
        editTextPassword   = findViewById(R.id.password);
        buttonLogin        = findViewById(R.id.loginButton);
        progressBar        = findViewById(R.id.progressBar);
        textView           = findViewById(R.id.registerInstead);

        textView.setOnClickListener(view -> {
            //mein erster App-interner Intent :) -> Navigation zur Login-Activity
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
            finish();

        });

        buttonLogin.setOnClickListener(view -> {

            //ProgressBar sichtbar machen
            progressBar.setVisibility(View.VISIBLE);

            String email, password;
            //Methodenvariablen zum Auslesen der Benutzereingaben

            email    = String.valueOf(editTextEmail.getText());
            password = String.valueOf(editTextPassword.getText());

            if (TextUtils.isEmpty(email)){
                Toast.makeText(LoginActivity.this,"Bitte E-Mail eingeben", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password)){
                Toast.makeText(LoginActivity.this,"Bitte Passwort eingeben", Toast.LENGTH_SHORT).show();
                return;
            }

            //Methodenimplementierung zum Login
            //aus https://firebase.google.com/docs/auth/android/password-auth?hl=de
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {

                            Toast.makeText(LoginActivity.this, "Login erfolgreich!",
                                    Toast.LENGTH_SHORT).show();

                            // mein zweiter Intent -> ab zur MainActivity

                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            startActivity(intent);
                            finish();

                        } else {

                            Toast.makeText(LoginActivity.this, "Login fehlgeschlagen!",
                                    Toast.LENGTH_SHORT).show();

                        }
                    });

        });


    }
}