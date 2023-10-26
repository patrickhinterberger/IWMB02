package com.example.appointy;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    Button button;
    TextView textView;
    FirebaseUser user;
    Button chatButton;
    String name;
    Button planEvent;
    DatabaseReference db;
    DatabaseReference latestAppointment;
    Button nextSession;
    TextView nextAppointmentDate;
    DatabaseReference firstReference;
    DatabaseReference secondReference;
    DatabaseReference thirdReference;
    FirebaseApp secondApp;
    ArrayList<String> dateItems = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    ListView listView;
    Map<String, Object> userUid;
    String username;

    @Override
    public void onStart() {
        super.onStart();
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

        //der Button soll bei Start 1x gedrückt werden, damit die Termindaten geladen werden
        nextSession.performClick();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        button = findViewById(R.id.logout);
        nextAppointmentDate = findViewById(R.id.nextAppointment);
        nextSession = findViewById(R.id.next_session);
        textView = findViewById(R.id.user_details);
        chatButton = findViewById(R.id.chatButton);
        planEvent = findViewById(R.id.planEvent);
        listView = findViewById(R.id.listView);


        db = FirebaseDatabase.getInstance("https://appointy-calendar-default-rtdb.europe-west1.firebasedatabase.app").getReference();
        latestAppointment = db.child("Appointments");

        // aktuellen User auslesen
        user = auth.getCurrentUser();

        // wenn kein aktueller User vorhanden ist: zum Login springen
        if (user == null) {

            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        } else {

            //Methode für Username-Anlage bzw. Auslesen
            request_user_name();

            textView.setText(user.getEmail());


        }

        button.setOnClickListener(view -> {
            // Logik fürs Ausloggen
            FirebaseAuth.getInstance().signOut();

            // danach aktuelle Activity verlassen & Login-Page öffnen

            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();

        });

        //Listener für Chat
        chatButton.setOnClickListener(view -> {
            //in den Chat springen
            Intent intent = new Intent(getApplicationContext(), ChatOverviewActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        //Listener für "Neues Event"
        planEvent.setOnClickListener(view -> {
            //in den Chat springen
            Intent intent = new Intent(getApplicationContext(), CalendarOverviewActivity.class);
            startActivity(intent);
        });

        //Listener für Click auf ein Item aus dem ListView
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = (String) parent.getItemAtPosition(position);
            Log.d("Clicked on:", "Daten:" + selectedItem);

            Intent intent = new Intent(getApplicationContext(), AppointmentActivity.class);
            intent.putExtra("clickedDate",selectedItem); //geklicktes Datum in die nächste Activity mitgeben
            intent.putExtra("username", username);
            startActivity(intent);
            //finish();

        });

    }

    public void buttonSync(View view) {

        arrayAdapter = new ArrayAdapter<String>(this, R.layout.custom_simple_item_list,dateItems) {
            //keine schöne, aber eine funktionierende Lösung, um die Farbe der ListItems zu setzen.
            //https://stackoverflow.com/questions/4533440/android-listview-text-color
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView textView = view.findViewById(android.R.id.text1);
                //text1 ist hier repräsentativ für das ListView-Item in der XML-Datei des simple_list_item1
                textView.setTextColor(Color.BLACK);

                return view;
            }

        };

        listView.setAdapter(arrayAdapter);

        secondReference.child("Appointments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //gemäß https://www.knowband.com/blog/tutorials/fetch-data-firebase-android/
                if (dataSnapshot.exists()) {
                    HashMap<String, Object> dataMap = (HashMap<String, Object>) dataSnapshot.getValue();

                    if (dataMap != null) {
                        for (String key : dataMap.keySet()) {

                            Object data = dataMap.get(key);

                            try {
                                HashMap<String, Object> valueData = (HashMap<String, Object>) data;


                                String Ort = null;
                                if (valueData != null) {
                                    Ort = (String) valueData.get("Ort");
                                }
                                String Endzeit = null;
                                if (valueData != null) {
                                    Endzeit = (String) valueData.get("Endzeit");
                                }
                                String Startzeit = null;
                                if (valueData != null) {
                                    Startzeit = (String) valueData.get("Startzeit");
                                }
                                String Eventbezeichnung = null;
                                if (valueData != null) {
                                    Eventbezeichnung = (String) valueData.get("Eventbezeichnung");
                                }
                                Log.d("Ort", "Daten:" + Ort);
                                Log.d("Endzeit", "Daten:" + Endzeit);
                                Log.d("Startzeit", "Daten:" + Startzeit);
                                Log.d("Eventbezeichnung", "Daten:" + Eventbezeichnung);
                                if (data != null) {
                                    Log.d("Key", "Daten:" + data);
                                }

                                Set<String> set = new HashSet<>();

                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    set.add(snapshot.getKey());
                                }

                                dateItems.clear();
                                dateItems.addAll(set);

                                //an der Stelle müssma noch sortieren..
                                Collections.sort(dateItems);

                                //dann den Adapter über die Änderungen informieren
                                arrayAdapter.notifyDataSetChanged();

                            } catch (ClassCastException cce) {


                            }
                        }
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        Toast.makeText(MainActivity.this,"Termindaten aktualisiert!", Toast.LENGTH_SHORT).show();

    }

    //Methode: bei erstmaligem Login soll nach einem Usernamen gefragt werden
    //dieser wird anschließend in einen eigenen Node geschrieben, um später wiederverwendet werden zu können.
    private void request_user_name() {

        firstReference = FirebaseDatabase.getInstance("https://appointy-login-register-default-rtdb.europe-west1.firebasedatabase.app").getReference().child("Users");

        //wir nehmen die UID des eingeloggten Users und schauen, ob der schon einen Usernamen für sich gewählt hat
        String uid = user.getUid();
        Log.e("UID", "uid:" + uid);

        //danach: in der DB nachsehen, ob bereits ein Eintrag mit dem User besteht
        firstReference.child(uid).get().addOnCompleteListener(task -> {

            if (!task.isSuccessful()) {
                Log.e("Error", "Error getting data", task.getException());

            }
            else {

                DataSnapshot dataSnapshot = task.getResult();
                String value = (String) dataSnapshot.getValue();

                if (value == null) {

                    //checken, ob es einen Eintrag mit dem User gibt

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Erster Login! Bitte einen Usernamen eingeben:");

                    final EditText input_field = new EditText(MainActivity.this);

                    builder.setView(input_field);
                    builder.setPositiveButton("OK", (dialogInterface, i) -> {
                        name = input_field.getText().toString();

                        userUid = new HashMap<>();
                        //Key: eindeutige Uid, Value: eingegebener Username
                        userUid.put(uid, name);

                        firstReference.updateChildren(userUid);
                        username = name;
                        //außerdem nehmen wir den Usernamen noch in die Liste der Organisatoren aufsetOrganizer(String uid)
                        setOrganizer(uid);

                        Toast.makeText(MainActivity.this,"Username gespeichert!", Toast.LENGTH_SHORT).show();

                    });

                    builder.setNegativeButton("Abbrechen", (dialogInterface, i) -> {
                        dialogInterface.cancel();
                        request_user_name();

                    });

                    builder.show();

                }
                else { //bereits ein Username vorhanden -> nehmen wir mit!

                    username = value;
                    Log.e("Userrrr", "User:" + username);

                }

            }

        });

    }


    public void setOrganizer(String uid) {

        thirdReference = FirebaseDatabase.getInstance("https://appointy-login-register-default-rtdb.europe-west1.firebasedatabase.app").getReference().child("Organizers");
        HashMap<String, Object> organizers = new HashMap<>();

        thirdReference.get().addOnCompleteListener(task -> {

            if (!task.isSuccessful()) {
                Log.e("Error", "Error getting data", task.getException());

            }
            else {
                Log.e("ELSE", "ELSE");
                int position;

                int totalEntries = (int) task.getResult().getChildrenCount();
                Log.e("totalEntries", "totalEntries: " + totalEntries);

                //nachsehen ob der Integer einen Wert hat
                //https://stackoverflow.com/questions/41183668/how-to-check-whether-an-integer-is-null-or-zero-in-java
                if ((Optional.of(totalEntries).orElse(0) != 0)) {

                    position = totalEntries;

                } else {

                    position = 0; //wir müssen mit 0 beginnen, sonst IndexOutOfBounds beim Nachlesen

                }
                    Log.e("Position", "Position: " + position);
                    //dann die Daten auf die DB schreiben
                    organizers.put(uid, position);
                    thirdReference.updateChildren(organizers);

            }

        });

    }

}