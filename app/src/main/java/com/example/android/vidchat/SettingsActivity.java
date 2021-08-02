package com.example.android.vidchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    private Button saveBtn;
    private EditText usernameet, userbioet;
    private ImageView profileiv;

    private Uri imageUri;

    private StorageReference userProfileImageRef;

    private String downloadUrl;

    private DatabaseReference userDbRef;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        saveBtn = findViewById(R.id.save_settings);
        userbioet = findViewById(R.id.bio_et);
        usernameet = findViewById(R.id.name_et);
        profileiv = findViewById(R.id.profile_iv);

        userProfileImageRef = FirebaseStorage.getInstance().getReference().child("ProfileImages");
        userDbRef = FirebaseDatabase.getInstance().getReference().child("users");

        progressDialog = new ProgressDialog(this);

        retrieveUserInfo();

        profileiv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select image"), 1);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserData();
            }
        });
    }

    private void saveUserData() {
        final String username = usernameet.getText().toString();
        final String userbio = userbioet.getText().toString();

        if(imageUri == null){
            userDbRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).hasChild("userpic")){
                        saveUserDataWithoutImage();
                    } else{
                        Toast.makeText(SettingsActivity.this, "Using an image helps other user to identify you.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        } else if(username.isEmpty()){
            Toast.makeText(this, "Username Can not be empty", Toast.LENGTH_SHORT).show();
        } else if(userbio.isEmpty()){
            Toast.makeText(this, "Please tell something about yourself", Toast.LENGTH_SHORT).show();
        } else{

            progressDialog.setTitle("Account Information");
            progressDialog.setMessage("Processing...");
            progressDialog.show();

            final StorageReference filepath = userProfileImageRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            final UploadTask uploadTask = filepath.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if(!task.isSuccessful()){
                        throw task.getException();
                    }
                    downloadUrl = filepath.getDownloadUrl().toString();
                    return filepath.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){
                        downloadUrl = task.getResult().toString();

                        HashMap<String, Object> profileMap = new HashMap<>();
                        profileMap.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        profileMap.put("uname", username);
                        profileMap.put("ubio", userbio);
                        profileMap.put("userpic", downloadUrl);

                        userDbRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    startActivity(new Intent(SettingsActivity.this, ContactsActivity.class));
                                    finish();
                                    progressDialog.dismiss();

                                    Toast.makeText(SettingsActivity.this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                                } else{
                                    progressDialog.dismiss();
                                    Toast.makeText(SettingsActivity.this, "Error updating profile!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void saveUserDataWithoutImage() {
        final String username = usernameet.getText().toString();
        final String userbio = userbioet.getText().toString();
        if(username.isEmpty()){
            Toast.makeText(this, "Username Can not be empty", Toast.LENGTH_SHORT).show();
        } else if(userbio.isEmpty()){
            Toast.makeText(this, "Please tell something about yourself", Toast.LENGTH_SHORT).show();
        } else{

            progressDialog.setTitle("Account Information");
            progressDialog.setMessage("Processing...");
            progressDialog.show();


            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            profileMap.put("uname", username);
            profileMap.put("ubio", userbio);

            userDbRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        startActivity(new Intent(SettingsActivity.this, ContactsActivity.class));
                        finish();
                        progressDialog.dismiss();

                        Toast.makeText(getApplicationContext(), "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                    } else{
                        progressDialog.dismiss();
                        Toast.makeText(SettingsActivity.this, "Error updating profile!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }

    }

    private void retrieveUserInfo(){
        userDbRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String imageDb = dataSnapshot.child("userpic").getValue().toString();
                    String unameDb = dataSnapshot.child("uname").getValue().toString();
                    String ubioDb = dataSnapshot.child("ubio").getValue().toString();

                    usernameet.setText(unameDb);
                    userbioet.setText(ubioDb);
                    Picasso.get().load(imageDb).placeholder(R.mipmap.ic_launcher).into(profileiv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == 1){
                imageUri = data.getData();
                if(imageUri != null)
                    profileiv.setImageURI(imageUri);
            }
        }
    }
}
