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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.NotesDatabaseHelper.TABLE;
import net.micode.notes.gtask.exception.ActionFailureException;

import org.json.JSONException;
import org.json.JSONObject;


// Defines a class named 'SqlData'
public class SqlData {
    // Declares a private constant 'TAG' to store the class's simple name
    private static final String TAG = SqlData.class.getSimpleName();

    // Declares a constant 'INVALID_ID' initialized with -99999
    private static final int INVALID_ID = -99999;

    // Defines an array of strings 'PROJECTION_DATA' for database column names
    public static final String[] PROJECTION_DATA = new String[] {
            DataColumns.ID, DataColumns.MIME_TYPE, DataColumns.CONTENT, DataColumns.DATA1,
            DataColumns.DATA3
    };

    // Constants for column indices in the projection array
    public static final int DATA_ID_COLUMN = 0;

    public static final int DATA_MIME_TYPE_COLUMN = 1;

    public static final int DATA_CONTENT_COLUMN = 2;

    public static final int DATA_CONTENT_DATA_1_COLUMN = 3;

    public static final int DATA_CONTENT_DATA_3_COLUMN = 4;

    // Private member variables
    private ContentResolver mContentResolver;

    private boolean mIsCreate;

    private long mDataId;

    private String mDataMimeType;

    private String mDataContent;

    private long mDataContentData1;

    private String mDataContentData3;

    private ContentValues mDiffDataValues;

    // Constructor initializing member variables
    public SqlData(Context context) {
        // Initializes ContentResolver with the provided context
        mContentResolver = context.getContentResolver();
        // Sets 'mIsCreate' to true
        mIsCreate = true;
        // Initializes data-related variables with default values
        mDataId = INVALID_ID;
        mDataMimeType = DataConstants.NOTE;
        mDataContent = "";
        mDataContentData1 = 0;
        mDataContentData3 = "";
         // Initializes ContentValues to store differences in data
        mDiffDataValues = new ContentValues();
    }

    // Constructor initializing member variables from a Cursor object
    public SqlData(Context context, Cursor c) {
        // Initializes ContentResolver with the provided context
        mContentResolver = context.getContentResolver();
        // Sets 'mIsCreate' to false
        mIsCreate = false;
        // Loads data from the provided Cursor
        loadFromCursor(c);
        // Initializes ContentValues to store differences in data
        mDiffDataValues = new ContentValues();
    }

    // Method to load data from a Cursor object
    private void loadFromCursor(Cursor c) {
        // Retrieves data from the Cursor and assigns it to respective member variables
        mDataId = c.getLong(DATA_ID_COLUMN);
        mDataMimeType = c.getString(DATA_MIME_TYPE_COLUMN);
        mDataContent = c.getString(DATA_CONTENT_COLUMN);
        mDataContentData1 = c.getLong(DATA_CONTENT_DATA_1_COLUMN);
        mDataContentData3 = c.getString(DATA_CONTENT_DATA_3_COLUMN);
    }

// Method to set content based on a JSONObject
    public void setContent(JSONObject js) throws JSONException {
        // Retrieves and sets values from the JSONObject to respective member variables
        // Also stores differences in mDiffDataValues for potential database updates
        // if there are changes compared to existing data

        // Retrieves the 'ID' field from the JSONObject 'js', or assigns 'INVALID_ID' if not present
        long dataId = js.has(DataColumns.ID) ? js.getLong(DataColumns.ID) : INVALID_ID;
        // Checks if it's a new creation or if 'mDataId' is different from 'dataId'
        if (mIsCreate || mDataId != dataId) {
            // Stores 'ID' in 'mDiffDataValues' if it's a new creation or there's a change
            mDiffDataValues.put(DataColumns.ID, dataId);
        }
        // Updates 'mDataId' with the retrieved 'dataId'
        mDataId = dataId;

        // Retrieves 'MIME_TYPE' field from 'js' or assigns 'DataConstants.NOTE' if not present
        String dataMimeType = js.has(DataColumns.MIME_TYPE) ? js.getString(DataColumns.MIME_TYPE)
                : DataConstants.NOTE;
        // Checks if it's a new creation or if 'mDataMimeType' is different from 'dataMimeType'
        if (mIsCreate || !mDataMimeType.equals(dataMimeType)) {
            // Stores 'MIME_TYPE' in 'mDiffDataValues' if it's a new creation or there's a change
            mDiffDataValues.put(DataColumns.MIME_TYPE, dataMimeType);
        }
        // Updates 'mDataMimeType' with the retrieved 'dataMimeType'
        mDataMimeType = dataMimeType;

        // Retrieves 'CONTENT' field from 'js' or assigns an empty string if not present
        String dataContent = js.has(DataColumns.CONTENT) ? js.getString(DataColumns.CONTENT) : "";
        // Checks if it's a new creation or if 'mDataContent' is different from 'dataContent'
        if (mIsCreate || !mDataContent.equals(dataContent)) {
            // Stores 'CONTENT' in 'mDiffDataValues' if it's a new creation or there's a change
            mDiffDataValues.put(DataColumns.CONTENT, dataContent);
        }
        // Updates 'mDataContent' with the retrieved 'dataContent'
        mDataContent = dataContent;

        // Retrieves 'DATA1' field from 'js' or assigns '0' if not present
        long dataContentData1 = js.has(DataColumns.DATA1) ? js.getLong(DataColumns.DATA1) : 0;
        // Checks if it's a new creation or if 'mDataContentData1' is different from 'dataContentData1'
        if (mIsCreate || mDataContentData1 != dataContentData1) {
            // Stores 'DATA1' in 'mDiffDataValues' if it's a new creation or there's a change
            mDiffDataValues.put(DataColumns.DATA1, dataContentData1);
        }
        // Updates 'mDataContentData1' with the retrieved 'dataContentData1'
        mDataContentData1 = dataContentData1;

        // Retrieves 'DATA3' field from 'js' or assigns an empty string if not present
        String dataContentData3 = js.has(DataColumns.DATA3) ? js.getString(DataColumns.DATA3) : "";
        // Checks if it's a new creation or if 'mDataContentData3' is different from 'dataContentData3'
        if (mIsCreate || !mDataContentData3.equals(dataContentData3)) {
            // Stores 'DATA3' in 'mDiffDataValues' if it's a new creation or there's a change
            mDiffDataValues.put(DataColumns.DATA3, dataContentData3);
        }
        // Updates 'mDataContentData3' with the retrieved 'dataContentData3'
        mDataContentData3 = dataContentData3;
    }


