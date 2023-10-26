package com.example.appointy;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ChatOverviewActivity extends AppCompatActivity {

    Button add_room;
    EditText room_name;
    ListView listView;
    ArrayList<String> roomlist = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;
    String username;
    DatabaseReference root = FirebaseDatabase.getInstance("https://appointy-login-register-default-rtdb.europe-west1.firebasedatabase.app").getReference().getRoot();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_overview);

        add_room  = findViewById(R.id.btn_add_room);
        room_name = findViewById(R.id.room_name_edittext);
        listView  = findViewById(R.id.listView);

        username = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("username")).toString();

        arrayAdapter = new ArrayAdapter<String>(this,R.layout.custom_simple_item_list,roomlist) {
            //keine schöne, aber eine funktionierende Lösung, um die Farbe der ListItems zu setzen.
            //https://stackoverflow.com/questions/4533440/android-listview-text-color
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView textView = view.findViewById(android.R.id.text1);

                /*YOUR CHOICE OF COLOR*/
                textView.setTextColor(Color.BLACK);

                return view;
            }
        };

        listView.setAdapter(arrayAdapter);

        add_room.setOnClickListener(view -> {

            Map<String,Object> map = new HashMap<>();
            map.put(room_name.getText().toString(),"");
            root.child("Chats").updateChildren(map);
            room_name.setText(null); //Text initialisieren

        });

        //zur Anzeige der Räume
        root.child("Chats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Set<String> set = new HashSet<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    set.add(snapshot.getKey());
                }

                roomlist.clear();
                roomlist.addAll(set);

                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            //ausgewählten Chatroom öffnen
            Intent intent = new Intent(getApplicationContext(),ChatRoomActivity.class);
            intent.putExtra("room_name",((TextView)view).getText().toString() );
            intent.putExtra("user_name",username);
            startActivity(intent);
        });

    }

}