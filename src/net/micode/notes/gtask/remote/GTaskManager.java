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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.data.MetaData;
import net.micode.notes.gtask.data.Node;
import net.micode.notes.gtask.data.SqlNote;
import net.micode.notes.gtask.data.Task;
import net.micode.notes.gtask.data.TaskList;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.gtask.exception.NetworkFailureException;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;


public class GTaskManager {
    private static final String TAG = GTaskManager.class.getSimpleName();

    public static final int STATE_SUCCESS = 0;

    public static final int STATE_NETWORK_ERROR = 1;

    public static final int STATE_INTERNAL_ERROR = 2;

    public static final int STATE_SYNC_IN_PROGRESS = 3;

    public static final int STATE_SYNC_CANCELLED = 4;

    private static GTaskManager mInstance = null;

    private Activity mActivity;

    private Context mContext;

    private ContentResolver mContentResolver;

    private boolean mSyncing;

    private boolean mCancelled;

    private HashMap<String, TaskList> mGTaskListHashMap;

    private HashMap<String, Node> mGTaskHashMap;

    private HashMap<String, MetaData> mMetaHashMap;

    private TaskList mMetaList;

    private HashSet<Long> mLocalDeleteIdMap;

    private HashMap<String, Long> mGidToNid;

    private HashMap<Long, String> mNidToGid;

    private GTaskManager() {
        mSyncing = false;
        mCancelled = false;
        mGTaskListHashMap = new HashMap<String, TaskList>();
        mGTaskHashMap = new HashMap<String, Node>();
        mMetaHashMap = new HashMap<String, MetaData>();
        mMetaList = null;
        mLocalDeleteIdMap = new HashSet<Long>();
        mGidToNid = new HashMap<String, Long>();
        mNidToGid = new HashMap<Long, String>();
    }

    public static synchronized GTaskManager getInstance() {
        if (mInstance == null) {
            mInstance = new GTaskManager();
        }
        return mInstance;
    }

    public synchronized void setActivityContext(Activity activity) {
        // used for getting authtoken
        mActivity = activity;
    }

    public int sync(Context context, GTaskASyncTask asyncTask) { // Method to synchronize data with Google Tasks
        if (mSyncing) { // Checking if synchronization is already in progress
            Log.d(TAG, "Sync is in progress"); // Logging that synchronization is ongoing
            return STATE_SYNC_IN_PROGRESS; // Returning the ongoing sync state
        }
        
        // Initializing synchronization variables and data structures
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mSyncing = true;
        mCancelled = false;
        mGTaskListHashMap.clear();
        mGTaskHashMap.clear();
        mMetaHashMap.clear();
        mLocalDeleteIdMap.clear();
        mGidToNid.clear();
        mNidToGid.clear();

        try {
            GTaskClient client = GTaskClient.getInstance(); // Getting an instance of GTaskClient

            client.resetUpdateArray(); // Resetting the update array in the GTaskClient instance

            // Logging into Google Tasks
            if (!mCancelled) {
                if (!client.login(mActivity)) { // If login fails
                    throw new NetworkFailureException("login google task failed"); // Throw a network failure exception
                }
            }

            asyncTask.publishProgess(mContext.getString(R.string.sync_progress_init_list)); // Publishing progress - initializing list
            initGTaskList(); // Initializing the Google Task list

            asyncTask.publishProgess(mContext.getString(R.string.sync_progress_syncing)); // Publishing progress - syncing
            syncContent(); // Synchronizing content
        } catch (NetworkFailureException e) { // Catching network failure exceptions
            Log.e(TAG, e.toString()); // Logging the exception
            return STATE_NETWORK_ERROR; // Returning network error state
        } catch (ActionFailureException e) { // Catching action failure exceptions
            Log.e(TAG, e.toString()); // Logging the exception
            return STATE_INTERNAL_ERROR; // Returning internal error state
        } catch (Exception e) { // Catching other exceptions
            Log.e(TAG, e.toString()); // Logging the exception
            e.printStackTrace(); // Printing the stack trace
            return STATE_INTERNAL_ERROR; // Returning internal error state
        } finally {
            // Clearing data structures and variables, setting syncing to false
            mGTaskListHashMap.clear();
            mGTaskHashMap.clear();
            mMetaHashMap.clear();
            mLocalDeleteIdMap.clear();
            mGidToNid.clear();
            mNidToGid.clear();
            mSyncing = false;
        }

        return mCancelled ? STATE_SYNC_CANCELLED : STATE_SUCCESS; // Returning sync cancelled or success state
    }


