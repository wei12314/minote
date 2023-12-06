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

package net.micode.notes.data;


import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import net.micode.notes.R;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.data.NotesDatabaseHelper.TABLE;

// Content provider for notes 
public class NotesProvider extends ContentProvider {
    // Content provider for notes 
    private static final UriMatcher mMatcher;
    // Database helper 
    private NotesDatabaseHelper mHelper;
    // Tag for logging  
    private static final String TAG = "NotesProvider";
    // URI match codes
    private static final int URI_NOTE            = 1;
    private static final int URI_NOTE_ITEM       = 2;
    private static final int URI_DATA            = 3;
    private static final int URI_DATA_ITEM       = 4;

    private static final int URI_SEARCH          = 5;
    private static final int URI_SEARCH_SUGGEST  = 6;
    // Initialize URI matcher
    static {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mMatcher.addURI(Notes.AUTHORITY, "note", URI_NOTE);
        mMatcher.addURI(Notes.AUTHORITY, "note/#", URI_NOTE_ITEM);
        mMatcher.addURI(Notes.AUTHORITY, "data", URI_DATA);
        mMatcher.addURI(Notes.AUTHORITY, "data/#", URI_DATA_ITEM);
        mMatcher.addURI(Notes.AUTHORITY, "search", URI_SEARCH);
        mMatcher.addURI(Notes.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, URI_SEARCH_SUGGEST);
        mMatcher.addURI(Notes.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", URI_SEARCH_SUGGEST);
    }

    /**
     * x'0A' represents the '\n' character in sqlite. For title and content in the search result,
     * we will trim '\n' and white space in order to show more information.
     */
    private static final String NOTES_SEARCH_PROJECTION = NoteColumns.ID + ","
        + NoteColumns.ID + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA + ","
        + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS " + SearchManager.SUGGEST_COLUMN_TEXT_1 + ","
        + "TRIM(REPLACE(" + NoteColumns.SNIPPET + ", x'0A','')) AS " + SearchManager.SUGGEST_COLUMN_TEXT_2 + ","
        + R.drawable.search_result + " AS " + SearchManager.SUGGEST_COLUMN_ICON_1 + ","
        + "'" + Intent.ACTION_VIEW + "' AS " + SearchManager.SUGGEST_COLUMN_INTENT_ACTION + ","
        + "'" + Notes.TextNote.CONTENT_TYPE + "' AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA;

    private static String NOTES_SNIPPET_SEARCH_QUERY = "SELECT " + NOTES_SEARCH_PROJECTION
        + " FROM " + TABLE.NOTE
        + " WHERE " + NoteColumns.SNIPPET + " LIKE ?"
        + " AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER
        + " AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE;

    @Override
    public boolean onCreate() {
        mHelper = NotesDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // Get readable database 
        Cursor c = null;
        SQLiteDatabase db = mHelper.getReadableDatabase();
        // URI match ID  
        String id = null;
        // Switch on URI matcher result
        switch (mMatcher.match(uri)) {
            // Case: All notes query 
            case URI_NOTE:
                c = db.query(TABLE.NOTE, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            // Case: Single note query
            case URI_NOTE_ITEM:
                // Get note ID from URI path 
                id = uri.getPathSegments().get(1);
                // Append ID to user selection
                c = db.query(TABLE.NOTE, projection, NoteColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            // Case: All data query
            case URI_DATA:
                c = db.query(TABLE.DATA, projection, selection, selectionArgs, null, null,
                        sortOrder);
                break;
            // Case: Single data query  
            case URI_DATA_ITEM:
                // Get data ID from URI path
                id = uri.getPathSegments().get(1);
                // Append ID to user selection
                c = db.query(TABLE.DATA, projection, DataColumns.ID + "=" + id
                        + parseSelection(selection), selectionArgs, null, null, sortOrder);
                break;
            // Search cases 
            case URI_SEARCH:
            case URI_SEARCH_SUGGEST:
                // Validate search query format
                if (sortOrder != null || projection != null) {
                    throw new IllegalArgumentException(
                            "do not specify sortOrder, selection, selectionArgs, or projection" + "with this query");
                }
                // Get search query term 
                String searchString = null;
                if (mMatcher.match(uri) == URI_SEARCH_SUGGEST) {
                    if (uri.getPathSegments().size() > 1) {
                        searchString = uri.getPathSegments().get(1);
                    }
                } else {
                    searchString = uri.getQueryParameter("pattern");
                }

                if (TextUtils.isEmpty(searchString)) {
                    return null;
                }

                try {
                    // Format search query 
                    searchString = String.format("%%%s%%", searchString);
                    // Execute snippet search query
                    c = db.rawQuery(NOTES_SNIPPET_SEARCH_QUERY,
                            new String[] { searchString });
                } catch (IllegalStateException ex) {
                    Log.e(TAG, "got exception: " + ex.toString());
                }
                break;
            // Unknown URI 
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        // Set notification URI
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // get writable database
        SQLiteDatabase db = mHelper.getWritableDatabase();
        // Track inserted IDs
        long dataId = 0, noteId = 0, insertedId = 0;
        // Switch on URI match 
        switch (mMatcher.match(uri)) {
            // Case: Insert note
            case URI_NOTE:
                // Insert note and save ID 
                insertedId = noteId = db.insert(TABLE.NOTE, null, values);
                break;
            // Case: Insert data
            case URI_DATA:
                // Check if note ID provided
                if (values.containsKey(DataColumns.NOTE_ID)) {
                    // Get note ID
                    noteId = values.getAsLong(DataColumns.NOTE_ID);
                } else {
                    // Log error if missing
                    Log.d(TAG, "Wrong data format without note id:" + values.toString());
                }

                // Insert data and save ID
                insertedId = dataId = db.insert(TABLE.DATA, null, values);
                break;
            // Unknown URI
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        // Notify the note uri
        if (noteId > 0) {
            getContext().getContentResolver().notifyChange(
                    ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId), null);
        }

        // Notify the data uri
        if (dataId > 0) {
            getContext().getContentResolver().notifyChange(
                    ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, dataId), null);
        }
        // Return URI with inserted ID
        return ContentUris.withAppendedId(uri, insertedId);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Track deleted rows count
        int count = 0;
        // URI match ID
        String id = null;
        // Get writable database
        SQLiteDatabase db = mHelper.getWritableDatabase();
        // Flag if deleting data
        boolean deleteData = false;
        // Switch on URI match
        switch (mMatcher.match(uri)) {
            // Case: Delete notes
            case URI_NOTE:
                // append id check to selection 
                selection = "(" + selection + ") AND " + NoteColumns.ID + ">0 ";
                // delte matching row
                count = db.delete(TABLE.NOTE, selection, selectionArgs);
                break;
            // case: delete single note item
            case URI_NOTE_ITEM:
                // get note id from uri
                id = uri.getPathSegments().get(1);
                /**
                 * ID that smaller than 0 is system folder which is not allowed to
                 * trash
                 */
                // check for system folder
                long noteId = Long.valueOf(id);
                if (noteId <= 0) {
                    break;
                }
                // delete note by id
                count = db.delete(TABLE.NOTE,
                        NoteColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                break;
            // delete data
            case URI_DATA:
                // delete matching row
                count = db.delete(TABLE.DATA, selection, selectionArgs);
                deleteData = true;
                break;
            // delete single data item
            case URI_DATA_ITEM:
                // get id from uri
                id = uri.getPathSegments().get(1);
                // delete data by id
                count = db.delete(TABLE.DATA,
                        DataColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                deleteData = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        // Notify related URIs
        if (count > 0) {
            if (deleteData) {
                getContext().getContentResolver().notifyChange(Notes.CONTENT_NOTE_URI, null);
            }
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    // Declares a method named 'update' that returns an integer and takes in parameters: Uri, ContentValues, String, and String[]
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Initializes a variable 'count' to 0
        int count = 0;
        // Initializes a String variable 'id' to null
        String id = null;
        // Obtains a reference to a SQLiteDatabase by getting a writable database from mHelper
        SQLiteDatabase db = mHelper.getWritableDatabase();
        // Initializes a boolean variable 'updateData' to false
        boolean updateData = false;
        
        // Begins a switch statement based on the match of the provided 'uri' using 'mMatcher'
        switch (mMatcher.match(uri)) {
            // Case where the URI matches URI_NOTE
            case URI_NOTE:
                // Calls the 'increaseNoteVersion' method with parameters: -1, selection, and selectionArgs
                increaseNoteVersion(-1, selection, selectionArgs);
                // Performs an update operation on the database TABLE.NOTE using provided 'values', 'selection', and 'selectionArgs'
                count = db.update(TABLE.NOTE, values, selection, selectionArgs);
                break;
            
            // Case where the URI matches URI_NOTE_ITEM
            case URI_NOTE_ITEM:
                // Retrieves the 'id' from the URI's path segments
                id = uri.getPathSegments().get(1);
                // Calls the 'increaseNoteVersion' method with parameters: Long value of 'id', selection, and selectionArgs
                increaseNoteVersion(Long.valueOf(id), selection, selectionArgs);
                // Performs an update operation on the database TABLE.NOTE using provided 'values', custom condition, 'selection', and 'selectionArgs'
                count = db.update(TABLE.NOTE, values, NoteColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                break;
            
            // Case where the URI matches URI_DATA
            case URI_DATA:
                // Performs an update operation on the database TABLE.DATA using provided 'values', 'selection', and 'selectionArgs'
                count = db.update(TABLE.DATA, values, selection, selectionArgs);
                // Sets 'updateData' flag to true
                updateData = true;
                break;
            
            // Case where the URI matches URI_DATA_ITEM
            case URI_DATA_ITEM:
                // Retrieves the 'id' from the URI's path segments
                id = uri.getPathSegments().get(1);
                // Performs an update operation on the database TABLE.DATA using provided 'values', custom condition, 'selection', and 'selectionArgs'
                count = db.update(TABLE.DATA, values, DataColumns.ID + "=" + id + parseSelection(selection), selectionArgs);
                // Sets 'updateData' flag to true
                updateData = true;
                break;
            
            // Default case if the URI doesn’t match any of the specified cases
            default:
                // Throws an IllegalArgumentException with a message indicating an unknown URI
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Checks if the count of updated rows is greater than 0
        if (count > 0) {
            // Checks if 'updateData' flag is true
            if (updateData) {
                // Notifies a change to the ContentResolver for Notes.CONTENT_NOTE_URI
                getContext().getContentResolver().notifyChange(Notes.CONTENT_NOTE_URI, null);
            }
            // Notifies a change to the ContentResolver for the provided 'uri'
            getContext().getContentResolver().notifyChange(uri, null);
        }
        // Returns the count of updated rows
        return count;
    }


    private String parseSelection(String selection) {
        return (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
    }

    // Declares a method named 'increaseNoteVersion' which takes in parameters: long id, String selection, and String[] selectionArgs
    private void increaseNoteVersion(long id, String selection, String[] selectionArgs) {
        // Initializes a StringBuilder 'sql' with an initial capacity of 120 characters
        StringBuilder sql = new StringBuilder(120);
        // Appends "UPDATE " to the 'sql' StringBuilder
        sql.append("UPDATE ");
        // Appends the TABLE.NOTE value to the 'sql' StringBuilder
        sql.append(TABLE.NOTE);
        // Appends " SET " to the 'sql' StringBuilder
        sql.append(" SET ");
        // Appends the value of NoteColumns.VERSION to the 'sql' StringBuilder
        sql.append(NoteColumns.VERSION);
        // Appends "= NoteColumns.VERSION + 1" to the 'sql' StringBuilder
        sql.append("=" + NoteColumns.VERSION + "+1 ");

        // Checks if 'id' is greater than 0 or if 'selection' is not empty
        if (id > 0 || !TextUtils.isEmpty(selection)) {
            // Appends " WHERE " to the 'sql' StringBuilder
            sql.append(" WHERE ");
        }
        // Checks if 'id' is greater than 0
        if (id > 0) {
            // Appends the condition NoteColumns.ID = 'id' to the 'sql' StringBuilder
            sql.append(NoteColumns.ID + "=" + String.valueOf(id));
        }
        // Checks if 'selection' is not empty
        if (!TextUtils.isEmpty(selection)) {
            // Creates a new String 'selectString' based on the conditions of 'id' and 'selection'
            String selectString = id > 0 ? parseSelection(selection) : selection;
            // Replaces placeholders in 'selectString' with corresponding values from 'selectionArgs'
            for (String args : selectionArgs) {
                selectString = selectString.replaceFirst("\\?", args);
            }
            // Appends the modified 'selectString' to the 'sql' StringBuilder
            sql.append(selectString);
        }

        // Gets a writable database using 'mHelper' and executes the SQL command obtained from the 'sql' StringBuilder
        mHelper.getWritableDatabase().execSQL(sql.toString());
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

}