    // Method to retrieve content as a JSONObject
    public JSONObject getContent() throws JSONException {
        // Creates and returns a JSONObject with data from member variables
        if (mIsCreate) {
            Log.e(TAG, "it seems that we haven't created this in database yet");
            return null;
        }
        JSONObject js = new JSONObject();
        js.put(DataColumns.ID, mDataId);
        js.put(DataColumns.MIME_TYPE, mDataMimeType);
        js.put(DataColumns.CONTENT, mDataContent);
        js.put(DataColumns.DATA1, mDataContentData1);
        js.put(DataColumns.DATA3, mDataContentData3);
        return js;
    }

    // Method to commit changes to the database
    public void commit(long noteId, boolean validateVersion, long version) {
        // Commits changes to the database based on conditions

        if (mIsCreate) {
            // Checks if it's a new data creation
            if (mDataId == INVALID_ID && mDiffDataValues.containsKey(DataColumns.ID)) {
                // Removes 'ID' if it's invalid and exists in 'mDiffDataValues'
                mDiffDataValues.remove(DataColumns.ID);
            }

            // Sets 'NOTE_ID' in 'mDiffDataValues' with the provided 'noteId'
            mDiffDataValues.put(DataColumns.NOTE_ID, noteId);
            // Inserts data into the database and retrieves the Uri
            Uri uri = mContentResolver.insert(Notes.CONTENT_DATA_URI, mDiffDataValues);
            try {
                // Retrieves the inserted 'mDataId' from the Uri path
                mDataId = Long.valueOf(uri.getPathSegments().get(1));
            } catch (NumberFormatException e) {
                // Handles errors related to getting the note ID
                Log.e(TAG, "Get note id error :" + e.toString());
                throw new ActionFailureException("create note failed");
            }
        } else {
            // Handles data update if it's not a new creation
            if (mDiffDataValues.size() > 0) {
                int result = 0;
                // Checks if version validation is required before updating
                if (!validateVersion) {
                    // Updates data in the database based on mDataId without version validation
                    result = mContentResolver.update(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mDataId), mDiffDataValues, null, null);
                } else {
                    // Updates data with version validation using mDataId, noteId, and version
                    result = mContentResolver.update(ContentUris.withAppendedId(
                            Notes.CONTENT_DATA_URI, mDataId), mDiffDataValues,
                            " ? in (SELECT " + NoteColumns.ID + " FROM " + TABLE.NOTE
                                    + " WHERE " + NoteColumns.VERSION + "=?)", new String[] {
                                    String.valueOf(noteId), String.valueOf(version)
                            });
                }
                // Logs a warning if the update result is zero
                if (result == 0) {
                    Log.w(TAG, "there is no update. maybe user updates note when syncing");
                }
            }
        }

    // Clears 'mDiffDataValues' after database operation
    mDiffDataValues.clear();
    // Resets 'mIsCreate' to false after committing changes
    mIsCreate = false;
}


    public long getId() {
        return mDataId;
    }
}
