package com.example.android.vidchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private EditText phoneet;
    private Button sendOtp;

    private boolean checker = false;

    private FirebaseAuth mAuth;
    private String verificationId;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);

        phoneet = findViewById(R.id.phone_et);
        sendOtp = findViewById(R.id.send_otp);


        sendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sendOtp.getText().equals("VERIFY OTP")) {
                    if (checker = true) {
                        String vcode = phoneet.getText().toString();
                        if(vcode.equals("")){
                            Toast.makeText(RegisterActivity.this, "Invalid Code", Toast.LENGTH_SHORT).show();
                        } else{
                            loadingBar.setTitle("OTP Verification");
                            loadingBar.setMessage("Please wait...");
                            loadingBar.setCanceledOnTouchOutside(true);
                            loadingBar.show();

                            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, vcode);
                            signInWithPhoneAuthCredential(credential);
                        }
                    }
                } else {
                    String phoneno = phoneet.getText().toString();
                    if (!phoneno.equals("")) {
                        loadingBar.setTitle("Phone Verification");
                        loadingBar.setMessage("Please wait...");
                        loadingBar.setCanceledOnTouchOutside(true);
                        loadingBar.show();

                        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneno, 60, TimeUnit.SECONDS, RegisterActivity.this, mCallbacks);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Invalid Phone no!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(RegisterActivity.this, "Cannot identify Phone Number!", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
                sendOtp.setText("SEND OTP");
                checker = false;
            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);

                verificationId = s;
                mResendToken = forceResendingToken;

                Toast.makeText(RegisterActivity.this, "Code sent successfully.", Toast.LENGTH_SHORT).show();
                loadingBar.dismiss();
                sendOtp.setText("VERIFY OTP");
                phoneet.setText("");
                phoneet.setHint("Enter Otp");
                checker = true;
            }
        };
    }


    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            loadingBar.dismiss();

                            Toast.makeText(RegisterActivity.this, "Successfully Signed in.", Toast.LENGTH_SHORT).show();
                            onVerificationCompleted();
                            FirebaseUser user = task.getResult().getUser();
                            // ...
                        } else {
                            loadingBar.dismiss();

                            String error = task.getException().getMessage();
                            Log.e("VidChatVerification", error);
                        }
                    }
                });
    }

    private void onVerificationCompleted(){
        Intent intent = new Intent(this, ContactsActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            onVerificationCompleted();
        }
    }
}