    private void initGTaskList() throws NetworkFailureException {
        if (mCancelled)
            return; // If cancelled, return without performing any operation

        GTaskClient client = GTaskClient.getInstance(); // Getting an instance of GTaskClient

        try {
            JSONArray jsTaskLists = client.getTaskLists(); // Getting task lists from GTaskClient

            mMetaList = null; // Initializing meta list as null initially

            // Iterating through the task lists obtained
            for (int i = 0; i < jsTaskLists.length(); i++) {
                JSONObject object = jsTaskLists.getJSONObject(i);
                String gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                String name = object.getString(GTaskStringUtils.GTASK_JSON_NAME);

                // Checking if the task list is a meta list
                if (name.equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_META)) {
                    mMetaList = new TaskList(); // Creating a new TaskList object for the meta list
                    mMetaList.setContentByRemoteJSON(object); // Setting content from the remote JSON to the meta list

                    // Loading meta data for the meta list
                    JSONArray jsMetas = client.getTaskList(gid);
                    for (int j = 0; j < jsMetas.length(); j++) {
                        object = jsMetas.getJSONObject(j);
                        MetaData metaData = new MetaData();
                        metaData.setContentByRemoteJSON(object);
                        if (metaData.isWorthSaving()) {
                            mMetaList.addChildTask(metaData); // Adding meta data as a child task
                            if (metaData.getGid() != null) {
                                mMetaHashMap.put(metaData.getRelatedGid(), metaData); // Storing meta data in the hash map
                            }
                        }
                    }
                }
            }

            // Creating a meta list if it does not exist
            if (mMetaList == null) {
                mMetaList = new TaskList();
                mMetaList.setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_META);
                GTaskClient.getInstance().createTaskList(mMetaList); // Creating the meta list
            }

