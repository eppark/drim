package com.example.drim.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class HeadlessSmsSendService extends Service {
    public HeadlessSmsSendService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        return super.onStartCommand(intent,flags,startID);
    }
}
