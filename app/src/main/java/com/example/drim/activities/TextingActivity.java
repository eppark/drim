package com.example.drim.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.drim.helpers.PhotoFromContact;
import com.example.drim.R;
import com.example.drim.helpers.RoundedCornersTransformation;
import com.example.drim.adapters.TextAdapter;
import com.example.drim.databinding.ActivityTextingBinding;
import com.example.drim.models.Contact;
import com.example.drim.models.Text;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Map;

public class TextingActivity extends AppCompatActivity {
    private ActivityTextingBinding binding;
    private View view;
    public Context mContext;

    // Store texts
    private ArrayList<Text> texts;
    private TextAdapter textAdapter;

    // Store contacts values in these arraylist
    private ArrayList<Map<String, String>> contactList;
    private SimpleAdapter adapter;

    // Selected contact
    private Contact currentContact;

    // Keep track of initial load to scroll to bottom of the view
    boolean mFirstLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set ViewBinding
        binding = ActivityTextingBinding.inflate(getLayoutInflater());

        // layout of activity is stored in a special property called root
        view = binding.getRoot();
        setContentView(view);
        mContext = this;

        // Keep track of texts
        texts = new ArrayList<>();
        textAdapter = new TextAdapter(mContext, texts);
        binding.rvTexts.setAdapter(textAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        binding.rvTexts.setLayoutManager(linearLayoutManager);

        // Set the information
        if (getIntent().getExtras() != null) {
            Contact contact = Parcels.unwrap(getIntent().getParcelableExtra(Contact.class.getSimpleName()));
            if (contact != null) {
                setNames(contact);
            } else {
                hideNames();
            }
        } else {
            hideNames();
        }

        binding.btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String textMessage = binding.etSend.getText().toString();
                if (currentContact == null) {
                    Toast.makeText(mContext, "Choose a recipient first.", Toast.LENGTH_SHORT).show();
                } else if (textMessage.length() == 0) {
                    Toast.makeText(mContext, "Text can't be empty!", Toast.LENGTH_SHORT).show();
                } else {
                    String phoneNum = null;
                    if (currentContact.number != null) {
                        phoneNum = currentContact.number;
                    } else if (currentContact.name != null) {
                        phoneNum = currentContact.name;
                    }

                    // Don't let the user click send too many times
                    binding.btnSend.setEnabled(false);

                    if (!isDefaultSmsApp()) {
                        // Set the intents to check if it works
                        // set pendingIntent for sent & delivered
                        PendingIntent sentIntent = PendingIntent.getBroadcast(mContext, 100, new
                                Intent("SENT"), 0);

                        PendingIntent deliveryIntent = PendingIntent.getBroadcast(mContext, 200, new
                                Intent("DELIVERED"), 0);

                        registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                int resultCode = getResultCode();
                                switch (resultCode) {
                                    case TextingActivity.RESULT_OK:
                                        // If the message was sent, we can clear everything
                                        binding.etSend.getText().clear();
                                        binding.btnSend.setEnabled(true);
                                        break;
                                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                        binding.btnSend.setEnabled(true);
                                        Toast.makeText(mContext, "Could not send message.", Toast.LENGTH_SHORT).show();
                                        break;
                                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                                        binding.btnSend.setEnabled(true);
                                        Toast.makeText(mContext, "Could not send message: no service.", Toast.LENGTH_SHORT).show();
                                        break;
                                    case SmsManager.RESULT_ERROR_NULL_PDU:
                                        binding.btnSend.setEnabled(true);
                                        Toast.makeText(mContext, "Could not send message: null PDU.", Toast.LENGTH_SHORT).show();
                                        break;
                                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                                        binding.btnSend.setEnabled(true);
                                        Toast.makeText(mContext, "Could not send message: radio off", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            }
                        }, new IntentFilter("SENT"));

                        // Send the message
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(phoneNum, null, textMessage, sentIntent, null);
                    } else {
                        // Manually write the message to the provider
                        ContentValues values = new ContentValues();
                        values.put(Telephony.Sms.ADDRESS, currentContact.number);
                        values.put(Telephony.Sms.BODY, textMessage);
                        getContentResolver().insert(Uri.parse("content://sms/sent"), values);
                        binding.btnSend.setEnabled(true);
                        binding.etSend.getText().clear();
                    }
                }
            }
        });
    }

    // Show contact info
    private void setNames(Contact contact) {
        mFirstLoad = true;
        currentContact = contact;
        binding.tvName.setVisibility(View.VISIBLE);
        binding.tvNumber.setVisibility(View.VISIBLE);
        binding.etName.setVisibility(View.GONE);
        binding.tvName.setText(contact.name);
        // Set number if we have it
        if (contact.number != null) {
            binding.tvNumber.setText(contact.number);
        } else {
            binding.tvNumber.setText("");
        }
        // Set the image
        Glide.with(this).load(new PhotoFromContact().retrieveContactPhoto(mContext, contact.id)).apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(this, 6, 6, "#FFFFFF", 2))).into(binding.ivPfp);

        populateTexts();
        myHandler.postDelayed(mRefreshMessagesRunnable, POLL_INTERVAL);

        markMessagesRead();
    }

    // Populate the texts
    private void populateTexts() {
        texts.clear();

        if (currentContact.threadid != null) {
            Uri uriSms = Uri.parse("content://sms");
            Cursor cursor = this.getContentResolver()
                    .query(uriSms, new String[]{"_id", "address", "date", "body", "type", "read", "thread_id"}, "thread_id=" + currentContact.threadid, null,
                            "date COLLATE LOCALIZED ASC");
            if (cursor != null) {
                cursor.moveToLast();
                if (cursor.getCount() > 0) {
                    do {
                        Text text = new Text();
                        if (cursor.getString(cursor.getColumnIndex("type")).equals("1")) {
                            text.from = currentContact;
                        } else {
                            text.from = null;
                        }
                        text.contentType = cursor.getString(cursor.getColumnIndex("type"));
                        text.stringContent = cursor.getString(cursor.getColumnIndex("body"));
                        text.timestamp = cursor.getString(cursor.getColumnIndex("date"));
                        texts.add(text);
                    } while (cursor.moveToPrevious());
                }
            }
            textAdapter.notifyDataSetChanged();
            // Scroll to the bottom of the list on initial load
            if (mFirstLoad) {
                binding.rvTexts.scrollToPosition(0);
                mFirstLoad = false;
            }
        } else if (currentContact.number != null) {
            Uri uriSms = Uri.parse("content://sms");
            Cursor cursor = this.getContentResolver()
                    .query(uriSms, new String[]{"_id", "address", "date", "body", "type", "read", "thread_id"}, "address=" + currentContact.number, null,
                            "date COLLATE LOCALIZED ASC");
            if (cursor != null) {
                cursor.moveToLast();
                if (cursor.getCount() > 0) {
                    do {
                        Text text = new Text();
                        if (cursor.getString(cursor.getColumnIndex("type")).equals("1")) {
                            text.from = currentContact;
                        } else {
                            text.from = null;
                        }
                        text.contentType = cursor.getString(cursor.getColumnIndex("type"));
                        text.stringContent = cursor.getString(cursor.getColumnIndex("body"));
                        text.timestamp = cursor.getString(cursor.getColumnIndex("date"));
                        texts.add(text);
                    } while (cursor.moveToPrevious());
                }
            }
            textAdapter.notifyDataSetChanged();
            // Scroll to the bottom of the list on initial load
            if (mFirstLoad) {
                binding.rvTexts.scrollToPosition(0);
                mFirstLoad = false;
            }
        } else if (currentContact.name != null) {
            Uri uriSms = Uri.parse("content://sms");
            Cursor cursor = this.getContentResolver()
                    .query(uriSms, new String[]{"_id", "address", "date", "body", "type", "read", "thread_id"}, "address=" + currentContact.name, null,
                            "date COLLATE LOCALIZED ASC");
            if (cursor != null) {
                cursor.moveToLast();
                if (cursor.getCount() > 0) {
                    do {
                        Text text = new Text();
                        if (cursor.getString(cursor.getColumnIndex("type")).equals("1")) {
                            text.from = currentContact;
                        } else {
                            text.from = null;
                        }
                        text.contentType = cursor.getString(cursor.getColumnIndex("type"));
                        text.stringContent = cursor.getString(cursor.getColumnIndex("body"));
                        text.timestamp = cursor.getString(cursor.getColumnIndex("date"));
                        texts.add(text);
                    } while (cursor.moveToPrevious());
                }
            }
            textAdapter.notifyDataSetChanged();
            // Scroll to the bottom of the list on initial load
            if (mFirstLoad) {
                binding.rvTexts.scrollToPosition(0);
                mFirstLoad = false;
            }
        }
    }

    // Show edit box
    private void hideNames() {
        binding.tvName.setVisibility(View.GONE);
        binding.tvNumber.setVisibility(View.GONE);
        binding.etName.setVisibility(View.VISIBLE);
        Glide.with(this).load(this.getResources().getDrawable(R.drawable.logo)).apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(this, 6, 6, "#FFFFFF", 6))).into(binding.ivPfp);

        //Create adapter
        contactList = new ArrayList<>((ArrayList<Map<String, String>>) getIntent().getSerializableExtra("contacts"));

        adapter = new SimpleAdapter(this, contactList, R.layout.view_contact, new String[]{"Name", "Phone", "Type"}, new int[]{R.id.tvName, R.id.tvNumber, R.id.tvType});
        binding.etName.setAdapter(adapter);
        binding.etName.setThreshold(1);
        binding.etName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Map<String, String> chosen = (Map<String, String>) adapterView.getItemAtPosition(i);

                Contact selected = new Contact(chosen.get("Name"), chosen.get("Phone"), chosen.get("ID"));
                if (chosen.get("ThreadID") != null) {
                    selected.threadid = chosen.get("ThreadID");
                } else {
                    selected.threadid = null;
                }
                setNames(selected);
            }
        });

        binding.etName.setImeActionLabel("Go", KeyEvent.KEYCODE_ENTER);
        binding.etName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                boolean handled = false;
                if (event != null) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                        // Handle pressing "Enter" key here
                        String check = binding.etName.getText().toString().replaceAll("[^0-9+]", "");
                        Log.d("ok well please", check);

                        if (check.matches("[0-9]+") && (check.length() >= 10 && check.length() <= 12)) {
                            // Perform action on key press
                            Contact selected = new Contact();
                            selected.name = check;
                            selected.number = null;
                            selected.threadid = null;
                            handled = true;
                            setNames(selected);
                        } else {
                            Toast.makeText(mContext, "Please enter a valid number!", Toast.LENGTH_SHORT).show();
                            handled = false;
                        }
                    }
                }
                return handled;
            }
        });
    }

    // Create a handler which can run code periodically
    static final int POLL_INTERVAL = 1000; // milliseconds
    Handler myHandler = new android.os.Handler();
    Runnable mRefreshMessagesRunnable = new Runnable() {
        @Override
        public void run() {
            if (texts != null && textAdapter != null) {
                populateTexts();
                myHandler.postDelayed(this, POLL_INTERVAL);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myHandler.removeCallbacks(mRefreshMessagesRunnable);
    }

    // Mark messages as read
    private void markMessagesRead() {
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = getContentResolver().query(uri, null, "thread_id=" + currentContact.threadid + " AND read=0", null, null);
        if (cursor != null) {
            cursor.moveToLast();
            if (cursor.getCount() > 0) {
                do {
                    String SmsMessageId = cursor.getString(cursor.getColumnIndex("_id"));
                    ContentValues values = new ContentValues();
                    values.put("read", true);
                    getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
                } while (cursor.moveToPrevious());
            }
            cursor.close();
        }
    }

    // Check if SMS App is default
    private boolean isDefaultSmsApp() {
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }
}