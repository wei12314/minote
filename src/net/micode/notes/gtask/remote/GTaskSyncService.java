/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.gtask.remote;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

// Definition of the GTaskSyncService class extending Service
public class GTaskSyncService extends Service {

    // Constants defining various action types and broadcast names
    public final static String ACTION_STRING_NAME = "sync_action_type";
    public final static int ACTION_START_SYNC = 0;
    public final static int ACTION_CANCEL_SYNC = 1;
    public final static int ACTION_INVALID = 2;
    public final static String GTASK_SERVICE_BROADCAST_NAME = "net.micode.notes.gtask.remote.gtask_sync_service";
    public final static String GTASK_SERVICE_BROADCAST_IS_SYNCING = "isSyncing";
    public final static String GTASK_SERVICE_BROADCAST_PROGRESS_MSG = "progressMsg";

    // Static variables for the synchronization task and progress message
    private static GTaskASyncTask mSyncTask = null;
    private static String mSyncProgress = "";

    // Method to start the synchronization process
    private void startSync() {
        if (mSyncTask == null) {
            mSyncTask = new GTaskASyncTask(this, new GTaskASyncTask.OnCompleteListener() {
                // Callback method on completion of the synchronization task
                public void onComplete() {
                    mSyncTask = null; // Resetting the synchronization task
                    sendBroadcast(""); // Sending broadcast indicating completion
                    stopSelf(); // Stopping the service after synchronization
                }
            });
            sendBroadcast(""); // Sending broadcast to indicate start of synchronization
            mSyncTask.execute(); // Executing the synchronization task
        }
    }

    private void cancelSync() {
        if (mSyncTask != null) {
            mSyncTask.cancelSync();
        }
    }

    @Override
    public void onCreate() {
        mSyncTask = null;
    }

    // Method called when the service is started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras(); // Getting extras from the intent
        if (bundle != null && bundle.containsKey(ACTION_STRING_NAME)) {
            // Handling different actions based on the received action type
            switch (bundle.getInt(ACTION_STRING_NAME, ACTION_INVALID)) {
                case ACTION_START_SYNC:
                    startSync(); // Starting synchronization
                    break;
                case ACTION_CANCEL_SYNC:
                    cancelSync(); // Cancelling synchronization
                    break;
                default:
                    break;
            }
            return START_STICKY; // Returning the service's behavior on restart
        }
        return super.onStartCommand(intent, flags, startId); // Returning default behavior if no action specified
    }

    // Method called when system memory is low
    @Override
    public void onLowMemory() {
        if (mSyncTask != null) {
            mSyncTask.cancelSync(); // Cancelling synchronization if in progress
        }
    }

    // Method to bind the service; returning null since binding is not needed
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Method to send a broadcast with synchronization progress
    public void sendBroadcast(String msg) {
        mSyncProgress = msg; // Setting synchronization progress message
        Intent intent = new Intent(GTASK_SERVICE_BROADCAST_NAME); // Creating a broadcast intent
        intent.putExtra(GTASK_SERVICE_BROADCAST_IS_SYNCING, mSyncTask != null); // Adding sync status to the intent
        intent.putExtra(GTASK_SERVICE_BROADCAST_PROGRESS_MSG, msg); // Adding progress message to the intent
        sendBroadcast(intent); // Sending the broadcast
    }

    // Method to start synchronization from an activity
    public static void startSync(Activity activity) {
        GTaskManager.getInstance().setActivityContext(activity); // Setting activity context in GTaskManager
        Intent intent = new Intent(activity, GTaskSyncService.class); // Creating an intent for the synchronization service
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_START_SYNC); // Adding action to start sync
        activity.startService(intent); // Starting the synchronization service
    }

    // Method to cancel synchronization from a context
    public static void cancelSync(Context context) {
        Intent intent = new Intent(context, GTaskSyncService.class); // Creating an intent for the synchronization service
        intent.putExtra(GTaskSyncService.ACTION_STRING_NAME, GTaskSyncService.ACTION_CANCEL_SYNC); // Adding action to cancel sync
        context.startService(intent); // Starting the synchronization service to cancel sync
    }


    public static boolean isSyncing() {
        return mSyncTask != null;
    }

    public static String getProgressString() {
        return mSyncProgress;
    }
}
