package com.example.android.vidchat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ContactsActivity extends AppCompatActivity {

    BottomNavigationView navView;
    private RecyclerView mContactrv;
    private Button findPeopleBtn;


    private DatabaseReference contactRef, userRef;
    private FirebaseAuth mAuth;
    private String currentUid, uname = "", profilePic = "", calledBy = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        navView = findViewById(R.id.nav_view);

        navView.setOnNavigationItemSelectedListener(navListener);


        contactRef = FirebaseDatabase.getInstance().getReference().child("contacts");
        userRef = FirebaseDatabase.getInstance().getReference().child("users");
        mAuth = FirebaseAuth.getInstance();
        currentUid = mAuth.getCurrentUser().getUid();


        mContactrv = findViewById(R.id.contacts_rv);
        findPeopleBtn = findViewById(R.id.find_people);

        mContactrv.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        findPeopleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FindPeopleActivity.class);
                startActivity(intent);
            }
        });

    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            switch (menuItem.getItemId()){
                case R.id.navigation_home:
                    Intent mainIntent = new Intent(ContactsActivity.this, ContactsActivity.class);
                    startActivity(mainIntent);
                    break;
                case R.id.navigation_notifications:
                    Intent notificationIntent = new Intent(ContactsActivity.this, NotificationActivity.class);
                    startActivity(notificationIntent);
                    break;
                case R.id.navigation_settings:
                    Intent settingsIntent = new Intent(ContactsActivity.this, SettingsActivity.class);
                    startActivity(settingsIntent);
                    break;
                case R.id.navigation_logout:
                    FirebaseAuth.getInstance().signOut();
                    Intent logoutIntent = new Intent(ContactsActivity.this, RegisterActivity.class);
                    startActivity(logoutIntent);
                    finish();
                    break;
            }

            return true;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        validateUser();

        checkForReceivingCall();

        FirebaseRecyclerOptions<Contact> options = new FirebaseRecyclerOptions.Builder<Contact>().setQuery(contactRef.child(currentUid), Contact.class).build();

        FirebaseRecyclerAdapter<Contact, ContactsViewHolder> frAdapter = new FirebaseRecyclerAdapter<Contact, ContactsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ContactsViewHolder holder, int position, @NonNull Contact model) {

                final String listUserId = getRef(position).getKey();

                userRef.child(listUserId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            uname = dataSnapshot.child("uname").getValue().toString();
                            profilePic = dataSnapshot.child("userpic").getValue().toString();

                            holder.contactName.setText(uname);
                            Picasso.get().load(profilePic).placeholder(R.mipmap.ic_launcher).into(holder.contactPic);

                        }

                        holder.callBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(ContactsActivity.this, CallActivity.class);
                                intent.putExtra("visit_userid", listUserId);
                                startActivity(intent);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
                ContactsViewHolder vh = new ContactsViewHolder(view);
                return vh;
            }
        };

        mContactrv.setAdapter(frAdapter);
        frAdapter.startListening();

    }


    public static class ContactsViewHolder extends RecyclerView.ViewHolder {

        TextView contactName;
        Button callBtn;
        ImageView contactPic;

        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);

            contactName = itemView.findViewById(R.id.contact_name);
            contactPic = itemView.findViewById(R.id.contact_pic);
            callBtn = itemView.findViewById(R.id.call_btn);
        }
    }

    private void validateUser(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        reference.child("users").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    Intent intent = new Intent(ContactsActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void checkForReceivingCall() {
        userRef.child(currentUid).child("ringing").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("ringto")){
                    calledBy = dataSnapshot.child("ringto").getValue().toString();

                    Intent intent = new Intent(ContactsActivity.this, CallActivity.class);
                    intent.putExtra("visit_userid", calledBy);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}
