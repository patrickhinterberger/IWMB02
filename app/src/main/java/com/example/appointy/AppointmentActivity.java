package com.example.appointy;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AppointmentActivity extends AppCompatActivity {

    String selectedDate;
    TextView datum;
    TextView Ort;
    TextView Startzeit;
    TextView Endzeit;
    TextView Eventbezeichnung;
    TextView Veranstalter;
    DatabaseReference secondReference;
    FirebaseApp secondApp;
    RatingBar ratingBarGast;
    RatingBar ratingBarFood;
    RatingBar ratingBarOverall;
    String stringGast;
    String stringFood;
    String stringOverall;
    Button saveRatings;
    FirebaseUser User;
    FirebaseAuth auth;
    String email;
    String subStringEmail;
    String footRate;
    String guestRate;
    String overallRate;
    LocalDate selectedDateAsDate;
    LocalDate todaysDate;
    Button voteForGames;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_appointment);
        //zweite DB initialisieren

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApiKey("AIzaSyAOg5AloQAJWgYXxO7S8NRl-_CUr-fDSOU")
                .setApplicationId("1:430597324358:android:0eae417bbe870d84a21447")
                .setDatabaseUrl("https://appointy-calendar-default-rtdb.europe-west1.firebasedatabase.app")
                .build();

        //kontrollieren, ob bereits initalisiert - ansonsten schmiert die App bei 2 Aufrufen hintereinander ab
        try {
            secondApp = FirebaseApp.initializeApp(getApplicationContext(), options, "second app");
            Log.d("Instanz", "neue Initialisierung der App.");
        } catch (IllegalStateException e) {
            //in dem Fall müssma nix tun..
            secondApp = FirebaseApp.getInstance("second app");
            Log.d("Init", "App bereits initialisiert.");

        }

        secondReference = FirebaseDatabase.getInstance(secondApp).getReference();

        auth = FirebaseAuth.getInstance();
        datum = findViewById(R.id.selectedDate);
        Ort = findViewById(R.id.Ort);
        Startzeit = findViewById(R.id.Startzeit);
        Endzeit = findViewById(R.id.Endzeit);
        Veranstalter = findViewById(R.id.editTextOwner);
        Eventbezeichnung = findViewById(R.id.Eventbezeichnung);
        ratingBarGast = findViewById(R.id.ratingBarGast);
        ratingBarFood = findViewById(R.id.ratingBarFood);
        ratingBarOverall = findViewById(R.id.ratingBarOverall);
        saveRatings = findViewById(R.id.saveRatings);
        voteForGames = findViewById(R.id.voteForGames);

       selectedDate = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("clickedDate")).toString();
       subStringEmail = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("username")).toString();
       //gleich noch ein Date-Format draus machen + heutiges Datum zum Vergleichen erzeugen
        selectedDateAsDate = LocalDate.parse(selectedDate);
        todaysDate         = LocalDate.now();

        Log.d("Intent-Daten:", "Daten:" + selectedDate);
        datum.setText("Datum:" + " " + selectedDate);

        // aktuellen User auslesen
        User = auth.getCurrentUser();
        Log.d("User:", "Daten:" + User);
        email = User.getEmail();

        secondReference.child("Ratings").child(selectedDate).child(subStringEmail).get().addOnCompleteListener(task -> {

            //RatingBars sollen nur eingabebereit sein, wenn das Event in der Vergangenheit liegt!
            if (todaysDate.isAfter(selectedDateAsDate)) {
                ratingBarFood.setEnabled(true);
                ratingBarGast.setEnabled(true);
                ratingBarOverall.setEnabled(true);
            }
            else {

                //Meldung, dass noch keine Bewertung möglich ist
                Toast.makeText(AppointmentActivity.this,"Event ist in der Zukunft - noch keine Bewertung möglich!", Toast.LENGTH_SHORT).show();

                //RatingBars + Speichern-Button disablen
                ratingBarFood.setEnabled(false);
                ratingBarGast.setEnabled(false);
                ratingBarOverall.setEnabled(false);
                saveRatings.setEnabled(false);
            }

            //wenn Call gegen DB nicht erfolgreich
            if (!task.isSuccessful()) {
                Log.e("firebase", "Error getting data", task.getException());

            }
            else {

                // je nach Key im Loop die jeweilige Value auslesen und auf die globale Variable umschreiben
                for (DataSnapshot ds : task.getResult().getChildren()) {
                    String key   = ds.getKey();
                    Log.d("key", "Daten:" + key);

                    if (Objects.equals(key, "Bewertung_Essen")) {

                        footRate = ds.getValue(String.class);
                        assert footRate != null;
                        float floatRateFood = Float.parseFloat(footRate);
                        ratingBarFood.setRating(floatRateFood);
                        Log.d("Essen:", "Daten:" + footRate);

                    } else if (Objects.equals(key, "Bewertung_Gastfreundlichkeit")) {

                        guestRate = ds.getValue(String.class);
                        assert guestRate != null;
                        float guestRateFood = Float.parseFloat(guestRate);
                        ratingBarGast.setRating(guestRateFood);
                        Log.d("Gastfreundlichkeit:", "Daten:" + guestRate);

                    } else {

                        overallRate = ds.getValue(String.class);
                        assert overallRate != null;
                        float overallRateFood = Float.parseFloat(overallRate);
                        ratingBarOverall.setRating(overallRateFood);
                        Log.d("Gesamt:", "Daten:" + overallRate);
                    }

                }

            }

        });

        secondReference.child("Appointments").child(selectedDate).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot DataSnapshot : snapshot.getChildren()) {
                        // loop over all child nodes
                        String OrtL = snapshot.child("Ort").getValue(String.class);
                        Log.d("Ort:", "Daten:" + Ort);

                    String Ende = snapshot.child("Endzeit").getValue(String.class);
                    Log.d("Ende:", "Daten:" + Ende);

                    String Start = snapshot.child("Startzeit").getValue(String.class);
                    Log.d("Start:", "Daten:" + Start);

                    String Event = snapshot.child("Eventbezeichnung").getValue(String.class);
                    Log.d("Event:", "Daten:" + Event);

                    String Veranst = snapshot.child("Veranstalter").getValue(String.class);
                    Log.d("Veranstalter:", "Daten:" + Veranstalter);

                    Ort.setText("Ort:" + " " + OrtL);
                    Startzeit.setText("Start:" + " " + Start);
                    Endzeit.setText("Ende:" + " " + Ende);
                    Eventbezeichnung.setText("Event:" + " " + Event);
                    Veranstalter.setText("Veranstalter:" + " " + Veranst);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //von den 3 RatingBars die jeweils ausgewählten Werte holen

        ratingBarGast.setOnRatingBarChangeListener((ratingBar, v, b) -> {
            float ratingValue = ratingBar.getRating();
            stringGast = String.valueOf(ratingValue);
            Log.d("stringGast:", "Daten:" + stringGast);

        });

        ratingBarFood.setOnRatingBarChangeListener((ratingBar, v, b) -> {
            float ratingValue = ratingBar.getRating();
            stringFood = String.valueOf(ratingValue);
            Log.d("stringFood:", "Daten:" + stringFood);

        });

        ratingBarOverall.setOnRatingBarChangeListener((ratingBar, v, b) -> {
            float ratingValue = ratingBar.getRating();
            stringOverall = String.valueOf(ratingValue);
            Log.d("stringOverall:", "Daten:" + stringOverall);

        });

        //onClick-Listener fürs Speichern der RatinWerte implementieren

        saveRatings.setOnClickListener(view -> {

            //Nachsehen, ob der Node bereits angelegt ist, ansonsten anlegen..
            secondReference.child("Ratings").child(selectedDate).child(subStringEmail).get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());

                }
                else {

                    //Logik zur Abfrage, ob die Bewertung bereits existiert

                    if (!String.valueOf(task.getResult().getValue()).equals("null")) {
                        //wenn das zutrifft -> bereits ein Wert da!


                        AlertDialog.Builder builder;
                        builder = new AlertDialog.Builder(AppointmentActivity.this);
                        builder.setMessage("Ihre bestehende Bewertung überschreiben?");

                        //Setting message manually and performing action on button click
                        builder.setMessage("Ihre bestehende Bewertung überschreiben?")
                                .setCancelable(false)
                                .setPositiveButton("Ja", (dialog, id) -> {

                                   //Logik zum Speichern der Daten
                                    saveRatings();

                                })
                                .setNegativeButton("Nein", (dialog, id) -> {
                                    //  Action for 'No' Button

                                    Toast.makeText(AppointmentActivity.this,"Speichern abgebrochen.", Toast.LENGTH_SHORT).show();

                                });
                        //Dialogbox erstellen
                        AlertDialog alert = builder.create();
                        //Titel setzen
                        alert.setTitle("Achtung!");
                        alert.show();

                    }
                    else { //wenn also noch kein Wert da ist für das Datum -> anlegen!

                        saveRatings();

                    }
                }

            });

        });

        voteForGames.setOnClickListener(view -> {

            //ausgewählten Chatroom öffnen
            Intent intent = new Intent(getApplicationContext(), GameListActivity.class);
            intent.putExtra("selectedDate", selectedDate);
            intent.putExtra("subStringEmail", subStringEmail);
            startActivity(intent);

        });

    }

    private void saveRatings() {

        //secondReference.child("Ratings").child(selectedDate).setValue(selectedDate);
        secondReference.child("Ratings").child(selectedDate).child(subStringEmail).setValue(subStringEmail);

        //alle 3 Bewertungen als Child in der Map speichern
        Map<String, Object> mapData = new HashMap<>();

        secondReference.updateChildren(mapData);

        DatabaseReference message_root = secondReference.child("Ratings").child(selectedDate).child(subStringEmail);
        mapData.put("Bewertung_Gastfreundlichkeit", stringGast);
        mapData.put("Bewertung_Essen", stringFood);
        mapData.put("Bewertung_Gesamt", stringOverall);

        message_root.updateChildren(mapData);

        Toast.makeText(AppointmentActivity.this,"Bewertung gespeichert!", Toast.LENGTH_SHORT).show();

    }

}