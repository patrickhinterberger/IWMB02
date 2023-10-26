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

public class RegisterActivity extends AppCompatActivity {

    //Texte + Button + FireBaseAuth
    TextInputEditText editTextEmail, editTextPassword;
    Button buttonRegistration;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    TextView textView;

    //zuerst checken, ob der User aktuell bereits angemeldet ist
    @Override
    public void onStart() {
        super.onStart();
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
        setContentView(R.layout.activity_register);

        //Auth-Objekt initialisieren
        mAuth = FirebaseAuth.getInstance();

        //hier initialisieren wir

        editTextEmail      = findViewById(R.id.email);
        editTextPassword   = findViewById(R.id.password);
        buttonRegistration = findViewById(R.id.registrationbutton);
        progressBar        = findViewById(R.id.progressBar);
        textView           = findViewById(R.id.loginInstead);

        textView.setOnClickListener(view -> {
            //mein erster App-interner Intent :) -> Navigation zur Login-Activity
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();

        });


                //Methode zur Reaktion auf Benutzereingaben
        buttonRegistration.setOnClickListener(view -> {
            //ProgressBar sichtbar machen
            progressBar.setVisibility(View.VISIBLE);

            String email, password;
            //Methodenvariablen zum Auslesen der Benutzereingaben

            email    = String.valueOf(editTextEmail.getText());
            password = String.valueOf(editTextPassword.getText());

        if (TextUtils.isEmpty(email)){
           Toast.makeText(RegisterActivity.this,"Bitte E-Mail eingeben", Toast.LENGTH_SHORT).show();
           return;
        }

        if (TextUtils.isEmpty(password)){
                Toast.makeText(RegisterActivity.this,"Bitte Passwort eingeben", Toast.LENGTH_SHORT).show();
                return;
            }

        //Methode zum Erstellen von Usern
        //aus https://firebase.google.com/docs/auth/android/password-auth?hl=de#java

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        //wenn Methode ausgeführt wurde -> ProgressBar wieder weg
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {

                            //wenn Methode ausgeführt wurde -> ProgressBar wieder weg
                            progressBar.setVisibility(View.GONE);

                            Toast.makeText(RegisterActivity.this, "Account erfolgreich erstellt.",
                                    Toast.LENGTH_SHORT).show();

                            // danach zur LoginActivity navigieren
                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            startActivity(intent);
                            finish();

                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

        });

    }
}