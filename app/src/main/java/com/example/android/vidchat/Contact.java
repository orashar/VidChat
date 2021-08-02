package com.example.android.vidchat;

public class Contact {

    String uid, uname, ubio, userpic;

    public Contact() {
    }

    public Contact(String uid, String uname, String ubio, String userpic) {
        this.uid = uid;
        this.uname = uname;
        this.ubio = ubio;
        this.userpic = userpic;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getUbio() {
        return ubio;
    }

    public void setUbio(String ubio) {
        this.ubio = ubio;
    }

    public String getUserpic() {
        return userpic;
    }

    public void setUserpic(String userpic) {
        this.userpic = userpic;
    }
}
