package com.example.android.vidchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

public class FindPeopleActivity extends AppCompatActivity {

    RecyclerView peoplerv;
    EditText searchet;

    private String query = "";

    private DatabaseReference userDbRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_people);

        userDbRef = FirebaseDatabase.getInstance().getReference().child("users");

        searchet = findViewById(R.id.search_people);
        peoplerv = findViewById(R.id.people_rv);

        peoplerv.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        searchet.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(searchet.getText().toString().isEmpty()){

                } else{
                    query = s.toString();
                    onStart();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Contact> options = null;
        if(query.isEmpty()){
            options = new FirebaseRecyclerOptions.Builder<Contact>().setQuery(userDbRef, Contact.class).build();
        } else{
            options = new FirebaseRecyclerOptions.Builder<Contact>().setQuery(userDbRef.orderByChild("uname").startAt(query).endAt(query + "\uf8ff"), Contact.class).build();
        }
        FirebaseRecyclerAdapter<Contact, FindPeopleViewHolder> frAdapter = new FirebaseRecyclerAdapter<Contact, FindPeopleViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull FindPeopleViewHolder holder, int position, @NonNull final Contact model) {
                holder.userName.setText(model.getUname());
                Picasso.get().load(model.getUserpic()).placeholder(R.mipmap.ic_launcher).into(holder.profilePic);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                        intent.putExtra("visit_userid", model.getUid());
                        intent.putExtra("profile_name", model.getUname());
                        intent.putExtra("profile_image", model.getUserpic());
                        startActivity(intent);
                    }
                });
            }

            @NonNull
            @Override
            public FindPeopleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
                FindPeopleViewHolder vh = new FindPeopleViewHolder(view);
                return vh;
            }
        };

        peoplerv.setAdapter(frAdapter);
        frAdapter.startListening();
    }

    public static class FindPeopleViewHolder extends RecyclerView.ViewHolder {

        TextView userName;
        Button callBtn;
        ImageView profilePic;
        RelativeLayout card;

        public FindPeopleViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.contact_name);
            profilePic = itemView.findViewById(R.id.contact_pic);
            callBtn = itemView.findViewById(R.id.call_btn);
            card = itemView.findViewById(R.id.contact_card);

            callBtn.setVisibility(View.GONE);
        }
    }

}
