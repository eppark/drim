package com.example.drim.receivers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import androidx.core.app.NotificationCompat;

import com.example.drim.R;
import com.example.drim.activities.MainActivity;
import com.example.drim.helpers.PhotoFromContact;
import com.example.drim.models.Contact;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

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

                if (isDefaultSmsApp(context)) {
                    // Write this message to our content provider
                    deliverSmsMessages(context, msgs);
                }
                // Now we show the notification
                showNotification(context, contact, strMessage);
            }
        }
    }

    private void showNotification(Context context, Contact contact, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("music", false);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
               intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentText(message);
        mBuilder.setContentIntent(contentIntent);
        mBuilder.setDefaults(Notification.DEFAULT_SOUND);
        mBuilder.setAutoCancel(true);
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Depending on the build version, we might need to add a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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

    private void deliverSmsMessages(Context context, SmsMessage[] messages) {
        final ContentValues messageValues = parseReceivedSmsMessage(messages);
        final long nowInMillis = System.currentTimeMillis();
        final long receivedTimestampMs = getMessageDate(messages[0], nowInMillis);
        messageValues.put(Telephony.Sms.Inbox.DATE, receivedTimestampMs);
        // Default to unread and unseen for us but ReceiveSmsMessageAction will override
        // seen for the telephony db.
        messageValues.put(Telephony.Sms.Inbox.READ, 0);
        messageValues.put(Telephony.Sms.Inbox.SEEN, 0);
        context.getContentResolver().insert(Uri.parse("content://sms/inbox"), messageValues);
    }

    private ContentValues parseReceivedSmsMessage(final SmsMessage[] msgs) {
        final SmsMessage sms = msgs[0];
        final ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, sms.getDisplayOriginatingAddress());
        values.put(Telephony.Sms.BODY, buildMessageBodyFromPdus(msgs));
        values.put(Telephony.Sms.DATE_SENT, sms.getTimestampMillis());
        values.put(Telephony.Sms.PROTOCOL, sms.getProtocolIdentifier());
        if (sms.getPseudoSubject().length() > 0) {
            values.put(Telephony.Sms.SUBJECT, sms.getPseudoSubject());
        }
        values.put(Telephony.Sms.REPLY_PATH_PRESENT, sms.isReplyPathPresent() ? 1 : 0);
        values.put(Telephony.Sms.SERVICE_CENTER, sms.getServiceCenterAddress());
        return values;
    }

    // Parse the message body from message PDUs
    private static String buildMessageBodyFromPdus(final SmsMessage[] msgs) {
        if (msgs.length == 1) {
            // There is only one part, so grab the body directly.
            return replaceFormFeeds(msgs[0].getDisplayMessageBody());
        } else {
            // Build up the body from the parts.
            final StringBuilder body = new StringBuilder();
            for (final SmsMessage msg : msgs) {
                try {
                    // getDisplayMessageBody() can NPE if mWrappedMessage inside is null.
                    body.append(msg.getDisplayMessageBody());
                } catch (final NullPointerException e) {
                    // Nothing to do
                }
            }
            return replaceFormFeeds(body.toString());
        }
    }

    // Some providers send formfeeds in their messages. Convert those formfeeds to newlines.
    private static String replaceFormFeeds(final String s) {
        return s == null ? "" : s.replace('\f', '\n');
    }

    // Parse the message date
    private Long getMessageDate(final SmsMessage sms, long now) {
        // Use now for the timestamp to avoid confusion with clock
        // drift between the handset and the SMSC.
        // Check to make sure the system is giving us a non-bogus time.
        final Calendar buildDate = new GregorianCalendar(2011, 8, 18);    // 18 Sep 2011
        final Calendar nowDate = new GregorianCalendar();
        nowDate.setTimeInMillis(now);
        if (nowDate.before(buildDate)) {
            // It looks like our system clock isn't set yet because the current time right now
            // is before an arbitrary time we made this build. Instead of inserting a bogus
            // receive time in this case, use the timestamp of when the message was sent.
            now = sms.getTimestampMillis();
        }
        return now;
    }
}