            // Initializing task lists
            for (int i = 0; i < jsTaskLists.length(); i++) {
                JSONObject object = jsTaskLists.getJSONObject(i);
                String gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                String name = object.getString(GTaskStringUtils.GTASK_JSON_NAME);

                // Checking if the task list is not a meta list
                if (name.startsWith(GTaskStringUtils.MIUI_FOLDER_PREFFIX)
                        && !name.equals(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_META)) {
                    TaskList tasklist = new TaskList(); // Creating a new TaskList object
                    tasklist.setContentByRemoteJSON(object); // Setting content from the remote JSON to the task list
                    mGTaskListHashMap.put(gid, tasklist); // Storing the task list in the hash map
                    mGTaskHashMap.put(gid, tasklist); // Storing the task list in another hash map

                    // Loading tasks for the task list
                    JSONArray jsTasks = client.getTaskList(gid);
                    for (int j = 0; j < jsTasks.length(); j++) {
                        object = jsTasks.getJSONObject(j);
                        gid = object.getString(GTaskStringUtils.GTASK_JSON_ID);
                        Task task = new Task();
                        task.setContentByRemoteJSON(object);
                        if (task.isWorthSaving()) {
                            task.setMetaInfo(mMetaHashMap.get(gid));
                            tasklist.addChildTask(task); // Adding task as a child task to the task list
                            mGTaskHashMap.put(gid, task); // Storing the task in the hash map
                        }
                    }
                }
            }
        } catch (JSONException e) { // Handling JSON exception
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("initGTaskList: handling JSONObject failed");
        }
    }


    private void syncContent() throws NetworkFailureException {
        int syncType;
        Cursor c = null;
        String gid;
        Node node;

        mLocalDeleteIdMap.clear(); // Clearing the local delete ID map

        if (mCancelled) {
            return; // If cancelled, return without performing any operation
        }

        // for local deleted note
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type<>? AND parent_id=?)", new String[]{
                            String.valueOf(Notes.TYPE_SYSTEM), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, null);

            if (c != null) {
                while (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    node = mGTaskHashMap.get(gid);
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        doContentSync(Node.SYNC_ACTION_DEL_REMOTE, node, c); // Deleting remote node
                    }

                    mLocalDeleteIdMap.add(c.getLong(SqlNote.ID_COLUMN)); // Adding ID to local delete map
                }
            } else {
                Log.w(TAG, "failed to query trash folder");
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // sync folder first
        syncFolder(); // Syncing the folder

        // for note existing in database
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type=? AND parent_id<>?)", new String[]{
                            String.valueOf(Notes.TYPE_NOTE), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC");

            if (c != null) {
                while (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    node = mGTaskHashMap.get(gid);
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        mGidToNid.put(gid, c.getLong(SqlNote.ID_COLUMN)); // Mapping GID to NID
                        mNidToGid.put(c.getLong(SqlNote.ID_COLUMN), gid); // Mapping NID to GID
                        syncType = node.getSyncAction(c); // Getting sync action for the node
                    } else {
                        if (c.getString(SqlNote.GTASK_ID_COLUMN).trim().length() == 0) {
                            // local add
                            syncType = Node.SYNC_ACTION_ADD_REMOTE; // Adding remote node
                        } else {
                            // remote delete
                            syncType = Node.SYNC_ACTION_DEL_LOCAL; // Deleting local node
                        }
                    }
                    doContentSync(syncType, node, c); // Performing content sync
                }
            } else {
                Log.w(TAG, "failed to query existing note in database");
            }

        } finally {
            if (c != null) {
                c.close();
            }
        }

        // go through remaining items
        Iterator<Map.Entry<String, Node>> iter = mGTaskHashMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Node> entry = iter.next();
            node = entry.getValue();
            doContentSync(Node.SYNC_ACTION_ADD_LOCAL, node, null); // Adding local node
        }

        // mCancelled can be set by another thread, so we need to check one by one
        // clear local delete table
        if (!mCancelled) {
            if (!DataUtils.batchDeleteNotes(mContentResolver, mLocalDeleteIdMap)) {
                throw new ActionFailureException("failed to batch-delete local deleted notes");
            }
        }

        // refresh local sync id
        if (!mCancelled) {
            GTaskClient.getInstance().commitUpdate(); // Committing the update
            refreshLocalSyncId(); // Refreshing local sync ID
        }
    }


    private void syncFolder() throws NetworkFailureException {
        Cursor c = null;
        String gid;
        Node node;
        int syncType;

        if (mCancelled) {
            return;
        }

        // for root folder
        try {
            c = mContentResolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI,
                    Notes.ID_ROOT_FOLDER), SqlNote.PROJECTION_NOTE, null, null, null);
            if (c != null) {
                c.moveToNext();
                gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                node = mGTaskHashMap.get(gid);
                if (node != null) {
                    mGTaskHashMap.remove(gid);
                    mGidToNid.put(gid, (long) Notes.ID_ROOT_FOLDER);
                    mNidToGid.put((long) Notes.ID_ROOT_FOLDER, gid);
                    // for system folder, only update remote name if necessary
                    if (!node.getName().equals(
                            GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT))
                        doContentSync(Node.SYNC_ACTION_UPDATE_REMOTE, node, c);
                } else {
                    doContentSync(Node.SYNC_ACTION_ADD_REMOTE, node, c);
                }
            } else {
                Log.w(TAG, "failed to query root folder");
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // for call-note folder
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE, "(_id=?)",
                    new String[]{
                            String.valueOf(Notes.ID_CALL_RECORD_FOLDER)
                    }, null);
            if (c != null) {
                if (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    node = mGTaskHashMap.get(gid);
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        mGidToNid.put(gid, (long) Notes.ID_CALL_RECORD_FOLDER);
                        mNidToGid.put((long) Notes.ID_CALL_RECORD_FOLDER, gid);
                        // for system folder, only update remote name if necessary
                        if (!node.getName().equals(
                                GTaskStringUtils.MIUI_FOLDER_PREFFIX
                                        + GTaskStringUtils.FOLDER_CALL_NOTE))
                            doContentSync(Node.SYNC_ACTION_UPDATE_REMOTE, node, c);
                    } else {
                        doContentSync(Node.SYNC_ACTION_ADD_REMOTE, node, c);
                    }
                }
            } else {
                Log.w(TAG, "failed to query call note folder");
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // for local existing folders
        try {
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type=? AND parent_id<>?)", new String[]{
                            String.valueOf(Notes.TYPE_FOLDER), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    node = mGTaskHashMap.get(gid);
                    if (node != null) {
                        mGTaskHashMap.remove(gid);
                        mGidToNid.put(gid, c.getLong(SqlNote.ID_COLUMN));
                        mNidToGid.put(c.getLong(SqlNote.ID_COLUMN), gid);
                        syncType = node.getSyncAction(c);
                    } else {
                        if (c.getString(SqlNote.GTASK_ID_COLUMN).trim().length() == 0) {
                            // local add
                            syncType = Node.SYNC_ACTION_ADD_REMOTE;
                        } else {
                            // remote delete
                            syncType = Node.SYNC_ACTION_DEL_LOCAL;
                        }
                    }
                    doContentSync(syncType, node, c);
                }
            } else {
                Log.w(TAG, "failed to query existing folder");
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // for remote add folders
        Iterator<Map.Entry<String, TaskList>> iter = mGTaskListHashMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, TaskList> entry = iter.next();
            gid = entry.getKey();
            node = entry.getValue();
            if (mGTaskHashMap.containsKey(gid)) {
                mGTaskHashMap.remove(gid);
                doContentSync(Node.SYNC_ACTION_ADD_LOCAL, node, null);
            }
        }

        if (!mCancelled) {
            GTaskClient.getInstance().commitUpdate(); // Committing the update
        }
    }


    // This method performs content synchronization based on the sync type and the provided node and cursor.
    private void doContentSync(int syncType, Node node, Cursor c) throws NetworkFailureException {
        // Checking if the sync process has been cancelled
        if (mCancelled) {
            return; // If cancelled, exit the method
        }

        MetaData meta; // Declaring a MetaData variable

        // Switch statement to handle different synchronization actions based on syncType
        switch (syncType) {
            case Node.SYNC_ACTION_ADD_LOCAL:
                addLocalNode(node); // Adding a local node
                break;
            case Node.SYNC_ACTION_ADD_REMOTE:
                addRemoteNode(node, c); // Adding a remote node
                break;
            case Node.SYNC_ACTION_DEL_LOCAL:
                // Getting metadata based on a specific column in the cursor
                meta = mMetaHashMap.get(c.getString(SqlNote.GTASK_ID_COLUMN));
                if (meta != null) {
                    // If metadata exists, deleting the node using GTaskClient
                    GTaskClient.getInstance().deleteNode(meta);
                }
                // Adding a local delete ID to the map
                mLocalDeleteIdMap.add(c.getLong(SqlNote.ID_COLUMN));
                break;
            case Node.SYNC_ACTION_DEL_REMOTE:
                // Getting metadata based on the GID of the node
                meta = mMetaHashMap.get(node.getGid());
                if (meta != null) {
                    // If metadata exists, deleting the node using GTaskClient
                    GTaskClient.getInstance().deleteNode(meta);
                }
                // Deleting the node using GTaskClient
                GTaskClient.getInstance().deleteNode(node);
                break;
            case Node.SYNC_ACTION_UPDATE_LOCAL:
                updateLocalNode(node, c); // Updating a local node
                break;
            case Node.SYNC_ACTION_UPDATE_REMOTE:
                updateRemoteNode(node, c); // Updating a remote node
                break;
            case Node.SYNC_ACTION_UPDATE_CONFLICT:
                // Handling conflict by updating the remote node (temporary solution)
                updateRemoteNode(node, c);
                break;
            case Node.SYNC_ACTION_NONE:
                // If no action is required, do nothing
                break;
            case Node.SYNC_ACTION_ERROR:
            default:
                // Handling an unknown or default sync action by throwing an exception
                throw new ActionFailureException("unknown sync action type");
        }
    }


    // Method to add a local node, handling different cases for TaskList and other nodes
    private void addLocalNode(Node node) throws NetworkFailureException {
        // Checking if the process is cancelled
        if (mCancelled) {
            return; // If cancelled, exit the method
        }

        SqlNote sqlNote; // Declaring a SqlNote variable

        // Checking if the node is an instance of TaskList
        if (node instanceof TaskList) {
            // Checking node name and creating SqlNote accordingly for specific cases
            if (node.getName().equals(
                    GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT)) {
                sqlNote = new SqlNote(mContext, Notes.ID_ROOT_FOLDER);
            } else if (node.getName().equals(
                    GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_CALL_NOTE)) {
                sqlNote = new SqlNote(mContext, Notes.ID_CALL_RECORD_FOLDER);
            } else {
                // Creating a default SqlNote and setting its content and parent ID
                sqlNote = new SqlNote(mContext);
                sqlNote.setContent(node.getLocalJSONFromContent());
                sqlNote.setParentId(Notes.ID_ROOT_FOLDER);
            }
        } else {
            // Handling other types of nodes
            sqlNote = new SqlNote(mContext);
            JSONObject js = node.getLocalJSONFromContent(); // Getting JSON data from the node

            try {
                // Checking JSON content for specific keys and handling them
                if (js.has(GTaskStringUtils.META_HEAD_NOTE)) {
                    JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);
                    if (note.has(NoteColumns.ID)) {
                        long id = note.getLong(NoteColumns.ID);
                        if (DataUtils.existInNoteDatabase(mContentResolver, id)) {
                            // If ID exists, remove it to create a new one
                            note.remove(NoteColumns.ID);
                        }
                    }
                }

                if (js.has(GTaskStringUtils.META_HEAD_DATA)) {
                    JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA);
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject data = dataArray.getJSONObject(i);
                        if (data.has(DataColumns.ID)) {
                            long dataId = data.getLong(DataColumns.ID);
                            if (DataUtils.existInDataDatabase(mContentResolver, dataId)) {
                                // If data ID exists, remove it to create a new one
                                data.remove(DataColumns.ID);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                // Catching JSON exception, logging, and printing the stack trace
                Log.w(TAG, e.toString());
                e.printStackTrace();
            }

            // Setting content and parent ID for SqlNote based on the node's information
            sqlNote.setContent(js);
            Long parentId = mGidToNid.get(((Task) node).getParent().getGid());
            if (parentId == null) {
                Log.e(TAG, "cannot find task's parent id locally");
                throw new ActionFailureException("cannot add local node");
            }
            sqlNote.setParentId(parentId.longValue());
        }

        // Creating the local node by setting its GTask ID and committing changes
        sqlNote.setGtaskId(node.getGid());
        sqlNote.commit(false);

        // Updating mapping between GTask ID and Note ID, and vice versa
        mGidToNid.put(node.getGid(), sqlNote.getId());
        mNidToGid.put(sqlNote.getId(), node.getGid());

        // Updating remote metadata using GTask ID and SqlNote
        updateRemoteMeta(node.getGid(), sqlNote);
    }


    // Method to update a local node using provided node and cursor information
    private void updateLocalNode(Node node, Cursor c) throws NetworkFailureException {
        // Checking if the process is cancelled
        if (mCancelled) {
            return; // If cancelled, exit the method
        }

        SqlNote sqlNote; // Declaring a SqlNote variable

        // Updating the note locally based on cursor information
        sqlNote = new SqlNote(mContext, c);
        sqlNote.setContent(node.getLocalJSONFromContent()); // Setting content from the node

        // Determining the parent ID based on node type (Task or defaulting to root folder)
        Long parentId = (node instanceof Task) ? mGidToNid.get(((Task) node).getParent().getGid())
                : new Long(Notes.ID_ROOT_FOLDER);

        // Checking if the parentId is null and handling the situation by throwing an exception
        if (parentId == null) {
            Log.e(TAG, "cannot find task's parent id locally");
            throw new ActionFailureException("cannot update local node");
        }

        // Setting the parent ID for the SqlNote and committing changes
        sqlNote.setParentId(parentId.longValue());
        sqlNote.commit(true);

        // Updating meta information using node's GTask ID and the updated SqlNote
        updateRemoteMeta(node.getGid(), sqlNote);
    }


    // Method to add a remote node using provided node and cursor information
    private void addRemoteNode(Node node, Cursor c) throws NetworkFailureException {
        // Checking if the process is cancelled
        if (mCancelled) {
            return; // If cancelled, exit the method
        }

        SqlNote sqlNote = new SqlNote(mContext, c); // Creating a SqlNote from the cursor data
        Node n; // Declaring a Node variable

        // Handling remote update based on the note type
        if (sqlNote.isNoteType()) { // If the SqlNote is of note type
            Task task = new Task(); // Creating a Task instance
            task.setContentByLocalJSON(sqlNote.getContent()); // Setting Task content from SqlNote

            // Getting parent GTask ID from mapping
            String parentGid = mNidToGid.get(sqlNote.getParentId());
            if (parentGid == null) {
                Log.e(TAG, "cannot find task's parent tasklist");
                throw new ActionFailureException("cannot add remote task");
            }

            // Adding child task to the specified task list
            mGTaskListHashMap.get(parentGid).addChildTask(task);

            // Creating the task remotely using GTaskClient
            GTaskClient.getInstance().createTask(task);
            n = (Node) task;

            // Adding meta information for the task
            updateRemoteMeta(task.getGid(), sqlNote);
        } else { // If not a note type, implying it's a TaskList
            TaskList tasklist = null; // Declaring a TaskList variable

            // Determining the folder name based on SqlNote ID
            String folderName = GTaskStringUtils.MIUI_FOLDER_PREFFIX;
            if (sqlNote.getId() == Notes.ID_ROOT_FOLDER)
                folderName += GTaskStringUtils.FOLDER_DEFAULT;
            else if (sqlNote.getId() == Notes.ID_CALL_RECORD_FOLDER)
                folderName += GTaskStringUtils.FOLDER_CALL_NOTE;
            else
                folderName += sqlNote.getSnippet();

            // Iterating through TaskLists to find a matching folder name
            Iterator<Map.Entry<String, TaskList>> iter = mGTaskListHashMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, TaskList> entry = iter.next();
                String gid = entry.getKey();
                TaskList list = entry.getValue();

                // Checking for a matching folder name
                if (list.getName().equals(folderName)) {
                    tasklist = list;
                    if (mGTaskHashMap.containsKey(gid)) {
                        mGTaskHashMap.remove(gid);
                    }
                    break;
                }
            }

            // If no match found, creating a new TaskList
            if (tasklist == null) {
                tasklist = new TaskList();
                tasklist.setContentByLocalJSON(sqlNote.getContent());
                GTaskClient.getInstance().createTaskList(tasklist);
                mGTaskListHashMap.put(tasklist.getGid(), tasklist);
            }
            n = (Node) tasklist;
        }

        // Updating local note by setting GTask ID and committing changes
        sqlNote.setGtaskId(n.getGid());
        sqlNote.commit(false);
        sqlNote.resetLocalModified();
        sqlNote.commit(true);

        // Updating mapping between GTask ID and Note ID, and vice versa
        mGidToNid.put(n.getGid(), sqlNote.getId());
        mNidToGid.put(sqlNote.getId(), n.getGid());
    }


    // Method to update remote metadata based on GTask ID and SqlNote
    private void updateRemoteMeta(String gid, SqlNote sqlNote) throws NetworkFailureException {
        // Checking if SqlNote exists and is of note type
        if (sqlNote != null && sqlNote.isNoteType()) {
            MetaData metaData = mMetaHashMap.get(gid); // Retrieving metadata based on GTask ID
            if (metaData != null) {
                // If metadata exists, updating its content and sending update request using GTaskClient
                metaData.setMeta(gid, sqlNote.getContent());
                GTaskClient.getInstance().addUpdateNode(metaData);
            } else {
                // If metadata doesn't exist, creating new metadata and adding it to metadata list
                metaData = new MetaData();
                metaData.setMeta(gid, sqlNote.getContent());
                mMetaList.addChildTask(metaData);
                mMetaHashMap.put(gid, metaData); // Updating metadata HashMap
                GTaskClient.getInstance().createTask(metaData); // Creating the metadata remotely
            }
        }
    }


    // Method to refresh local synchronization ID
    private void refreshLocalSyncId() throws NetworkFailureException {
        // Checking if the process is cancelled
        if (mCancelled) {
            return; // If cancelled, exit the method
        }

        // Clearing existing GTask and metadata HashMaps and initializing GTaskList
        mGTaskHashMap.clear();
        mGTaskListHashMap.clear();
        mMetaHashMap.clear();
        initGTaskList(); // Initializing the GTaskList

        Cursor c = null; // Initializing Cursor to null
        try {
            // Querying local notes to update synchronization ID
            c = mContentResolver.query(Notes.CONTENT_NOTE_URI, SqlNote.PROJECTION_NOTE,
                    "(type<>? AND parent_id<>?)", new String[]{
                            String.valueOf(Notes.TYPE_SYSTEM), String.valueOf(Notes.ID_TRASH_FOLER)
                    }, NoteColumns.TYPE + " DESC");

            if (c != null) {
                while (c.moveToNext()) {
                    // Retrieving GTask ID from the cursor
                    String gid = c.getString(SqlNote.GTASK_ID_COLUMN);
                    Node node = mGTaskHashMap.get(gid);

                    // Handling the case where the node is not found
                    if (node != null) {
                        mGTaskHashMap.remove(gid); // Removing the GTask from the map
                        ContentValues values = new ContentValues();
                        values.put(NoteColumns.SYNC_ID, node.getLastModified());

                        // Updating sync ID in the content resolver for the specific note
                        mContentResolver.update(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI,
                                c.getLong(SqlNote.ID_COLUMN)), values, null, null);
                    } else {
                        // Logging an error if a local item lacks a GID after sync and throwing an exception
                        Log.e(TAG, "something is missed");
                        throw new ActionFailureException(
                                "some local items don't have gid after sync");
                    }
                }
            } else {
                // Logging a warning if querying local notes fails
                Log.w(TAG, "failed to query local note to refresh sync id");
            }
        } finally {
            // Closing the cursor in a finally block to ensure its closure
            if (c != null) {
                c.close();
            }
        }
    }


    public String getSyncAccount() {
        return GTaskClient.getInstance().getSyncAccount().name;
    }

    public void cancelSync() {
        mCancelled = true;
    }
}
