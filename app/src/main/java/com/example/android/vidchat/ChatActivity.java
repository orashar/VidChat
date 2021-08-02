package com.example.android.vidchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class ChatActivity extends AppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener {

    private static String apikey = "46648542",
            sessionid = "2_MX40NjY0ODU0Mn5-MTU4NjA3OTY2MzA2Mn5LampvcXhSVXZaZFNEaHhlNE0yU2VjaWN-fg",
            token = "T1==cGFydG5lcl9pZD00NjY0ODU0MiZzaWc9MTFiZjFmNDA4MTYxMDVjYTIyYTQ2NDFjMzkxYjNhYWFhZmNhMTE1MTpzZXNzaW9uX2lkPTJfTVg0ME5qWTBPRFUwTW41LU1UVTROakEzT1RZMk16QTJNbjVMYW1wdmNYaFNWWFphWkZORWFIaGxORTB5VTJWamFXTi1mZyZjcmVhdGVfdGltZT0xNTg2MDc5NzUyJm5vbmNlPTAuNTI3NjQ1MTA0MjcwOTk1NSZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNTg4NjcxNzUxJmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9",
            LOGTAG = "ChatActivity_VidChat";
    private static final int VIDEO_PERM = 124;
    private Button closeVideoBtn;

    private DatabaseReference userRef;
    private String userid = "";

    private FrameLayout senderContainer, receiverCntainer;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        userRef = FirebaseDatabase.getInstance().getReference().child("users");
        userid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        closeVideoBtn = findViewById(R.id.close_video);
        closeVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.child(userid).hasChild("ringing")) {
                            userRef.child(userid).child("ringing").removeValue();

                            if(mPublisher != null) mPublisher.destroy();
                            if(mSubscriber != null) mSubscriber.destroy();

                            startActivity(new Intent(ChatActivity.this, RegisterActivity.class));
                            finish();
                        }
                        if(dataSnapshot.child(userid).hasChild("calling")) {
                            userRef.child(userid).child("calling").removeValue();

                            if(mPublisher != null) mPublisher.destroy();
                            if(mSubscriber != null) mSubscriber.destroy();

                            startActivity(new Intent(ChatActivity.this, RegisterActivity.class));
                            finish();
                        } else{
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, ChatActivity.this);

    }

    @AfterPermissionGranted(VIDEO_PERM)
    private void requestPermissions(){
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
        if(EasyPermissions.hasPermissions(this, perms)){
            senderContainer = findViewById(R.id.container_sender);
            receiverCntainer = findViewById(R.id.container_receiver);

            // init and connect to session
            mSession = new com.opentok.android.Session.Builder(this, apikey, sessionid).build();
            mSession.setSessionListener(ChatActivity.this);

            mSession.connect(token);

        } else{
            EasyPermissions.requestPermissions(this, "Permission are required to make a call!", VIDEO_PERM, perms);
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    // publish stream to session
    @Override
    public void onConnected(Session session) {
        Log.i(LOGTAG, "session connected");

        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(this);

        senderContainer.addView(mPublisher.getView());
        /*if(mPublisher.getView() instanceof GLSurfaceView){
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }*/

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOGTAG, "stream disconnected");
    }

    // receiving stream on receiver
    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOGTAG, "stream received");

        if(mSubscriber == null){
            mSubscriber = new Subscriber.Builder(this, stream).build();
            mSession.subscribe(mSubscriber);
            receiverCntainer.addView(mSubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOGTAG, "stream dropped");

        if(mSubscriber != null){
            mSubscriber = null;
            receiverCntainer.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.i(LOGTAG, "stream error");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        Log.i(LOGTAG, "stream pointer changed");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSession.disconnect();
    }
}
