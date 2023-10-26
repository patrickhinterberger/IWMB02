package com.example.appointy;

import static java.lang.Boolean.parseBoolean;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GameListActivity extends AppCompatActivity {

    Button add_game;
    Button saveVoting;
    EditText game_name;
    ListView listView;
    ArrayList<String> gamelist = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    DatabaseReference gameReference;
    DatabaseReference gameVotingReference;
    FirebaseApp secondApp;
    String selectedDate;
    String subStringEmail;
    LocalDate selectedDateAsDate;
    LocalDate todaysDate;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_list);

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

        gameReference = FirebaseDatabase.getInstance(secondApp).getReference().child("Games");
        gameVotingReference = FirebaseDatabase.getInstance(secondApp).getReference().child("Gamevotings");

        add_game  = findViewById(R.id.btn_add_game);
        game_name = findViewById(R.id.game_name_edittext);
        listView  = findViewById(R.id.listView);
        saveVoting = findViewById(R.id.btn_add_votings);

        //übergebene Extras aus dem Intent dazulesen:
        selectedDate = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("selectedDate")).toString();
        subStringEmail = Objects.requireNonNull(getIntent().getExtras().get("subStringEmail")).toString();
        selectedDateAsDate = LocalDate.parse(selectedDate);
        todaysDate         = LocalDate.now();

        arrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice,gamelist) {
            //keine schöne, aber eine funktionierende Lösung, um die Farbe der ListItems zu setzen.
            //https://stackoverflow.com/questions/4533440/android-listview-text-color
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.BLACK);
                return view;
            }
        };

        listView.setAdapter(arrayAdapter);

        //wir lesen eine möglicherweise bereits getroffene Auswahl des aktuell eingeloggten Benutzers auch mal aus..
        gameVotingReference.child(selectedDate).child(subStringEmail).get().addOnCompleteListener(task -> {

            //Checkboxen sollen nur eingabebereit sein, wenn das Event in der Zukunft liegt!
            if (todaysDate.isBefore(selectedDateAsDate)) {
                saveVoting.setEnabled(true);
            }
            else {
                //Meldung, dass kein Voting mehr möglich ist
                Toast.makeText(GameListActivity.this,"Event bereits vorbei - Abstimmung geschlossen!", Toast.LENGTH_SHORT).show();
                //Speichern-Button disablen
                saveVoting.setEnabled(false);
            }

            if (!task.isSuccessful()) {
                Log.e("firebase", "Error getting data", task.getException());

            }
            else {

                //Logik zum Anzeigen der vom User gevoteten Spiele
                for (DataSnapshot ds : task.getResult().getChildren()) {
                    String key = ds.getKey();
                    String value = (String) ds.getValue();
                    boolean bval = parseBoolean(value);

                    // wir suchen aus der ListView den passenden Eintrag
                    // Finde das Listenelement, das dem Schlüssel in der ListView entspricht
                    int itemCount = listView.getCount();
                    for (int i = 0; i < itemCount; i++) {
                        Object item = listView.getItemAtPosition(i);
                        String sItem = item.toString();

                        if (sItem.equals(key)) {
                            //aktuelle Position je nach DB-Wert setzen
                            Log.d("Position", "Position:" + i + bval);
                            Log.d("sItem", "sItem:" + sItem);
                            Log.d("BVal", "Value:" + bval);
                            listView.setItemChecked(i,bval);
                        }
                    }

                    //der Liste Bescheid geben, dass sich was geändert hat..
                    arrayAdapter.notifyDataSetChanged();

                }

            }

        });

        saveVoting.setOnClickListener(view -> {

            //zuerst Eintrag mit Datum erzeugen
            Map<String,Object> map = new HashMap<>();
            map.put(selectedDate,"");
            gameVotingReference.updateChildren(map);

            //dann Knoten mit dem Username erzeugen
            gameVotingReference.child(selectedDate).child(subStringEmail).setValue("");

            //schlussendlich die gecheckten Spiele speichern
            saveVote();

            Toast.makeText(GameListActivity.this, "Auswahl gespeichert.", Toast.LENGTH_SHORT).show();

        });

        add_game.setOnClickListener(view -> {

            Map<String,Object> map = new HashMap<>();
            map.put(game_name.getText().toString(),"");
            gameReference.updateChildren(map);
            game_name.setText(null); //Text initialisieren

                arrayAdapter.notifyDataSetChanged();

            //Activity neu starten, damit die Checkboxen stimmen...
            //siehe auch https://stackoverflow.com/questions/3053761/reload-activity-in-android
            finish();
            startActivity(getIntent());

        });

        //zur Anzeige der Games
        gameReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //wir müssen 1x vorher refreshen, sonst stimmen die Checkboxen nicht mit den
                //neu hinzugefügten Spielen überein..
                arrayAdapter.notifyDataSetChanged();

                Set<String> set = new HashSet<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    set.add(snapshot.getKey());
                }

                gamelist.clear();
                gamelist.addAll(set);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //siehe auch: https://www.youtube.com/watch?v=xuMuNNyL85Y
        getMenuInflater().inflate(R.menu.gamelist_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        //if (id == R.id.item_done) {
        if (id == R.id.btn_add_votings) {

            StringBuilder itemSelected = new StringBuilder("Ausgewählte Spiele: \n");
            for (int i=0;i<listView.getCount();i++) {

                if (listView.isItemChecked(i)) {
                    itemSelected.append(listView.getItemAtPosition(i)).append("\n");
                }
            }

            Toast.makeText(this, itemSelected.toString(),Toast.LENGTH_SHORT).show();

        }

        return super.onOptionsItemSelected(item);

    }

    //Logik zum Speichern der Daten -> erstes Mal ChatGPT verwendet
    private void saveVote() {

        Map<String,Object> trueMap = new HashMap<>();
        Map<String,Object> falseMap = new HashMap<>();

        //Die angewählten Elemente suchen & verarbeiten
        List<String> checkedItems = getCheckedValues();
        for (String item : checkedItems) {

            trueMap.put(item,"true");
            gameVotingReference.child(selectedDate).child(subStringEmail).updateChildren(trueMap);

            Log.d("Checked Item", item);


        }

        //Die nicht angewählten Elemente suchen & verarbeiten
        List<String> uncheckedItems = getUncheckedValues();
        for (String item : uncheckedItems) {

            falseMap.put(item,"false");
            gameVotingReference.child(selectedDate).child(subStringEmail).updateChildren(falseMap);

            Log.d("Unchecked Item", item);
        }

    }

    // Methode zum Auslesen der nicht ausgewählten Elemente
    private List<String> getUncheckedValues() {
        List<String> uncheckedItems = new ArrayList<>();
        for (int i = 0; i < arrayAdapter.getCount(); i++) {
            if (!listView.isItemChecked(i)) {
                String uncheckedItem = gamelist.get(i);
                uncheckedItems.add(uncheckedItem);
            }
        }
        return uncheckedItems;
    }

    //Methode zum Auslesen der ausgewählten Elemente
    private List<String> getCheckedValues() {
        List<String> checkedItems = new ArrayList<>();
        for (int i = 0; i < arrayAdapter.getCount(); i++) {
            if (listView.isItemChecked(i)) {
                String uncheckedItem = gamelist.get(i);
                checkedItems.add(uncheckedItem);
            }
        }
        return checkedItems;
    }

    }
