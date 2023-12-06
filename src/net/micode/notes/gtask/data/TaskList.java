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

package net.micode.notes.gtask.data;

import android.database.Cursor;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class TaskList extends Node { // Class representing a list of tasks

    private static final String TAG = TaskList.class.getSimpleName(); // TAG for logging

    private int mIndex; // Index of the task list

    private ArrayList<Task> mChildren; // List of child tasks

    public TaskList() { // Constructor for TaskList class
        super(); // Calling superclass constructor
        mChildren = new ArrayList<Task>(); // Initializing the list of child tasks
        mIndex = 1; // Setting the index to 1 by default
    }


    public JSONObject getCreateAction(int actionId) { // Method to generate create action JSON for a TaskList

        JSONObject js = new JSONObject(); // Creating a new JSONObject

        try {
            // action_type
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_CREATE); // Setting "action_type" key in the JSONObject to "create"

            // action_id
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId); // Setting "action_id" key in the JSONObject with the provided actionId

            // index
            js.put(GTaskStringUtils.GTASK_JSON_INDEX, mIndex); // Setting "index" key in the JSONObject with the value of mIndex

            // entity_delta
            JSONObject entity = new JSONObject(); // Creating a new JSONObject for entity_delta
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName()); // Setting "name" key in the "entity" JSONObject using the result of getName() method
            entity.put(GTaskStringUtils.GTASK_JSON_CREATOR_ID, "null"); // Setting "creator_id" key in the "entity" JSONObject to "null"
            entity.put(GTaskStringUtils.GTASK_JSON_ENTITY_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_GROUP); // Setting "entity_type" key in the "entity" JSONObject to "group"
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity); // Putting the "entity" JSONObject in the main JSONObject under the key "entity_delta"

        } catch (JSONException e) { // Handling JSONException
            Log.e(TAG, e.toString()); // Logging the exception
            e.printStackTrace(); // Printing the stack trace of the exception
            throw new ActionFailureException("fail to generate tasklist-create jsonobject"); // Throwing a custom exception in case of failure to generate the JSONObject
        }

        return js; // Returning the constructed JSONObject
    }


    public JSONObject getUpdateAction(int actionId) { // Method to generate update action JSON for a TaskList

        JSONObject js = new JSONObject(); // Creating a new JSONObject

        try {
            // action_type
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_UPDATE); // Setting "action_type" key in the JSONObject to "update"

            // action_id
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId); // Setting "action_id" key in the JSONObject with the provided actionId

            // id
            js.put(GTaskStringUtils.GTASK_JSON_ID, getGid()); // Setting "id" key in the JSONObject with the result of getGid() method

            // entity_delta
            JSONObject entity = new JSONObject(); // Creating a new JSONObject for entity_delta
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName()); // Setting "name" key in the "entity" JSONObject using the result of getName() method
            entity.put(GTaskStringUtils.GTASK_JSON_DELETED, getDeleted()); // Setting "deleted" key in the "entity" JSONObject with the result of getDeleted() method
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity); // Putting the "entity" JSONObject in the main JSONObject under the key "entity_delta"

        } catch (JSONException e) { // Handling JSONException
            Log.e(TAG, e.toString()); // Logging the exception
            e.printStackTrace(); // Printing the stack trace of the exception
            throw new ActionFailureException("fail to generate tasklist-update jsonobject"); // Throwing a custom exception in case of failure to generate the JSONObject
        }

        return js; // Returning the constructed JSONObject
    }


    public void setContentByRemoteJSON(JSONObject js) { // Method to set content based on provided remote JSON

        if (js != null) { // Checking if the JSONObject is not null
            try {
                // id
                if (js.has(GTaskStringUtils.GTASK_JSON_ID)) {
                    setGid(js.getString(GTaskStringUtils.GTASK_JSON_ID)); // Setting Gid if the key "id" exists in the JSONObject
                }

                // last_modified
                if (js.has(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED)) {
                    setLastModified(js.getLong(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED)); // Setting lastModified if the key "last_modified" exists in the JSONObject
                }

                // name
                if (js.has(GTaskStringUtils.GTASK_JSON_NAME)) {
                    setName(js.getString(GTaskStringUtils.GTASK_JSON_NAME)); // Setting name if the key "name" exists in the JSONObject
                }

            } catch (JSONException e) { // Handling JSONException
                Log.e(TAG, e.toString()); // Logging the exception
                e.printStackTrace(); // Printing the stack trace of the exception
                throw new ActionFailureException("fail to get tasklist content from jsonobject"); // Throwing a custom exception in case of failure to get content from the JSONObject
            }
        }
    }


    public void setContentByLocalJSON(JSONObject js) { // Method to set content based on provided local JSON

        if (js == null || !js.has(GTaskStringUtils.META_HEAD_NOTE)) { // Checking if the JSONObject is null or doesn't contain "META_HEAD_NOTE"
            Log.w(TAG, "setContentByLocalJSON: nothing is available"); // Logging a warning if no content is available
        }

        try {
            JSONObject folder = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE); // Retrieving the "META_HEAD_NOTE" JSONObject from the provided JSON

            if (folder.getInt(NoteColumns.TYPE) == Notes.TYPE_FOLDER) { // Checking the type of the folder
                String name = folder.getString(NoteColumns.SNIPPET); // Getting the snippet (name) of the folder
                setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + name); // Setting the name with a prefix for folder type tasks
            } else if (folder.getInt(NoteColumns.TYPE) == Notes.TYPE_SYSTEM) { // Checking if the folder type is a system type
                if (folder.getLong(NoteColumns.ID) == Notes.ID_ROOT_FOLDER) {
                    setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_DEFAULT);
                } else if (folder.getLong(NoteColumns.ID) == Notes.ID_CALL_RECORD_FOLDER) {
                    setName(GTaskStringUtils.MIUI_FOLDER_PREFFIX + GTaskStringUtils.FOLDER_CALL_NOTE);
                } else {
                    Log.e(TAG, "invalid system folder"); // Logging an error for an invalid system folder
                }
            } else {
                Log.e(TAG, "error type"); // Logging an error for an unknown type
            }
        } catch (JSONException e) { // Handling JSONException
            Log.e(TAG, e.toString()); // Logging the exception
            e.printStackTrace(); // Printing the stack trace of the exception
        }
    }


    public JSONObject getLocalJSONFromContent() { // Method to create local JSON from content

        try {
            JSONObject js = new JSONObject(); // Creating a new JSONObject
            JSONObject folder = new JSONObject(); // Creating a new JSONObject for the folder

            String folderName = getName(); // Getting the name of the folder

            if (getName().startsWith(GTaskStringUtils.MIUI_FOLDER_PREFFIX)) { // Checking if the folder name starts with a specific prefix
                folderName = folderName.substring(GTaskStringUtils.MIUI_FOLDER_PREFFIX.length(),
                        folderName.length()); // Removing the prefix from the folder name
            }

            folder.put(NoteColumns.SNIPPET, folderName); // Putting the folder name in the "SNIPPET" field of the folder JSONObject

            if (folderName.equals(GTaskStringUtils.FOLDER_DEFAULT)
                    || folderName.equals(GTaskStringUtils.FOLDER_CALL_NOTE)) { // Checking if the folder name matches predefined system folder names
                folder.put(NoteColumns.TYPE, Notes.TYPE_SYSTEM); // Setting the folder type as system type
            } else {
                folder.put(NoteColumns.TYPE, Notes.TYPE_FOLDER); // Setting the folder type as a regular folder
            }

            js.put(GTaskStringUtils.META_HEAD_NOTE, folder); // Putting the folder JSONObject in the main JSONObject under the key "META_HEAD_NOTE"

            return js; // Returning the constructed JSONObject

        } catch (JSONException e) { // Handling JSONException
            Log.e(TAG, e.toString()); // Logging the exception
            e.printStackTrace(); // Printing the stack trace of the exception
            return null; // Returning null in case of exception
        }
    }


    // Method to determine synchronization action based on cursor data
        public int getSyncAction(Cursor c) {
            try {
                // Checking local modifications and synchronization status
                if (c.getInt(SqlNote.LOCAL_MODIFIED_COLUMN) == 0) {
                    if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                        return SYNC_ACTION_NONE; // No update both sides
                    } else {
                        return SYNC_ACTION_UPDATE_LOCAL; // Apply remote to local
                    }
                } else {
                    if (!c.getString(SqlNote.GTASK_ID_COLUMN).equals(getGid())) {
                        Log.e(TAG, "gtask id doesn't match");
                        return SYNC_ACTION_ERROR; // Error due to mismatched IDs
                    }
                    if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                        return SYNC_ACTION_UPDATE_REMOTE; // Local modification only
                    } else {
                        return SYNC_ACTION_UPDATE_REMOTE; // For folder conflicts, apply local modification
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
                e.printStackTrace();
            }
            return SYNC_ACTION_ERROR; // Return error action if an exception occurs
        }

    public int getChildTaskCount() {
        return mChildren.size();
    }

    // add child task into this list
    public boolean addChildTask(Task task) { // Method to add a child task to the task list
        boolean ret = false; // Initializing return status as false

        if (task != null && !mChildren.contains(task)) { // Checking if the task is not null and not already present in the list
            ret = mChildren.add(task); // Adding the task to the list

            if (ret) { // If addition is successful
                // Need to set prior sibling and parent for the added task
                task.setPriorSibling(mChildren.isEmpty() ? null : mChildren.get(mChildren.size() - 1));
                task.setParent(this);
            }
        }
        return ret; // Returning the status of task addition (true/false)
    }

    // Add child task into this list at a specific index
    public boolean addChildTask(Task task, int index) { // Method to add a child task at a specific index in the task list
        if (index < 0 || index > mChildren.size()) { // Checking if the index is out of bounds
            Log.e(TAG, "add child task: invalid index"); // Logging an error for an invalid index
            return false; // Returning false indicating the operation failed
        }

        int pos = mChildren.indexOf(task); // Getting the position of the task in the list
        if (task != null && pos == -1) { // Checking if the task is not null and not already present in the list
            mChildren.add(index, task); // Adding the task at the specified index in the list

            // Updating the task list and setting prior siblings for the added task
            Task preTask = null;
            Task afterTask = null;
            if (index != 0)
                preTask = mChildren.get(index - 1);
            if (index != mChildren.size() - 1)
                afterTask = mChildren.get(index + 1);

            task.setPriorSibling(preTask);
            if (afterTask != null)
                afterTask.setPriorSibling(task);
        }

        return true; // Returning true indicating the task was added successfully
    }


    
    public boolean removeChildTask(Task task) { // Method to remove a child task from the task list
        boolean ret = false; // Initializing return status as false
        int index = mChildren.indexOf(task); // Getting the index of the task in the list

        if (index != -1) { // Checking if the task exists in the list
            ret = mChildren.remove(task); // Removing the task from the list

            if (ret) { // If removal is successful
                // Resetting prior sibling and parent of the removed task
                task.setPriorSibling(null);
                task.setParent(null);

                // Updating the task list if necessary
                if (index != mChildren.size()) {
                    mChildren.get(index).setPriorSibling(index == 0 ? null : mChildren.get(index - 1));
                }
            }
        }
        return ret; // Returning the status of task removal (true/false)
    }

    public boolean moveChildTask(Task task, int index) { // Method to move a child task to a specific index
        if (index < 0 || index >= mChildren.size()) { // Checking if the index is out of bounds
            Log.e(TAG, "move child task: invalid index"); // Logging an error for an invalid index
            return false; // Returning false indicating the operation failed
        }

        int pos = mChildren.indexOf(task); // Getting the position of the task in the list
        if (pos == -1) { // Checking if the task exists in the list
            Log.e(TAG, "move child task: the task should be in the list"); // Logging an error if the task is not in the list
            return false; // Returning false indicating the operation failed
        }

        if (pos == index) // Checking if the task is already at the desired index
            return true; // Returning true indicating the task is already at the desired position

        // Removing the task from its current position and adding it to the specified index
        return (removeChildTask(task) && addChildTask(task, index));
    }


    public Task findChildTaskByGid(String gid) {
        for (int i = 0; i < mChildren.size(); i++) {
            Task t = mChildren.get(i);
            if (t.getGid().equals(gid)) {
                return t;
            }
        }
        return null;
    }

    public int getChildTaskIndex(Task task) {
        return mChildren.indexOf(task);
    }

    public Task getChildTaskByIndex(int index) {
        if (index < 0 || index >= mChildren.size()) {
            Log.e(TAG, "getTaskByIndex: invalid index");
            return null;
        }
        return mChildren.get(index);
    }

    public Task getChilTaskByGid(String gid) {
        for (Task task : mChildren) {
            if (task.getGid().equals(gid))
                return task;
        }
        return null;
    }

    public ArrayList<Task> getChildTaskList() {
        return this.mChildren;
    }

    public void setIndex(int index) {
        this.mIndex = index;
    }

    public int getIndex() {
        return this.mIndex;
    }
}
