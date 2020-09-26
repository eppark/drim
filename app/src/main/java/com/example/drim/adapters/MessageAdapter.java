package com.example.drim.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.drim.models.Contact;
import com.example.drim.models.Message;
import com.example.drim.helpers.PhotoFromContact;
import com.example.drim.R;
import com.example.drim.helpers.RoundedCornersTransformation;
import com.example.drim.activities.TextingActivity;

import org.parceler.Parcels;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder>{
    private List<Message> messageList;
    public Context mContext;


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public ImageView ivPfp;
        public TextView tvName;
        public TextView tvDate;
        public TextView tvLastMessage;

        public ViewHolder(final View itemView) {
            super(itemView);

            ivPfp = (ImageView) itemView.findViewById(R.id.ivPfp);
            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvDate = (TextView) itemView.findViewById(R.id.tvDate);
            tvLastMessage = (TextView) itemView.findViewById(R.id.tvLastMessage);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition(); // Gets the item position

            // Make sure the position is valid
            if (position != RecyclerView.NO_POSITION) {
                Message message = messageList.get(position);

                // Create an intent for the new activity
                Intent intent = new Intent(mContext, TextingActivity.class);
                intent.putExtra(Contact.class.getSimpleName(), Parcels.wrap(message.contact));

                // Show the activity
                mContext.startActivity(intent);
            }
        }
    }

    public MessageAdapter(Context context, ArrayList<Message> aMessages) {
        messageList = aMessages;
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View messageView = inflater.inflate(R.layout.item_messages, parent, false);

        // Return a new holder instance
        return new MessageAdapter.ViewHolder(messageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the data model based on position
        Message message = messageList.get(position);

        // Populate data into the template view using the data object
        holder.tvName.setText(message.contact.name);
        holder.tvLastMessage.setText(message.content);

        // Format the date
        String year = new SimpleDateFormat("yyyy").format(Long.parseLong(message.timestamp));
        String formattedDate = new SimpleDateFormat("MM/dd/yyyy").format(Long.parseLong(message.timestamp));
        if (Long.parseLong(year) == Calendar.getInstance().get(Calendar.YEAR)) {
            String today = new SimpleDateFormat("MM/dd/yyyy").format(Calendar.getInstance().getTime());
            if (formattedDate.equals(today)) {
                formattedDate = new SimpleDateFormat("hh:mm aa").format(Long.parseLong(message.timestamp));
            } else {
                formattedDate = new SimpleDateFormat("MMM d").format(Long.parseLong(message.timestamp));
            }
        }
        holder.tvDate.setText(formattedDate);

        // Set the images if we have them
        if (message.contact.id != null) {
            Glide.with(mContext).load(new PhotoFromContact().retrieveContactPhoto(mContext, message.contact.id)).apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(mContext, 6, 6, "#FFFFFF", 6))).into(holder.ivPfp);
        } else {
            Glide.with(mContext).load(mContext.getResources().getDrawable(R.drawable.logo)).apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(mContext, 6, 6, "#FFFFFF", 6))).into(holder.ivPfp);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // Clean all elements of the recycler
    public void clear() {
        messageList.clear();
        notifyDataSetChanged();
    }

    // Add a list of items
    public void addAll(List<Message> list) {
        messageList.addAll(list);
        notifyDataSetChanged();
    }
}
