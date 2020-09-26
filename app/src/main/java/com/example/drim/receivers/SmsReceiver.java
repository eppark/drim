package com.example.drim.receivers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.drim.R;
import com.example.drim.activities.MainActivity;
import com.example.drim.activities.TextingActivity;
import com.example.drim.helpers.PhotoFromContact;
import com.example.drim.models.Contact;

import org.parceler.Parcels;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (isDefaultSmsApp(context)) {
            // Get the SMS message.
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs;
            String phoneNumber = "";
            String strMessage = "";
            String format = bundle.getString("format");
            // Retrieve the SMS message received.
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                // Fill the msgs array.
                msgs = new SmsMessage[pdus.length];
                for (int i = 0; i < msgs.length; i++) {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // Version Marshmallow and above
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                    } else {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    }
                    // Build the message to show.
                    phoneNumber = msgs[i].getOriginatingAddress();
                    strMessage += msgs[i].getMessageBody();

                    // Look up the contact, if possible
                    Contact contact = new Contact();
                    Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
                    Cursor c = context.getContentResolver().query(lookupUri, new String[]{ContactsContract.Data.DISPLAY_NAME, ContactsContract.Data.CONTACT_ID}, null, null, null);
                    try {
                        if (c != null) {
                            c.moveToFirst();
                            contact.name = c.getString(0);
                            contact.id = c.getString(1);
                            contact.number = phoneNumber;
                        }

                    } catch (Exception e) {
                        // If we don't find the contact, we can just set the contact name as the number
                        contact.name = phoneNumber;
                    } finally {
                        c.close();
                    }

                    // Find the thread ID
                    Uri uriSms = Uri.parse("content://sms");
                    Cursor cursor = context.getContentResolver()
                            .query(uriSms, new String[]{"address", "thread_id"}, "address=" + phoneNumber, null,
                                    "date DESC LIMIT 1");
                    try {
                        if (cursor != null) {
                            cursor.moveToFirst();
                            contact.threadid = c.getString(1);
                        }
                    } catch (Exception e) {
                        // If we don't find the thread, we can just set the thread to null
                        contact.threadid = null;
                    } finally {
                        cursor.close();
                    }

                    // Now we show the notification
                    showNotification(context, contact, strMessage);
                }
            }
        }
    }

    private void showNotification(Context context, Contact contact, String message) {
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        // If we have a thread, we can open the exact text message
        /*if (contact.threadid == null) {
            contentIntent = PendingIntent.getActivity(context, 0,
                    new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            Intent intent = new Intent(context, TextingActivity.class);
            intent.putExtra(Contact.class.getSimpleName(), Parcels.wrap(contact));
            contentIntent = PendingIntent.getActivity(context, 0,
                    new Intent(context, TextingActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        }*/

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setLargeIcon(new PhotoFromContact().retrieveContactPhoto(context, contact.id))
                        .setContentTitle(contact.name)
                        .setContentText(message);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setDefaults(Notification.DEFAULT_SOUND);
        mBuilder.setAutoCancel(true);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Depending on the build version, we might need to add a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = "1";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Notification Channel",
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }

        mNotificationManager.notify(1, mBuilder.build());
    }

    // Check if SMS App is default
    private boolean isDefaultSmsApp(Context context) {
        return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
    }
}
