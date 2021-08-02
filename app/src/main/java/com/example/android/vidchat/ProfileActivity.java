package com.example.android.vidchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.media.Image;
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

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserId = "", receiverUserImage = "", receiverUserName = "";
    private ImageView profileImageiv;
    private TextView usernametv;
    private Button addFriendBtn, cancelRequestBtn;

    private FirebaseAuth mAuth;
    private DatabaseReference friendRequestRef, contactRef;
    private String currentUid;

    private String currentState = "new";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUid = mAuth.getCurrentUser().getUid();

        friendRequestRef = FirebaseDatabase.getInstance().getReference().child("firend_requests");
        contactRef = FirebaseDatabase.getInstance().getReference().child("contacts");

        receiverUserId = getIntent().getStringExtra("visit_userid");
        receiverUserName = getIntent().getStringExtra("profile_name");
        receiverUserImage = getIntent().getStringExtra("profile_image");

        profileImageiv = findViewById(R.id.profile_image_iv);
        usernametv = findViewById(R.id.profile_name_tv);
        addFriendBtn = findViewById(R.id.profile_sendreq_btn);
        cancelRequestBtn = findViewById(R.id.profile_cancelreq_btn);

        Picasso.get().load(receiverUserImage).placeholder(R.mipmap.ic_launcher).into(profileImageiv);
        usernametv.setText(receiverUserName);

        manageBtnClicks();
    }

    private void manageBtnClicks() {

        friendRequestRef.child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(receiverUserId)){
                    String requestType = dataSnapshot.child(receiverUserId).child("request_type").getValue().toString();
                    if(requestType.equals("sent")){
                        currentState = "request_sent";
                        addFriendBtn.setText("Cancel request");
                    } else if(requestType.equals("received")){
                        currentState = "request_received";
                        addFriendBtn.setText("Accept request");

                        cancelRequestBtn.setVisibility(View.VISIBLE);
                        cancelRequestBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                cancelFriendRequest();
                            }
                        });
                    }
                } else{
                    contactRef.child(currentState).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.hasChild(receiverUserId)){
                                currentState = "friends";
                                addFriendBtn.setText("Delete Connection");
                            } else{
                                currentState = "new";
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(currentUid.equals(receiverUserId)){
            addFriendBtn.setVisibility(View.GONE);
        } else{
            addFriendBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentState.equals("new")){
                        sendFriendRequest();
                    } else if(currentState.equals("request_sent")){
                        cancelFriendRequest();
                    } else if(currentState.equals("request_received")){
                        acceptFriendRequest();
                    } else if(currentState.equals("friends")){

                    }
                }
            });
        }
    }

    private void acceptFriendRequest() {
        contactRef.child(currentUid).child(receiverUserId).child("contact").setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    contactRef.child(receiverUserId).child(currentUid).child("contact").setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                friendRequestRef.child(currentUid).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            friendRequestRef.child(receiverUserId).child(currentUid).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    currentState = "friends";
                                                    addFriendBtn.setText("delete Friend");

                                                    cancelRequestBtn.setVisibility(View.GONE);
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void cancelFriendRequest() {
        friendRequestRef.child(currentUid).child(receiverUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    friendRequestRef.child(receiverUserId).child(currentUid).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            currentState = "new";
                            addFriendBtn.setText("Add Friend");
                        }
                    });
                }
            }
        });
    }

    private void sendFriendRequest() {
        friendRequestRef.child(currentUid).child(receiverUserId).child("request_type").setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    friendRequestRef.child(receiverUserId).child(currentUid).child("request_type").setValue("received").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                currentState = "request_sent";
                                addFriendBtn.setText("Cancel request");
                                Toast.makeText(ProfileActivity.this, "Request sent", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }
}
