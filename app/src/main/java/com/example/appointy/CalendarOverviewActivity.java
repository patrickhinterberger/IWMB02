package com.example.appointy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CalendarOverviewActivity extends AppCompatActivity {

    CalendarView calendarView;
    EditText editText;
    EditText editTextOrt;
    EditText editTextStartTime;
    EditText editTextEndTime;
    String stringDateSelected;
    String selectedDate;
    FirebaseAuth  mAuth;
    DatabaseReference secondReference;
    DatabaseReference latestEventReference;
    DatabaseReference organizerReference;
    DatabaseReference usernameReference;
    String temp_key;
    FirebaseApp secondApp;
    String lastDateString;
    Spinner spinner;
    ArrayList<String> spinnerValues = new ArrayList<>();
    String selectedUsername;
    String lastOrganizer;
    String lastOrganizerUid;
    int lastOrganizerPosition;
    int nextOrganizerPosition;
    String nextOrganizerUid;
    LocalDate nextSaturday;
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    LocalDate LatestEventLocalDate;
    String dbUser;
    int adapterIndex;

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

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_overview);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(CalendarOverviewActivity.this, android.R.layout.simple_spinner_item, spinnerValues) {
            //da die Einfärbung + Textgröße der Items des Dropdowns im XML nicht funktioniert, muss es hier gemacht werden
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK);
                //https://stackoverflow.com/questions/3687065/textview-settextsize-behaves-abnormally-how-to-set-text-size-of-textview-dynam
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                return view;
            }

        };

        mAuth = FirebaseAuth.getInstance();
        calendarView = findViewById(R.id.calendarView);
        editText     = findViewById(R.id.editText);
        editTextOrt  = findViewById(R.id.editTextOrt);
        editTextStartTime = findViewById(R.id.editTextStartTime);
        editTextEndTime = findViewById(R.id.editTextEndTime);
        spinner = findViewById(R.id.Spinner);

       //die aktuellen Values fürs Dropdown holen
        organizerReference = FirebaseDatabase.getInstance("https://appointy-login-register-default-rtdb.europe-west1.firebasedatabase.app").getReference().child("Organizers");
        usernameReference = FirebaseDatabase.getInstance("https://appointy-login-register-default-rtdb.europe-west1.firebasedatabase.app").getReference().child("Users");
        latestEventReference = FirebaseDatabase.getInstance("https://appointy-login-register-default-rtdb.europe-west1.firebasedatabase.app").getReference().child("LatestEvent");

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        organizerReference.get().addOnCompleteListener(task -> {

            for (DataSnapshot ds : task.getResult().getChildren()) {
                //im key steht die eindeutige UID
                String key = ds.getKey();
                //spinnerValues.add(key);
                //mit der UID suchen wir jetzt noch einen brauchbaren Usernamen raus

                assert key != null;
                usernameReference.child(key).get().addOnCompleteListener(task1 -> {
                    DataSnapshot ds1 = task1.getResult();
                    String value = (String) ds1.getValue();
                    spinnerValues.add(value);
                    Log.e("SpinnerValue", "Value: " + spinnerValues);

                    //Adapter für den Spinner erst createn, wenn Values in der ArrayList sind, sonst springen die OnItemSelected-Methoden nicht an
                    //https://github.com/Lesilva/BetterSpinner/issues/11
                    spinner.setAdapter(adapter);
                    spinner.setSelection(0);
                    Log.e("SpinnerValue2", "Value: " + spinnerValues.toString());

                });

            }

        });



        //Listener für Datums-Auswahl
        //int i = Jahr, int i1 = Monat, int i2 = Datum
        calendarView.setOnDateChangeListener((calendarView, year, month, day) -> {

            //Datumsformat
            //Hilfe aus https://stackoverflow.com/questions/12121214/android-calendarview-how-do-i-get-the-date-in-correct-format

            selectedDate = sdf.format(new Date(calendarView.getDate()));

            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day);
            String datum = sdf.format(cal.getTime());
            Log.e("datum", datum);

            selectedDate = datum;

            //ausgewähltes Datum auslesen
            //Besonderheit: Monat startet bei 0 -> daher i1 + 1 rechnen
            stringDateSelected = year + Integer.toString(month + 1) + day;

            calendarClicked();

        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //den Wert auslesen, den der User aus dem Dropdown wählt
                selectedUsername = spinner.getSelectedItem().toString();
                Log.e("selectedUsername", "selectedUsername: " + selectedUsername);
                spinner.setSelection(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    private void calendarClicked(){

    }


    public void buttonSave(View view){

        Log.d("Speichern", "Event-Speichervorgang gestartet.");

        //als allererstes muss überprüft werden, ob an dem Tag bereits ein Event stattfindet

        secondReference.child("Appointments").child(selectedDate).get().addOnCompleteListener(task -> {

            //wenn Call gegen DB nicht erfolgreich
            if (!task.isSuccessful()) {
                Log.e("firebase", "Error getting data", task.getException());

            }
            //wenn Call gegen DB erfolgreich -> nachsehen ob wir an dem Tag was haben
            else {

                //hier dann wirklich abfragen, ob wir ein Event an dem Tag haben
                if (!String.valueOf(task.getResult().getValue()).equals("null")) {

                    Log.d("firebase", String.valueOf(task.getResult().getValue()));

                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(CalendarOverviewActivity.this);
                    builder.setMessage("Es existiert bereits ein Event an dem Tag. Überschreiben?");

                    //Setting message manually and performing action on button click
                    builder.setMessage("Es existiert bereits ein Event an dem Tag. Überschreiben?")
                            .setCancelable(false)
                            .setPositiveButton("Ja", (dialog, id) -> {
                                //  Action for 'Yes' Button

                                saveData();

                            })
                            .setNegativeButton("Nein", (dialog, id) -> {
                                //  Action for 'No' Button

                                Toast.makeText(CalendarOverviewActivity.this,"Speichern abgebrochen.", Toast.LENGTH_SHORT).show();

                            });
                    //Creating dialog box
                    AlertDialog alert = builder.create();
                    //Setting the title manually
                    alert.setTitle("Achtung!");
                    alert.show();

                }

                //wir haben also noch kein Event an dem Tag -> können ungestört Speichern!
                else {

                    Log.d("Event", "kein Event bisher.");
                    saveData();
                }

            }


        });


    }

    //Methode für onClick des "Vorschlagen"-Buttons
    public void buttonSuggest(View view) {

        latestEventReference.get().addOnCompleteListener(task -> {
            for (DataSnapshot ds : task.getResult().getChildren()) {
                lastOrganizer = (String) ds.getValue();
                lastDateString = ds.getKey();
            }
        });

        //nach dem nun der letzte Organisator bekannt ist, suchen wir uns die UID dazu

        usernameReference.get().addOnCompleteListener(task -> {
            for (DataSnapshot ds : task.getResult().getChildren()) {
                String key = ds.getKey();
                String value = (String) ds.getValue();

                if (value != null && value.equals(lastOrganizer)) {

                    lastOrganizerUid = key;
                    Log.d("lastOrganizerUid", "lastOrganizerUid: " + lastOrganizerUid);
                }
            }

        });

        //jetzt suchen wir die Position des letzten & nächsten Organizers
        organizerReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            int i = 0;
            int valueInt;
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                for (DataSnapshot ds : task.getResult().getChildren()) {
                    i++;

                    Long value = (Long) ds.getValue();
                    if (value != null) {
                        valueInt = value.intValue();
                    }
                    String key = ds.getKey();

                    if (key != null && key.equals(lastOrganizerUid)) {

                        Long lastOrganizerPositionLog = (Long) ds.getValue();
                        lastOrganizerPosition = lastOrganizerPositionLog.intValue();

                    }

                }

                nextOrganizerPosition = lastOrganizerPosition + 1;
                //wenn der nächste Organizer größer als der letztmögliche wäre, zurücksetzen
                if (nextOrganizerPosition >  i) {
                    nextOrganizerPosition = 0;
                }

            }

        });

        //dann müssen wir noch schauen, ob wir eh nicht das Ende der Organizer-Liste erreicht haben..
        organizerReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(CalendarOverviewActivity.this, android.R.layout.simple_spinner_item, spinnerValues);
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                for (DataSnapshot ds : task.getResult().getChildren()) {
                    Long value = (Long) ds.getValue();
                    int intValue = 0;
                    if (value != null) {
                        intValue = value.intValue();
                    }
                    String key = ds.getKey();

                    if (intValue == nextOrganizerPosition) {

                        nextOrganizerUid = key;
                        Log.d("nextOrganizerUid", " nextOrganizerUid: " +  nextOrganizerUid);

                        //TODO: die Position des Namens im Spinner ist nicht gleich jener in der DB..
                        usernameReference.child(nextOrganizerUid).get().addOnCompleteListener(task1 -> {
                            DataSnapshot ds1 = task1.getResult();
                            dbUser = (String) ds1.getValue();
                            Log.d(" dbUser", "dbUser: " +  dbUser);
                            adapterIndex = adapter.getPosition(dbUser);
                            spinner.setSelection(adapterIndex);
                        });

                    }
                }

                Date date = null;
                try {
                    date = sdf.parse(lastDateString);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                //nächsten Samstag ausgehend vom nächsten Spieltermin ermitteln
                if (date != null) {
                    LatestEventLocalDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
                nextSaturday = LatestEventLocalDate.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

                //mit dem LocalDate können wir im CalendarView leider nix anfangen, daher konvertieren
                //hier gibts noch die Besonderheit: LocalDate geht von 1-12, Calendar aber von 0-11..
                calendar.set(nextSaturday.getYear(), nextSaturday.getMonthValue() - 1, nextSaturday.getDayOfMonth());

                //den nächsten Organizer + den nächsten Samstag setzen

                //spinner.setSelection(nextOrganizerPosition);
                calendarView.setDate(calendar.getTimeInMillis(), false, true);
                //außerdem müssen wir das selectedDate setzen, sonst dumpt die App
                selectedDate = sdf.format(calendarView.getDate());
                Log.d("selectedDate", "selectedDate: " + selectedDate);

            }
        });

        Toast.makeText(CalendarOverviewActivity.this, "Nächsten Spieltermin vorgeschlagen!", Toast.LENGTH_SHORT).show();

    }

    //Methode zum speichern der Daten
    private void saveData() {

        //Datum speichern
         secondReference.child("Appointments").child(selectedDate).setValue(editText.getText().toString());
        // secondReference.child(stringDateSelected).setValue(editText.getText().toString());

        //Key-Value-Pair für Ort + Bezeichnung
        Map<String, Object> map = new HashMap<>();
        temp_key = secondReference.child("Appointments").child(selectedDate).getKey();
        secondReference.updateChildren(map);

        //DatabaseReference message_root = secondReference.child(temp_key);
        DatabaseReference message_root = secondReference.child("Appointments").child(temp_key);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("Veranstalter", selectedUsername);
        map2.put("Ort", editTextOrt.getText().toString());
        map2.put("Eventbezeichnung", editText.getText().toString());

        Map<String, Object> mapTime = new HashMap<>();
        mapTime.put("Startzeit", editTextStartTime.getText().toString());
        mapTime.put("Endzeit", editTextEndTime.getText().toString());

        message_root.updateChildren(map2);
        message_root.updateChildren(mapTime);

        saveToLatestEvent();

        //ToDo: hier noch abfragen, ob speichern erfolgreich war..
        Toast.makeText(this,"Event gespeichert!", Toast.LENGTH_SHORT).show();

        editText.setText("Eventbezeichnung"); //Platzhalter nach Versand initialisieren
        editTextOrt.setText("Ort"); //Platzhalter nach Versand initialisieren
        editTextStartTime.setText("18:00"); //Platzhalter nach Versand initialisieren
        editTextEndTime.setText("22:00"); //Platzhalter nach Versand initialisieren
    }

    public void saveToLatestEvent() {

        //das alte Event müssen wir noch bügeln, bevor wir das neue schreiben
        latestEventReference.removeValue();

        //wenn wir ein Event speichern, nehmen wir das als Referenz für die Veranstalter-Rotation
        HashMap mapData = new HashMap();

        mapData.put(selectedDate, selectedUsername);
        latestEventReference.updateChildren(mapData);



    }

}