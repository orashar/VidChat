package com.example.android.vidchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class CallActivity extends AppCompatActivity {

    private TextView callName;
    private ImageView callPic;
    private Button cancelCall, makeCall;

    private String receiverUid = "", receiverUname = "", receiverUpic = "";
    private String senderUid = "", senderUname = "", senderUpic = "";
    private DatabaseReference userRef;

    private String checker = "", callingid = "", ringingid = "";

    private MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        userRef = FirebaseDatabase.getInstance().getReference().child("users");

        callPic = findViewById(R.id.call_pic);
        callName = findViewById(R.id.call_name);
        makeCall = findViewById(R.id.make_call);
        cancelCall = findViewById(R.id.cancel_call);

        receiverUid = getIntent().getStringExtra("visit_userid");
        senderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mp = MediaPlayer.create(this, R.raw.ringtone);

        cancelCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checker = "clicked";
                mp.stop();
                cancelCalling();
            }
        });

        makeCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mp.stop();
                final HashMap<String, Object> callPickupMap = new HashMap<>();

                callPickupMap.put("picked", "picked");

                userRef.child(senderUid).child("ringing").updateChildren(callPickupMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Intent intent = new Intent(CallActivity.this, ChatActivity.class);
                            startActivity(intent);
                            finish();
                        } else{
                            Toast.makeText(getApplicationContext(), "Error occured", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
            }
        });

        getSetReceiverInfo();
    }

    private void getSetReceiverInfo() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.child(receiverUid).exists()){
                    receiverUpic = dataSnapshot.child(receiverUid).child("userpic").getValue().toString();
                    receiverUname = dataSnapshot.child(receiverUid).child("uname").getValue().toString();

                    callName.setText(receiverUname);
                    Picasso.get().load(receiverUpic).placeholder(R.mipmap.ic_launcher).into(callPic);
                }
                if(dataSnapshot.child(senderUid).exists()){

                    senderUname = dataSnapshot.child(senderUid).child("userpic").getValue().toString();
                    senderUpic = dataSnapshot.child(senderUid).child("uname").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mp.start();

        userRef.child(receiverUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChild("calling") && !dataSnapshot.hasChild("ringing") && !checker.equals("clicked")){
                    final HashMap<String, Object> callingInfo = new HashMap<>();
                    callingInfo.put("callto", receiverUid);

                    userRef.child(senderUid).child("calling").updateChildren(callingInfo).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                final HashMap<String, Object> ringingInfo = new HashMap<>();
                                ringingInfo.put("ringto", senderUid);

                                userRef.child(receiverUid).child("ringing").updateChildren(ringingInfo);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(senderUid).hasChild("ringing") && !dataSnapshot.child(senderUid).hasChild("calling")){
                    makeCall.setVisibility(View.VISIBLE);
                }
                if(dataSnapshot.child(receiverUid).child("ringing").hasChild("picked")){
                    mp.stop();
                    Intent intent = new Intent(CallActivity.this, ChatActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void cancelCalling() {
        userRef.child(senderUid).child("calling").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.hasChild("callto")){
                    callingid = dataSnapshot.child("callto").getValue().toString();

                    userRef.child(callingid).child("ringing").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                userRef.child(senderUid).child("calling").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        startActivity(new Intent(CallActivity.this, RegisterActivity.class));
                                        finish();
                                    }
                                });
                            }
                        }
                    });
                } else{
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        userRef.child(senderUid).child("ringing").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.hasChild("ringto")){
                    ringingid = dataSnapshot.child("ringto").getValue().toString();

                    userRef.child(ringingid).child("calling").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                userRef.child(senderUid).child("ringing").removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        startActivity(new Intent(CallActivity.this, RegisterActivity.class));
                                        finish();
                                    }
                                });
                            }
                        }
                    });
                } else{
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}
