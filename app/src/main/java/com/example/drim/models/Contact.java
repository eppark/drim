package com.example.drim.models;

import org.parceler.Parcel;

@Parcel
public class Contact {
    public String name;
    public String number;
    public String id;
    public String threadid;

    public Contact() {}

    public Contact(String name, String number, String id) {
        this.name = name;
        this.number = number;
        this.id = id;
    }
}
