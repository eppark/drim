package com.example.drim.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.drim.helpers.PhotoFromContact;
import com.example.drim.models.Contact;
import com.example.drim.R;
import com.example.drim.helpers.RoundedCornersTransformation;

import java.util.ArrayList;
import java.util.List;

public class ContactAdapter extends ArrayAdapter<Contact> {
    private List<Contact> contactList, tempItems, suggestions;
    private int resourceId;
    public Context mContext;

    public ContactAdapter(@NonNull Context context, int resourceId, ArrayList<Contact> items) {
        super(context, resourceId, items);
        this.contactList = items;
        this.mContext = context;
        this.resourceId = resourceId;
        tempItems = new ArrayList<>(items);
        suggestions = new ArrayList<>();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        convertView = inflater.inflate(R.layout.view_contact, parent, false);
        ImageView ivPfp = (ImageView) convertView.findViewById(R.id.ivPfp);
        TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
        TextView tvNumber = (TextView) convertView.findViewById(R.id.tvNumber);

        // Populate data into the template view using the data object
        Contact contact = getItem(position);
        tvName.setText(contact.name);
        tvNumber.setText(contact.number);

        // Set the images if we have them
        if (contact.id != null) {
            Glide.with(mContext).load(new PhotoFromContact().retrieveContactPhoto(mContext, contact.id)).apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(mContext, 6, 6, "#FFFFFF", 6))).into(ivPfp);
        } else {
            ivPfp.setImageDrawable(mContext.getResources().getDrawable(R.drawable.logo));
        }
        return convertView;
    }

    @Nullable
    @Override
    public Contact getItem(int position) {
        return contactList.get(position);
    }

    @Override
    public int getCount() {
        return contactList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}

