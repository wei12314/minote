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

import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.exception.ActionFailureException;
import net.micode.notes.tool.GTaskStringUtils;
import net.micode.notes.tool.ResourceParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class SqlNote {
    // Declares a private constant 'TAG' to store the class's simple name
    private static final String TAG = SqlNote.class.getSimpleName();
    // Declares a constant 'INVALID_ID' initialized with -99999
    private static final int INVALID_ID = -99999;
    // Defines an array of strings 'PROJECTION_NOTE' for database column names
    public static final String[] PROJECTION_NOTE = new String[] {
            NoteColumns.ID, NoteColumns.ALERTED_DATE, NoteColumns.BG_COLOR_ID,
            NoteColumns.CREATED_DATE, NoteColumns.HAS_ATTACHMENT, NoteColumns.MODIFIED_DATE,
            NoteColumns.NOTES_COUNT, NoteColumns.PARENT_ID, NoteColumns.SNIPPET, NoteColumns.TYPE,
            NoteColumns.WIDGET_ID, NoteColumns.WIDGET_TYPE, NoteColumns.SYNC_ID,
            NoteColumns.LOCAL_MODIFIED, NoteColumns.ORIGIN_PARENT_ID, NoteColumns.GTASK_ID,
            NoteColumns.VERSION
    };
    // Constants for column indices in the projection array
    public static final int ID_COLUMN = 0;

    public static final int ALERTED_DATE_COLUMN = 1;

    public static final int BG_COLOR_ID_COLUMN = 2;

    public static final int CREATED_DATE_COLUMN = 3;

    public static final int HAS_ATTACHMENT_COLUMN = 4;

    public static final int MODIFIED_DATE_COLUMN = 5;

    public static final int NOTES_COUNT_COLUMN = 6;

    public static final int PARENT_ID_COLUMN = 7;

    public static final int SNIPPET_COLUMN = 8;

    public static final int TYPE_COLUMN = 9;

    public static final int WIDGET_ID_COLUMN = 10;

    public static final int WIDGET_TYPE_COLUMN = 11;

    public static final int SYNC_ID_COLUMN = 12;

    public static final int LOCAL_MODIFIED_COLUMN = 13;

    public static final int ORIGIN_PARENT_ID_COLUMN = 14;

    public static final int GTASK_ID_COLUMN = 15;

    public static final int VERSION_COLUMN = 16;

    // Member variables representing note properties
    private Context mContext;

    private ContentResolver mContentResolver;

    private boolean mIsCreate;

    private long mId;

    private long mAlertDate;

    private int mBgColorId;

    private long mCreatedDate;

    private int mHasAttachment;

    private long mModifiedDate;

    private long mParentId;

    private String mSnippet;

    private int mType;

    private int mWidgetId;

    private int mWidgetType;

    private long mOriginParent;

    private long mVersion;
    // ContentValues to store differences in note values
    private ContentValues mDiffNoteValues;
    // ArrayList to store SqlData objects related to this note
    private ArrayList<SqlData> mDataList;
    // constructor of this class
    public SqlNote(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = true;
        mId = INVALID_ID;
        mAlertDate = 0;
        mBgColorId = ResourceParser.getDefaultBgId(context);
        mCreatedDate = System.currentTimeMillis();
        mHasAttachment = 0;
        mModifiedDate = System.currentTimeMillis();
        mParentId = 0;
        mSnippet = "";
        mType = Notes.TYPE_NOTE;
        mWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        mWidgetType = Notes.TYPE_WIDGET_INVALIDE;
        mOriginParent = 0;
        mVersion = 0;
        mDiffNoteValues = new ContentValues();
        mDataList = new ArrayList<SqlData>();
    }

    public SqlNote(Context context, Cursor c) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = false;
        loadFromCursor(c);
        mDataList = new ArrayList<SqlData>();
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();
        mDiffNoteValues = new ContentValues();
    }

    public SqlNote(Context context, long id) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mIsCreate = false;
        loadFromCursor(id);
        mDataList = new ArrayList<SqlData>();
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();
        mDiffNoteValues = new ContentValues();

    }

    private void loadFromCursor(long id) {
        // Declares and initializes a Cursor variable
        Cursor c = null;
        try {
            // Queries the ContentResolver to retrieve data from the specified URI based on the 'id'
            c = mContentResolver.query(
                Notes.CONTENT_NOTE_URI,  // URI to query
                PROJECTION_NOTE,         // Projection (list of columns to retrieve)
                "(_id=?)",               // Selection criteria
                new String[] {           // Selection arguments (the 'id')
                    String.valueOf(id)
                },
                null                     // No group by
            );

            // Checks if the cursor is not null and moves to the first result
            if (c != null) {
                c.moveToNext();
                // Calls another method 'loadFromCursor' passing the Cursor as an argument
                loadFromCursor(c);
            } else {
                // Logs a warning if the cursor is null
                Log.w(TAG, "loadFromCursor: cursor = null");
            }
        } finally {
            // Ensures the Cursor is closed to release resources
            if (c != null)
                c.close();
        }
    }


    private void loadFromCursor(Cursor c) {
        // Retrieves values from the Cursor and assigns them to respective member variables
        mId = c.getLong(ID_COLUMN);
        mAlertDate = c.getLong(ALERTED_DATE_COLUMN);
        mBgColorId = c.getInt(BG_COLOR_ID_COLUMN);
        mCreatedDate = c.getLong(CREATED_DATE_COLUMN);
        mHasAttachment = c.getInt(HAS_ATTACHMENT_COLUMN);
        mModifiedDate = c.getLong(MODIFIED_DATE_COLUMN);
        mParentId = c.getLong(PARENT_ID_COLUMN);
        mSnippet = c.getString(SNIPPET_COLUMN);
        mType = c.getInt(TYPE_COLUMN);
        mWidgetId = c.getInt(WIDGET_ID_COLUMN);
        mWidgetType = c.getInt(WIDGET_TYPE_COLUMN);
        mVersion = c.getLong(VERSION_COLUMN);
    }

    private void loadDataContent() {
        // Initializes a Cursor variable
        Cursor c = null;
        // Clears the existing list of SqlData objects
        mDataList.clear();
        try {
            // Queries the ContentResolver to retrieve data related to 'mId' from the CONTENT_DATA_URI
            c = mContentResolver.query(
                Notes.CONTENT_DATA_URI,      // URI to query
                SqlData.PROJECTION_DATA,     // Projection (list of columns to retrieve)
                "(note_id=?)",               // Selection criteria
                new String[] {               // Selection arguments (the 'mId')
                    String.valueOf(mId)
                },
                null                         // No group by
            );

            // Checks if the cursor is not null and has data
            if (c != null) {
                if (c.getCount() == 0) {
                    // Logs a warning if no data is found for the note
                    Log.w(TAG, "it seems that the note has no data");
                    return;
                }
                // Iterates through the cursor results
                while (c.moveToNext()) {
                    // Creates a new SqlData object using data from the cursor and adds it to the list
                    SqlData data = new SqlData(mContext, c);
                    mDataList.add(data);
                }
            } else {
                // Logs a warning if the cursor is null
                Log.w(TAG, "loadDataContent: cursor = null");
            }
        } finally {
            // Ensures the Cursor is closed to release associated resources
            if (c != null)
                c.close();
        }
    }

    public boolean setContent(JSONObject js) {
        try {
            // Retrieves the 'note' object from the JSONObject
            JSONObject note = js.getJSONObject(GTaskStringUtils.META_HEAD_NOTE);

            // Handles different types of notes based on their 'TYPE' field
            if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_SYSTEM) {
                Log.w(TAG, "cannot set system folder");
            } else if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_FOLDER) {
                // Processes folder type note: updates snippet and type
                // Retrieves snippet and type values from 'note'
                String snippet = note.has(NoteColumns.SNIPPET) ? note.getString(NoteColumns.SNIPPET) : "";
                if (mIsCreate || !mSnippet.equals(snippet)) {
                    mDiffNoteValues.put(NoteColumns.SNIPPET, snippet);
                }
                mSnippet = snippet;

                int type = note.has(NoteColumns.TYPE) ? note.getInt(NoteColumns.TYPE) : Notes.TYPE_NOTE;
                if (mIsCreate || mType != type) {
                    mDiffNoteValues.put(NoteColumns.TYPE, type);
                }
                mType = type;
            } else if (note.getInt(NoteColumns.TYPE) == Notes.TYPE_NOTE) {
                // Processes note type: updates various note attributes
                JSONArray dataArray = js.getJSONArray(GTaskStringUtils.META_HEAD_DATA);

                // Retrieves various note attributes from the 'note' JSONObject
                // and updates respective member variables if they have changed
                long id = note.has(NoteColumns.ID) ? note.getLong(NoteColumns.ID) : INVALID_ID;
                if (mIsCreate || mId != id) {
                    mDiffNoteValues.put(NoteColumns.ID, id);
                }
                mId = id;

                // Retrieves various note attributes from the 'note' JSONObject
                // and updates respective member variables if they have changed
                long alertDate = note.has(NoteColumns.ALERTED_DATE) ? note
                        .getLong(NoteColumns.ALERTED_DATE) : 0;
                if (mIsCreate || mAlertDate != alertDate) {
                    mDiffNoteValues.put(NoteColumns.ALERTED_DATE, alertDate);
                }
                mAlertDate = alertDate;

                // Retrieves various note attributes from the 'note' JSONObject
                // and updates respective member variables if they have changed
                int bgColorId = note.has(NoteColumns.BG_COLOR_ID) ? note
                        .getInt(NoteColumns.BG_COLOR_ID) : ResourceParser.getDefaultBgId(mContext);
                if (mIsCreate || mBgColorId != bgColorId) {
                    mDiffNoteValues.put(NoteColumns.BG_COLOR_ID, bgColorId);
                }
                mBgColorId = bgColorId;

                // Retrieves various note attributes from the 'note' JSONObject
                // and updates respective member variables if they have changed
                long createDate = note.has(NoteColumns.CREATED_DATE) ? note
                        .getLong(NoteColumns.CREATED_DATE) : System.currentTimeMillis();
                if (mIsCreate || mCreatedDate != createDate) {
                    mDiffNoteValues.put(NoteColumns.CREATED_DATE, createDate);
                }
                mCreatedDate = createDate;

                // Retrieves various note attributes from the 'note' JSONObject
                // and updates respective member variables if they have changed
                int hasAttachment = note.has(NoteColumns.HAS_ATTACHMENT) ? note
                        .getInt(NoteColumns.HAS_ATTACHMENT) : 0;
                if (mIsCreate || mHasAttachment != hasAttachment) {
                    mDiffNoteValues.put(NoteColumns.HAS_ATTACHMENT, hasAttachment);
                }
                mHasAttachment = hasAttachment;

                // Retrieves various note attributes from the 'note' JSONObject
                // and updates respective member variables if they have changed
                long modifiedDate = note.has(NoteColumns.MODIFIED_DATE) ? note
                        .getLong(NoteColumns.MODIFIED_DATE) : System.currentTimeMillis();
                if (mIsCreate || mModifiedDate != modifiedDate) {
                    mDiffNoteValues.put(NoteColumns.MODIFIED_DATE, modifiedDate);
                }
                mModifiedDate = modifiedDate;

                // Retrieves various note attributes from the 'note' JSONObject
                // and updates respective member variables if they have changed
                long parentId = note.has(NoteColumns.PARENT_ID) ? note
                        .getLong(NoteColumns.PARENT_ID) : 0;
                if (mIsCreate || mParentId != parentId) {
                    mDiffNoteValues.put(NoteColumns.PARENT_ID, parentId);
                }
                mParentId = parentId;

                // Retrieves various note attributes from the 'note' JSONObject
                // and updates respective member variables if they have changed
                String snippet = note.has(NoteColumns.SNIPPET) ? note
                        .getString(NoteColumns.SNIPPET) : "";
                if (mIsCreate || !mSnippet.equals(snippet)) {
                    mDiffNoteValues.put(NoteColumns.SNIPPET, snippet);
                }
                mSnippet = snippet;

                // Retrieves various note attributes from the 'note' JSONObject
                // and updates respective member variables if they have changed
                int type = note.has(NoteColumns.TYPE) ? note.getInt(NoteColumns.TYPE)
                        : Notes.TYPE_NOTE;
                if (mIsCreate || mType != type) {
                    mDiffNoteValues.put(NoteColumns.TYPE, type);
                }
                mType = type;

                int widgetId = note.has(NoteColumns.WIDGET_ID) ? note.getInt(NoteColumns.WIDGET_ID)
                        : AppWidgetManager.INVALID_APPWIDGET_ID;
                if (mIsCreate || mWidgetId != widgetId) {
                    mDiffNoteValues.put(NoteColumns.WIDGET_ID, widgetId);
                }
                mWidgetId = widgetId;

                int widgetType = note.has(NoteColumns.WIDGET_TYPE) ? note
                        .getInt(NoteColumns.WIDGET_TYPE) : Notes.TYPE_WIDGET_INVALIDE;
                if (mIsCreate || mWidgetType != widgetType) {
                    mDiffNoteValues.put(NoteColumns.WIDGET_TYPE, widgetType);
                }
                mWidgetType = widgetType;

                long originParent = note.has(NoteColumns.ORIGIN_PARENT_ID) ? note
                        .getLong(NoteColumns.ORIGIN_PARENT_ID) : 0;
                if (mIsCreate || mOriginParent != originParent) {
                    mDiffNoteValues.put(NoteColumns.ORIGIN_PARENT_ID, originParent);
                }
                mOriginParent = originParent;

                // Iterates through the 'dataArray' to process associated data for the note
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject data = dataArray.getJSONObject(i);
                    SqlData sqlData = null;
                    // Finds existing SqlData object or creates a new one for the data
                    if (data.has(DataColumns.ID)) {
                        long dataId = data.getLong(DataColumns.ID);
                        for (SqlData temp : mDataList) {
                            if (dataId == temp.getId()) {
                                sqlData = temp;
                            }
                        }
                    }
                    // Adds the data to SqlData objects or creates new SqlData objects
                    if (sqlData == null) {
                        sqlData = new SqlData(mContext);
                        mDataList.add(sqlData);
                    }
                    // Sets content for the SqlData object
                    sqlData.setContent(data);
                }
            }
        } catch (JSONException e) {
            // Logs any exceptions and returns false in case of JSON parsing errors
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public JSONObject getContent() {
        try {
            JSONObject js = new JSONObject();

            if (mIsCreate) {
                // If the note is in creation mode, logs an error and returns null
                Log.e(TAG, "it seems that we haven't created this in database yet");
                return null;
            }

            // Creates a JSON object 'note' to hold note-related data based on its type
            JSONObject note = new JSONObject();
            if (mType == Notes.TYPE_NOTE) {
                // Populates 'note' object with note attributes for a note type
                note.put(NoteColumns.ID, mId);
                note.put(NoteColumns.ALERTED_DATE, mAlertDate);
                note.put(NoteColumns.BG_COLOR_ID, mBgColorId);
                note.put(NoteColumns.CREATED_DATE, mCreatedDate);
                note.put(NoteColumns.HAS_ATTACHMENT, mHasAttachment);
                note.put(NoteColumns.MODIFIED_DATE, mModifiedDate);
                note.put(NoteColumns.PARENT_ID, mParentId);
                note.put(NoteColumns.SNIPPET, mSnippet);
                note.put(NoteColumns.TYPE, mType);
                note.put(NoteColumns.WIDGET_ID, mWidgetId);
                note.put(NoteColumns.WIDGET_TYPE, mWidgetType);
                note.put(NoteColumns.ORIGIN_PARENT_ID, mOriginParent);
                // Adds 'note' object to the main JSON object 'js'
                js.put(GTaskStringUtils.META_HEAD_NOTE, note);

                // Creates a JSON array 'dataArray' for associated data (SqlData) for the note
                JSONArray dataArray = new JSONArray();
                for (SqlData sqlData : mDataList) {
                    JSONObject data = sqlData.getContent();
                    if (data != null) {
                        dataArray.put(data);
                    }
                }
                // Adds 'dataArray' to the main JSON object 'js'
                js.put(GTaskStringUtils.META_HEAD_DATA, dataArray);
            } else if (mType == Notes.TYPE_FOLDER || mType == Notes.TYPE_SYSTEM) {
                // Handles folder or system type notes
                // Populates 'note' object with attributes for folder or system type
                note.put(NoteColumns.ID, mId);
                note.put(NoteColumns.TYPE, mType);
                note.put(NoteColumns.SNIPPET, mSnippet);

                // Adds 'note' object to the main JSON object 'js'
                js.put(GTaskStringUtils.META_HEAD_NOTE, note);
            }

            return js;
        } catch (JSONException e) {
            // Handles JSON parsing exceptions: logs error and returns null
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        return null; // Returns null in case of exceptions
    }

    // multiple setter and getter for this class fields
    public void setParentId(long id) {
        mParentId = id;
        mDiffNoteValues.put(NoteColumns.PARENT_ID, id);
    }

    public void setGtaskId(String gid) {
        mDiffNoteValues.put(NoteColumns.GTASK_ID, gid);
    }

    public void setSyncId(long syncId) {
        mDiffNoteValues.put(NoteColumns.SYNC_ID, syncId);
    }

    public void resetLocalModified() {
        mDiffNoteValues.put(NoteColumns.LOCAL_MODIFIED, 0);
    }

    public long getId() {
        return mId;
    }

    public long getParentId() {
        return mParentId;
    }

    public String getSnippet() {
        return mSnippet;
    }

    public boolean isNoteType() {
        return mType == Notes.TYPE_NOTE;
    }

    public void commit(boolean validateVersion) {
        // If a new note is being created
        if (mIsCreate) {
            // Checking if the ID is invalid and if the diff values contain an ID, then remove it
            if (mId == INVALID_ID && mDiffNoteValues.containsKey(NoteColumns.ID)) {
                mDiffNoteValues.remove(NoteColumns.ID);
            }

            // Insert the new note into the database
            Uri uri = mContentResolver.insert(Notes.CONTENT_NOTE_URI, mDiffNoteValues);
            
            // Obtain the ID of the newly created note
            try {
                mId = Long.valueOf(uri.getPathSegments().get(1));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Get note id error :" + e.toString());
                throw new ActionFailureException("create note failed");
            }
            
            // If the ID is 0, throw an IllegalStateException indicating failure in creating a thread ID
            if (mId == 0) {
                throw new IllegalStateException("Create thread id failed");
            }

            // If the type of note is a regular note, commit associated data (SqlData)
            if (mType == Notes.TYPE_NOTE) {
                for (SqlData sqlData : mDataList) {
                    sqlData.commit(mId, false, -1);
                }
            }
        } else {
            // If the note is not newly created but being updated

            // Check for valid note IDs, excluding root folder and call record folder
            if (mId <= 0 && mId != Notes.ID_ROOT_FOLDER && mId != Notes.ID_CALL_RECORD_FOLDER) {
                Log.e(TAG, "No such note");
                throw new IllegalStateException("Try to update note with invalid id");
            }

            // If there are differences in note values, update the version and perform the update in the database
            if (mDiffNoteValues.size() > 0) {
                mVersion++;
                int result = 0;
                
                // Update the note in the database based on conditions and version validation
                if (!validateVersion) {
                    result = mContentResolver.update(Notes.CONTENT_NOTE_URI, mDiffNoteValues, "("
                            + NoteColumns.ID + "=?)", new String[]{
                            String.valueOf(mId)
                    });
                } else {
                    result = mContentResolver.update(Notes.CONTENT_NOTE_URI, mDiffNoteValues, "("
                            + NoteColumns.ID + "=?) AND (" + NoteColumns.VERSION + "<=?)",
                            new String[]{
                                    String.valueOf(mId), String.valueOf(mVersion)
                            });
                }
                
                // If the update result is 0, log a warning indicating no updates occurred
                if (result == 0) {
                    Log.w(TAG, "there is no update. maybe user updates note when syncing");
                }
            }

            // If the note type is a regular note, commit associated data (SqlData)
            if (mType == Notes.TYPE_NOTE) {
                for (SqlData sqlData : mDataList) {
                    sqlData.commit(mId, validateVersion, mVersion);
                }
            }
        }

        // Refresh local information by loading data from the cursor and data content if it's a regular note
        loadFromCursor(mId);
        if (mType == Notes.TYPE_NOTE)
            loadDataContent();

        // Clear the temporary diff values and set the creation flag to false
        mDiffNoteValues.clear();
        mIsCreate = false;
    }

}
