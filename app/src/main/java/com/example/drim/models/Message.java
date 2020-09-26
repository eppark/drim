package com.example.drim.models;

import org.parceler.Parcel;

@Parcel
public class Message {
    public Contact contact;
    public String content;
    public String timestamp;
    public String id;
    public String read;

    public Message() {}
}
