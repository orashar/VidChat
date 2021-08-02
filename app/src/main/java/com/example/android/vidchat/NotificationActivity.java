package com.example.android.vidchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import static android.view.View.GONE;

public class NotificationActivity extends AppCompatActivity {

    RecyclerView notificationList;

    private DatabaseReference friendRequestRef, contactRef, userRef;
    private FirebaseAuth mAuth;
    private String currentUid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        mAuth = FirebaseAuth.getInstance();
        currentUid = mAuth.getCurrentUser().getUid();

        friendRequestRef = FirebaseDatabase.getInstance().getReference().child("firend_requests");
        contactRef = FirebaseDatabase.getInstance().getReference().child("contacts");
        userRef = FirebaseDatabase.getInstance().getReference().child("users");

        notificationList = findViewById(R.id.notification_rv);
        notificationList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Contact> options = new FirebaseRecyclerOptions.Builder<Contact>().setQuery(friendRequestRef.child(currentUid), Contact.class).build();

        FirebaseRecyclerAdapter<Contact, NotificationViewHolder> frAdapter = new FirebaseRecyclerAdapter<Contact, NotificationViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final NotificationViewHolder holder, int position, @NonNull Contact model) {
                holder.accept.setVisibility(View.VISIBLE);
                holder.cancel.setVisibility(View.VISIBLE);

                final String listUserId = getRef(position).getKey();
                final DatabaseReference requestTypeRef = getRef(position).child("request_type").getRef();
                requestTypeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            String type = dataSnapshot.getValue().toString();
                            if(type.equals("received")){
                                holder.card.setVisibility(View.VISIBLE);

                                userRef.child(listUserId).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.hasChild("userpic")){
                                            final String imageStr = dataSnapshot.child("userpic").getValue().toString();
                                            final String nameStr = dataSnapshot.child("uname").getValue().toString();
                                            Picasso.get().load(imageStr).placeholder(R.mipmap.ic_launcher).into(holder.profilePic);
                                            holder.userName.setText(nameStr);
                                        }
                                        final String nameStr = dataSnapshot.child("uname").getValue().toString();
                                        holder.userName.setText(nameStr);

                                        holder.accept.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                acceptFriendRequest(listUserId);
                                            }
                                        });
                                        holder.cancel.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                cancelFriendRequest(listUserId);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            } else if(type.equals("sent")){
                                holder.card.setVisibility(GONE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.find_people_item, parent, false);
                NotificationViewHolder vh = new NotificationViewHolder(view);
                return vh;
            }
        };

        notificationList.setAdapter(frAdapter);
        frAdapter.startListening();
    }

    private void acceptFriendRequest(final String listUserId) {
        contactRef.child(currentUid).child(listUserId).child("contact").setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    contactRef.child(listUserId).child(currentUid).child("contact").setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                friendRequestRef.child(currentUid).child(listUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            friendRequestRef.child(listUserId).child(currentUid).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()) {
                                                        Toast.makeText(NotificationActivity.this, "New Connection Made", Toast.LENGTH_SHORT).show();
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
            }
        });
    }

    private void cancelFriendRequest(final String listUserId) {
        friendRequestRef.child(currentUid).child(listUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    friendRequestRef.child(listUserId).child(currentUid).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(NotificationActivity.this, "Request canceled", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {

        TextView userName;
        Button accept, cancel;
        ImageView profilePic;
        RelativeLayout card;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.people_name);
            profilePic = itemView.findViewById(R.id.people_pic);
            accept = itemView.findViewById(R.id.accept_request);
            cancel = itemView.findViewById(R.id.cancel_request);
            card = itemView.findViewById(R.id.people_card);
        }
    }
}
