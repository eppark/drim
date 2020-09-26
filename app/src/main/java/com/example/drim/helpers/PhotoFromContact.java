package com.example.drim.helpers;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;

import com.example.drim.R;

import java.io.IOException;
import java.io.InputStream;

public class PhotoFromContact {
    public static Bitmap retrieveContactPhoto(Context context, String contactId) {
        Bitmap photo = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.logo);

        try {
            if(contactId != null) {
                InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(contactId)));

                if (inputStream != null) {
                    photo = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return photo;
    }
}
