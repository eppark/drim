package com.example.drim.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.view.View;
import android.widget.Toast;

import com.example.drim.helpers.EndlessRecyclerViewScrollListener;
import com.example.drim.R;
import com.example.drim.adapters.MessageAdapter;
import com.example.drim.databinding.ActivityMainBinding;
import com.example.drim.models.Contact;
import com.example.drim.models.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public ActivityMainBinding binding;
    public Context mContext;
    public Cursor people;
    public Cursor phones;

    public ArrayList<Map<String, String>> contactList;
    private EndlessRecyclerViewScrollListener scrollListener;

    // Sound player
    MediaPlayer mp = null;

    public static int LIMIT = 50;

    // Getting the inbox
    private ArrayList<Message> messageList;
    private ArrayList<String> threadids;
    private MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        // layout of activity is stored in a special property called root
        View view = binding.getRoot();
        setContentView(view);

        mContext = this;

        // Play sound at start
        if (getIntent() == null || getIntent().getExtras() == null || !getIntent().hasExtra("music")) {
            mp = MediaPlayer.create(getApplicationContext(), R.raw.startup);
            mp.start();
        }

        // Request perms
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.RECEIVE_MMS, Manifest.permission.WAKE_LOCK},
                1);

        // Ask to be the default SMS app
        //openSMSappChooser();

        // Populate the contacts list
        contactList = new ArrayList<>();
        populateContacts();

        // Get messages
        threadids = new ArrayList<>();
        messageList = new ArrayList<>();
        adapter = new MessageAdapter(this, messageList);
        binding.rvMessages.setAdapter(adapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        binding.rvMessages.setLayoutManager(linearLayoutManager);
        initialQuery();

        // Set the refresher
        // Setup refresh listener which triggers new data loading
        binding.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initialQuery();
                binding.swipeContainer.setRefreshing(false);
            }
        });
        // Configure the refreshing colors
        binding.swipeContainer.setColorSchemeResources(R.color.colorBlue);

        // Retain an instance for fresh searches
        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                fetchInboxSms(page);
            }
        };
        // Adds the scroll listener to RecyclerView
        binding.rvMessages.addOnScrollListener(scrollListener);

        // When the user scrolls, hide the compose button
        binding.rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && binding.floatingActionButton.getVisibility() == View.VISIBLE) {
                    binding.floatingActionButton.hide();
                } else if (dy < 0 && binding.floatingActionButton.getVisibility() != View.VISIBLE) {
                    binding.floatingActionButton.show();
                }
            }
        });

        // Create the compose button click event
        binding.floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start the activity
                Intent intent = new Intent(mContext, TextingActivity.class);
                intent.putExtra("contacts", contactList);
                startActivity(intent);
            }
        });
    }

    private void initialQuery() {
        threadids.clear();
        adapter.clear();
        messageList.clear();
        fetchInboxSms(0);
    }

    private void fetchInboxSms(int page) {
        ArrayList<Message> smsInbox = new ArrayList<>();
        String sorting = "date DESC LIMIT " + LIMIT + " OFFSET " + (LIMIT * page);
        Cursor c = this.getContentResolver().query(Telephony.Sms.Conversations.CONTENT_URI,
                null, null, null, sorting);
        if (c != null) {
            c.moveToLast();
            if (c.getCount() > 0) {
                do {
                    Message message = new Message();
                    message.contact = new Contact();
                    message.contact.threadid = c.getString(c.getColumnIndex("thread_id"));
                    if (!threadids.contains(message.contact.threadid)) {
                        threadids.add(message.contact.threadid);
                        message.content = c.getString(c.getColumnIndex("snippet"));

                        // Now check for the rest of the info
                        Uri uriSms = Uri.parse("content://sms");
                        Cursor cursor = this.getContentResolver()
                                .query(uriSms, new String[]{"_id", "address", "date", "body", "type", "read", "thread_id"}, "thread_id=" + message.contact.threadid, null,
                                        "date DESC LIMIT 1");
                        if (cursor != null) {
                            cursor.moveToLast();
                            if (cursor.getCount() > 0) {
                                do {
                                    message.contact.number = cursor.getString(cursor.getColumnIndex("address")).replaceAll("[^0-9+]", "");
                                    for (Map<String, String> map : contactList) {
                                        if (map.get("Phone").equals(message.contact.number) || map.get("Phone").contains(message.contact.number) || message.contact.number.contains(map.get("Phone"))) {
                                            message.contact.name = map.get("Name");
                                            message.contact.id = map.get("ID");
                                            map.put("ThreadID", message.contact.threadid);
                                            break;
                                        }
                                    }
                                    if (message.contact.name == null) {
                                        message.contact.name = "Unknown";
                                    }
                                    message.timestamp = cursor.getString(cursor.getColumnIndex("date"));
                                    message.read = cursor.getString(cursor.getColumnIndex("read"));
                                    message.type = cursor.getString(cursor.getColumnIndex("type"));
                                    smsInbox.add(0, message);
                                } while (cursor.moveToPrevious());
                            }
                        }
                    }
                } while (c.moveToPrevious());
            }
        }
        messageList.addAll(smsInbox);
        if (messageList.size() == 0) {
            Toast.makeText(mContext, "Inbox is empty.", Toast.LENGTH_SHORT).show();
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initialQuery();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Some permissions were denied. App may not work properly.", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    // Populate the contacts search list
    private void populateContacts() {
        contactList.clear();

        people = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        while (people.moveToNext()) {
            String contactName = people.getString(people.getColumnIndex(
                    ContactsContract.Contacts.DISPLAY_NAME));
            String contactId = people.getString(people.getColumnIndex(
                    ContactsContract.Contacts._ID));
            String hasPhone = people.getString(people.getColumnIndex(
                    ContactsContract.Contacts.HAS_PHONE_NUMBER));

            if ((Integer.parseInt(hasPhone) > 0)) {
                // If they have a phone number
                phones = getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                        null, null);
                while (phones.moveToNext()) {
                    // Store numbers and display a dialog letting the user select which contact
                    String phoneNumber = phones.getString(
                            phones.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER));

                    String numberType = phones.getString(phones.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.TYPE));

                    Map<String, String> NamePhoneType = new HashMap<String, String>();

                    NamePhoneType.put("Name", contactName);
                    NamePhoneType.put("Phone", phoneNumber.replaceAll("[^0-9+]", ""));

                    if (numberType.equals("0")) {
                        NamePhoneType.put("Type", "Work");
                    } else if (numberType.equals("1")) {
                        NamePhoneType.put("Type", "Home");
                    } else if (numberType.equals("2")) {
                        NamePhoneType.put("Type", "Mobile");
                    } else {
                        NamePhoneType.put("Type", "Other");
                    }

                    NamePhoneType.put("ID", contactId);
                    // Then add this map to the list.
                    contactList.add(NamePhoneType);
                }
            }
        }
        startManagingCursor(people);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        phones.close();
        people.close();
        if (mp != null || mp.isPlaying()) {
            mp.stop();
            mp.release();
        }
        mp = null;
    }

    // Ask to be the default SMS app
    private void openSMSappChooser() {
        if (!isDefaultSmsApp()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = getSystemService(RoleManager.class);
                if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    } else {
                        Intent roleRequestIntent = roleManager.createRequestRoleIntent(
                                RoleManager.ROLE_SMS);
                        startActivityForResult(roleRequestIntent, 0);
                    }
                }
            } else {
                Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                startActivityForResult(intent, 0);
            }
        }
    }

    // Check if SMS App is default
    private boolean isDefaultSmsApp() {
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }

}