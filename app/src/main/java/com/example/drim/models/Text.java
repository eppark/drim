package com.example.drim.models;

import org.parceler.Parcel;

import java.io.File;

@Parcel
public class Text {
    public Contact from;
    public String contentType;
    public String stringContent;
    public File fileContent;
    public String timestamp;

    public Text() {}
}
