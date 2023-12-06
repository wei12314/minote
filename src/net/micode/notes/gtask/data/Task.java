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
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Task extends Node {
    // Defining a class named Task that extends the Node class

    private static final String TAG = Task.class.getSimpleName();
    // Declaring a private constant TAG for logging purposes, holds the class's simple name

    private boolean mCompleted;
    // Declaring a boolean variable to track if the task is completed

    private String mNotes;
    // Declaring a String variable to hold notes related to the task

    private JSONObject mMetaInfo;
    // Declaring a JSONObject variable to store additional metadata related to the task

    private Task mPriorSibling;
    // Declaring a Task variable to reference the prior sibling task

    private TaskList mParent;
    // Declaring a TaskList variable to reference the parent task list

    public Task() {
        // Constructor for the Task class
        
        super();
        // Calls the constructor of the superclass (Node)

        mCompleted = false;
        // Initializes the task completion status as false by default

        mNotes = null;
        // Initializes the notes related to the task as null by default

        mPriorSibling = null;
        // Initializes the prior sibling task reference as null by default

        mParent = null;
        // Initializes the parent task list reference as null by default

        mMetaInfo = null;
        // Initializes the metadata information as null by default
    }

    public JSONObject getCreateAction(int actionId) {
        // Creates and returns a JSON object representing a 'create' action for this task
        
        JSONObject js = new JSONObject(); // Initialize a new JSON object

        try {
            // action_type
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE,
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_CREATE);
            // Sets the action type to 'create'

            // action_id
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId);
            // Sets the action ID provided as a parameter

            // index
            js.put(GTaskStringUtils.GTASK_JSON_INDEX, mParent.getChildTaskIndex(this));
            // Sets the task's index within its parent's list of child tasks

            // entity_delta
            JSONObject entity = new JSONObject(); // Initialize a new JSON object for the entity
            
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName());
            // Sets the task's name in the entity

            entity.put(GTaskStringUtils.GTASK_JSON_CREATOR_ID, "null");
            // Sets the creator ID as 'null'

            entity.put(GTaskStringUtils.GTASK_JSON_ENTITY_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_TASK);
            // Sets the entity type as 'task'

            if (getNotes() != null) {
                entity.put(GTaskStringUtils.GTASK_JSON_NOTES, getNotes());
                // Sets the task's notes in the entity if they exist
            }

            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity);
            // Adds the entity data to the main JSON object

            // parent_id
            js.put(GTaskStringUtils.GTASK_JSON_PARENT_ID, mParent.getGid());
            // Sets the parent ID as the task's parent's GID

            // dest_parent_type
            js.put(GTaskStringUtils.GTASK_JSON_DEST_PARENT_TYPE,
                    GTaskStringUtils.GTASK_JSON_TYPE_GROUP);
            // Sets the destination parent type as 'group'

            // list_id
            js.put(GTaskStringUtils.GTASK_JSON_LIST_ID, mParent.getGid());
            // Sets the list ID as the parent's GID

            // prior_sibling_id
            if (mPriorSibling != null) {
                js.put(GTaskStringUtils.GTASK_JSON_PRIOR_SIBLING_ID, mPriorSibling.getGid());
                // Sets the prior sibling ID if it exists
            }

        } catch (JSONException e) {
            // Handles JSON exceptions by logging and throwing an ActionFailureException
            Log.e(TAG, e.toString());
            e.printStackTrace();
            throw new ActionFailureException("fail to generate task-create jsonobject");
        }

        return js; // Returns the constructed JSON object
    }


    public JSONObject getUpdateAction(int actionId) { // Method declaration that generates a JSONObject for an update action

        JSONObject js = new JSONObject(); // Creating a new JSONObject instance

        try {
            // action_type
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_TYPE, // Setting "action_type" key in the JSONObject
                    GTaskStringUtils.GTASK_JSON_ACTION_TYPE_UPDATE); // Value for "action_type" key is set to "update"

            // action_id
            js.put(GTaskStringUtils.GTASK_JSON_ACTION_ID, actionId); // Setting "action_id" key with the provided actionId

            // id
            js.put(GTaskStringUtils.GTASK_JSON_ID, getGid()); // Setting "id" key using the result of the getGid() method

            // entity_delta
            JSONObject entity = new JSONObject(); // Creating a new JSONObject named "entity"
            entity.put(GTaskStringUtils.GTASK_JSON_NAME, getName()); // Setting "name" key in the "entity" JSONObject using the result of getName() method
            if (getNotes() != null) { // Checking if notes are not null
                entity.put(GTaskStringUtils.GTASK_JSON_NOTES, getNotes()); // Setting "notes" key in the "entity" JSONObject using the result of getNotes() method
            }
            entity.put(GTaskStringUtils.GTASK_JSON_DELETED, getDeleted()); // Setting "deleted" key in the "entity" JSONObject using the result of getDeleted() method
            js.put(GTaskStringUtils.GTASK_JSON_ENTITY_DELTA, entity); // Putting the "entity" JSONObject in the main JSONObject under the key "entity_delta"

        } catch (JSONException e) { // Handling JSONException
            Log.e(TAG, e.toString()); // Logging the exception
            e.printStackTrace(); // Printing the stack trace of the exception
            throw new ActionFailureException("fail to generate task-update jsonobject"); // Throwing a custom exception in case of failure to generate the JSONObject
        }

        return js; // Returning the constructed JSONObject
    }


    public void setContentByRemoteJSON(JSONObject js) { // Method to set content based on provided JSONObject

        if (js != null) { // Checking if the provided JSONObject is not null

            try {

                // id
                if (js.has(GTaskStringUtils.GTASK_JSON_ID)) { // Checking if the JSONObject has the key "id"
                    setGid(js.getString(GTaskStringUtils.GTASK_JSON_ID)); // Setting the value of "id" by retrieving the string associated with the key "id"
                }

                // last_modified
                if (js.has(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED)) { // Checking if the JSONObject has the key "last_modified"
                    setLastModified(js.getLong(GTaskStringUtils.GTASK_JSON_LAST_MODIFIED)); // Setting the value of "last_modified" by retrieving the long associated with the key "last_modified"
                }

                // name
                if (js.has(GTaskStringUtils.GTASK_JSON_NAME)) { // Checking if the JSONObject has the key "name"
                    setName(js.getString(GTaskStringUtils.GTASK_JSON_NAME)); // Setting the value of "name" by retrieving the string associated with the key "name"
                }

                // notes
                if (js.has(GTaskStringUtils.GTASK_JSON_NOTES)) { // Checking if the JSONObject has the key "notes"
                    setNotes(js.getString(GTaskStringUtils.GTASK_JSON_NOTES)); // Setting the value of "notes" by retrieving the string associated with the key "notes"
                }

                // deleted
                if (js.has(GTaskStringUtils.GTASK_JSON_DELETED)) { // Checking if the JSONObject has the key "deleted"
                    setDeleted(js.getBoolean(GTaskStringUtils.GTASK_JSON_DELETED)); // Setting the value of "deleted" by retrieving the boolean associated with the key "deleted"
                }

                // completed
                if (js.has(GTaskStringUtils.GTASK_JSON_COMPLETED)) { // Checking if the JSONObject has the key "completed"
                    setCompleted(js.getBoolean(GTaskStringUtils.GTASK_JSON_COMPLETED)); // Setting the value of "completed" by retrieving the boolean associated with the key "completed"
                }

            } catch (JSONException e) { // Handling JSONException
                Log.e(TAG, e.toString()); // Logging the exception
                e.printStackTrace(); // Printing the stack trace of the exception
                throw new ActionFailureException("fail to get task content from jsonobject"); // Throwing a custom exception in case of failure to get task content from the JSONObject
            }
        }
    }


    public void setContentByLocalJSON(JSONObject js) { // Method to set content based on provided local JSONObject

        if (js == null || !js.has(GTaskStringUtils.META_HEAD_NOTE) // Checking if the provided JSONObject is null or lacks necessary keys
                || !js.has(GTaskStringUtils.META_HEAD_DATA)) {
            Log.w(TAG, "setContentByLocalJSON: nothing is available"); // Logging a warning if the required keys are missing in the JSONObject
        }

        try {
            JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE); // Getting a JSONObject with key "META_HEAD_NOTE"
            JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA); // Getting a JSONArray with key "META_HEAD_DATA"

            if (note.getInt(NoteColumns.TYPE) != Notes.TYPE_NOTE) { // Checking if the type in the note JSONObject is not of TYPE_NOTE
                Log.e(TAG, "invalid type"); // Logging an error for an invalid type
                return; // Exiting the method
            }

            for (int i = 0; i < dataArray.length(); i++) { // Looping through the dataArray JSONArray
                JSONObject data = dataArray.getJSONObject(i); // Getting JSONObject at index i in the array

                if (TextUtils.equals(data.getString(DataColumns.MIME_TYPE), DataConstants.NOTE)) {
                    // Checking if the MIME_TYPE of the data is equal to DataConstants.NOTE
                    setName(data.getString(DataColumns.CONTENT)); // Setting the name using the CONTENT value in the data JSONObject
                    break; // Exiting the loop once the name is set
                }
            }

        } catch (JSONException e) { // Handling JSONException
            Log.e(TAG, e.toString()); // Logging the exception
            e.printStackTrace(); // Printing the stack trace of the exception
        }
    }


    public JSONObject getLocalJSONFromContent() { // Method to create local JSONObject from task content

        String name = getName(); // Getting the name

        try {
            if (mMetaInfo == null) { // Checking if metaInfo is null (new task created from web)

                if (name == null) { // Checking if name is null
                    Log.w(TAG, "the note seems to be an empty one"); // Logging a warning for an empty note
                    return null; // Returning null as the note is empty
                }

                // Creating new JSONObjects and JSONArray
                JSONObject js = new JSONObject();
                JSONObject note = new JSONObject();
                JSONArray dataArray = new JSONArray();
                JSONObject data = new JSONObject();

                data.put(DataColumns.CONTENT, name); // Putting the name in the content field of the data JSONObject
                dataArray.put(data); // Adding the data JSONObject to the dataArray JSONArray
                js.put(GTaskStringUtils.META_HEAD_DATA, dataArray); // Putting the dataArray in the js JSONObject under key "META_HEAD_DATA"

                note.put(NoteColumns.TYPE, Notes.TYPE_NOTE); // Putting the type of note in the note JSONObject
                js.put(GTaskStringUtils.META_HEAD_NOTE, note); // Putting the note JSONObject in the js JSONObject under key "META_HEAD_NOTE"

                return js; // Returning the constructed js JSONObject

            } else { // If mMetaInfo is not null (synced task)

                JSONObject note = mMetaInfo.getJSONObject(GTaskStringUtils.META_HEAD_NOTE); // Getting the note JSONObject from mMetaInfo
                JSONArray dataArray = mMetaInfo.getJSONArray(GTaskStringUtils.META_HEAD_DATA); // Getting the dataArray JSONArray from mMetaInfo

                for (int i = 0; i < dataArray.length(); i++) { // Looping through the dataArray JSONArray
                    JSONObject data = dataArray.getJSONObject(i); // Getting JSONObject at index i in the array

                    if (TextUtils.equals(data.getString(DataColumns.MIME_TYPE), DataConstants.NOTE)) {
                        // Checking if the MIME_TYPE of the data is equal to DataConstants.NOTE
                        data.put(DataColumns.CONTENT, getName()); // Updating the CONTENT field in the data JSONObject with the current name
                        break; // Exiting the loop once the content is updated
                    }
                }

                note.put(NoteColumns.TYPE, Notes.TYPE_NOTE); // Putting the type of note in the note JSONObject
                return mMetaInfo; // Returning the updated mMetaInfo JSONObject
            }
        } catch (JSONException e) { // Handling JSONException
            Log.e(TAG, e.toString()); // Logging the exception
            e.printStackTrace(); // Printing the stack trace of the exception
            return null; // Returning null in case of exception
        }
    }


    public void setMetaInfo(MetaData metaData) {
        if (metaData != null && metaData.getNotes() != null) {
            try {
                mMetaInfo = new JSONObject(metaData.getNotes());
            } catch (JSONException e) {
                Log.w(TAG, e.toString());
                mMetaInfo = null;
            }
        }
    }

    public int getSyncAction(Cursor c) { // Method to determine synchronization action based on cursor data

        try {
            JSONObject noteInfo = null;

            if (mMetaInfo != null && mMetaInfo.has(GTaskStringUtils.META_HEAD_NOTE)) {
                noteInfo = mMetaInfo.getJSONObject(GTaskStringUtils.META_HEAD_NOTE); // Getting note information from mMetaInfo if available
            }

            if (noteInfo == null) {
                Log.w(TAG, "it seems that note meta has been deleted"); // Logging a warning if noteInfo is null (note meta deleted)
                return SYNC_ACTION_UPDATE_REMOTE; // Returning action to update remote
            }

            if (!noteInfo.has(NoteColumns.ID)) {
                Log.w(TAG, "remote note id seems to be deleted"); // Logging a warning if remote note id is deleted
                return SYNC_ACTION_UPDATE_LOCAL; // Returning action to update local
            }

            // Validate the note id now
            if (c.getLong(SqlNote.ID_COLUMN) != noteInfo.getLong(NoteColumns.ID)) {
                Log.w(TAG, "note id doesn't match"); // Logging a warning if note id doesn't match
                return SYNC_ACTION_UPDATE_LOCAL; // Returning action to update local
            }

            if (c.getInt(SqlNote.LOCAL_MODIFIED_COLUMN) == 0) {
                // There is no local update
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // No update both sides
                    return SYNC_ACTION_NONE; // Returning no synchronization action
                } else {
                    // Apply remote to local
                    return SYNC_ACTION_UPDATE_LOCAL; // Returning action to update local
                }
            } else {
                // Validate gtask id
                if (!c.getString(SqlNote.GTASK_ID_COLUMN).equals(getGid())) {
                    Log.e(TAG, "gtask id doesn't match"); // Logging an error if gtask id doesn't match
                    return SYNC_ACTION_ERROR; // Returning an error action
                }
                if (c.getLong(SqlNote.SYNC_ID_COLUMN) == getLastModified()) {
                    // Local modification only
                    return SYNC_ACTION_UPDATE_REMOTE; // Returning action to update remote
                } else {
                    return SYNC_ACTION_UPDATE_CONFLICT; // Returning action to handle update conflict
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString()); // Logging any exception occurred
            e.printStackTrace(); // Printing the stack trace of the exception
        }

        return SYNC_ACTION_ERROR; // Returning an error action if there's an exception
    }


    public boolean isWorthSaving() {
        return mMetaInfo != null || (getName() != null && getName().trim().length() > 0)
                || (getNotes() != null && getNotes().trim().length() > 0);
    }

    public void setCompleted(boolean completed) {
        this.mCompleted = completed;
    }

    public void setNotes(String notes) {
        this.mNotes = notes;
    }

    public void setPriorSibling(Task priorSibling) {
        this.mPriorSibling = priorSibling;
    }

    public void setParent(TaskList parent) {
        this.mParent = parent;
    }

    public boolean getCompleted() {
        return this.mCompleted;
    }

    public String getNotes() {
        return this.mNotes;
    }

    public Task getPriorSibling() {
        return this.mPriorSibling;
    }

    public TaskList getParent() {
        return this.mParent;
    }

}
