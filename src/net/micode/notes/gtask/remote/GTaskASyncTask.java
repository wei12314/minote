
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import net.micode.notes.R;
import net.micode.notes.ui.NotesListActivity;
import net.micode.notes.ui.NotesPreferenceActivity;


public class GTaskASyncTask extends AsyncTask<Void, String, Integer> { // Class declaration extending AsyncTask

    private static int GTASK_SYNC_NOTIFICATION_ID = 5234235; // Declaration of a static integer variable

    public interface OnCompleteListener { // Interface declaration
        void onComplete(); // Interface method signature
    }

    private Context mContext; // Declaration of a Context variable
    private NotificationManager mNotifiManager; // Declaration of a NotificationManager variable
    private GTaskManager mTaskManager; // Declaration of a GTaskManager variable
    private OnCompleteListener mOnCompleteListener; // Declaration of a listener variable of type OnCompleteListener

    public GTaskASyncTask(Context context, OnCompleteListener listener) { // Constructor with Context and OnCompleteListener parameters
        mContext = context; // Assigning passed Context to the class variable
        mOnCompleteListener = listener; // Assigning passed OnCompleteListener to the class variable
        mNotifiManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE); // Initializing NotificationManager
        mTaskManager = GTaskManager.getInstance(); // Initializing GTaskManager
    }

    public void cancelSync() { // Method to cancel synchronization
        mTaskManager.cancelSync(); // Calling a method from GTaskManager to cancel synchronization
    }

    public void publishProgess(String message) { // Method to publish progress with a message
        publishProgress(new String[]{message}); // Publishing progress with the provided message
    }

    private void showNotification(int tickerId, String content) { // Method to show a notification
        Notification notification = new Notification(R.drawable.notification, mContext.getString(tickerId), System.currentTimeMillis()); // Creating a new notification
        notification.defaults = Notification.DEFAULT_LIGHTS; // Setting default lights for the notification
        notification.flags = Notification.FLAG_AUTO_CANCEL; // Setting notification flags

        PendingIntent pendingIntent; // Declaration of PendingIntent variable

        if (tickerId != R.string.ticker_success) { // Checking the tickerId for a specific value
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext, NotesPreferenceActivity.class), 0); // Creating a PendingIntent for a specific activity
        } else {
            pendingIntent = PendingIntent.getActivity(mContext, 0, new Intent(mContext, NotesListActivity.class), 0); // Creating a PendingIntent for a different activity
        }

        notification.setLatestEventInfo(mContext, mContext.getString(R.string.app_name), content, pendingIntent); // Setting the latest event information for the notification
        mNotifiManager.notify(GTASK_SYNC_NOTIFICATION_ID, notification); // Notifying using the NotificationManager with a specific ID
    }

    @Override
    protected Integer doInBackground(Void... unused) { // Background task execution method
        publishProgess(mContext.getString(R.string.sync_progress_login, NotesPreferenceActivity.getSyncAccountName(mContext))); // Publishing progress with a specific message
        return mTaskManager.sync(mContext, this); // Initiating synchronization using GTaskManager
    }

    @Override
    protected void onProgressUpdate(String... progress) { // Method called when progress is updated
        showNotification(R.string.ticker_syncing, progress[0]); // Showing a notification for syncing progress
        if (mContext instanceof GTaskSyncService) { // Checking if the context is an instance of GTaskSyncService
            ((GTaskSyncService) mContext).sendBroadcast(progress[0]); // Broadcasting progress if it is an instance of GTaskSyncService
        }
    }


    @Override
    protected void onPostExecute(Integer result) { // Method called after the background task is finished
        if (result == GTaskManager.STATE_SUCCESS) { // Checking the result of the background task
            showNotification(R.string.ticker_success, mContext.getString(R.string.success_sync_account, mTaskManager.getSyncAccount())); // Showing a success notification
            NotesPreferenceActivity.setLastSyncTime(mContext, System.currentTimeMillis()); // Setting the last synchronization time
        } else if (result == GTaskManager.STATE_NETWORK_ERROR) { // Handling network error result
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_network)); // Showing a notification for network error
        } else if (result == GTaskManager.STATE_INTERNAL_ERROR) { // Handling internal error result
            showNotification(R.string.ticker_fail, mContext.getString(R.string.error_sync_internal)); // Showing a notification for internal error
        } else if (result == GTaskManager.STATE_SYNC_CANCELLED) { // Handling cancelled synchronization result
            showNotification(R.string.ticker_cancel, mContext.getString(R.string.error_sync_cancelled)); // Showing a notification for cancelled synchronization
        }
        
        if (mOnCompleteListener != null) { // Checking if there's a completion listener
            new Thread(new Runnable() { // Creating a new thread
                public void run() { // Implementing the run method for the thread
                    mOnCompleteListener.onComplete(); // Calling the onComplete method of the listener
                }
            }).start(); // Starting the thread
        }
    }
}
