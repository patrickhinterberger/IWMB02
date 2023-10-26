package com.example.appointy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class ChatRoomActivity extends AppCompatActivity {

    Button sendMessage;
    EditText inputMessage;
    TextView conversation;
    String user_name;
    String room_name;
    DatabaseReference root;
    String temp_key;
    String chatMessage;
    String chatUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        sendMessage = findViewById(R.id.btn_send);
        inputMessage = findViewById(R.id.msg_input);
        conversation = findViewById(R.id.textView);

        //Raumname & Username auslesen
        user_name = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("user_name")).toString();
        room_name = Objects.requireNonNull(getIntent().getExtras().get("room_name")).toString();
        setTitle(" Raum - " + room_name);

        //Link wird hier direkt hinterlegt, sonst zu instabil in der google-services.json
        root = FirebaseDatabase.getInstance("https://appointy-login-register-default-rtdb.europe-west1.firebasedatabase.app").getReference().child("Chats").child(room_name);

        sendMessage.setOnClickListener(view -> {

            Map<String, Object> map = new HashMap<>();
            temp_key = root.push().getKey();
            root.updateChildren(map);

            DatabaseReference message_root = root.child(temp_key);
            Map<String, Object> map2 = new HashMap<>();
            map2.put("name", user_name);
            map2.put("msg", inputMessage.getText().toString());

            message_root.updateChildren(map2);

            inputMessage.setText(null); //Message nach Versand initialisieren
        });

        root.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {

                append_chat_conversation(dataSnapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {

                append_chat_conversation(dataSnapshot);

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void append_chat_conversation(DataSnapshot dataSnapshot) {

        Iterator i = dataSnapshot.getChildren().iterator();

        while (i.hasNext()) {

            chatMessage = (String) ((DataSnapshot) i.next()).getValue();
            chatUsername = (String) ((DataSnapshot) i.next()).getValue();

            conversation.append(chatUsername + " : " + chatMessage + " \n");

        }
    }

}