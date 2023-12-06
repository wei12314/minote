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

package net.micode.notes.tool;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.CallNote;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute;

import java.util.ArrayList;
import java.util.HashSet;


public class DataUtils {
    public static final String TAG = "DataUtils";
    // Method to batch delete notes using ContentResolver
    public static boolean batchDeleteNotes(ContentResolver resolver, HashSet<Long> ids) {
        if (ids == null) {
            Log.d(TAG, "the ids is null"); // Logging if the IDs set is null
            return true;
        }
        if (ids.size() == 0) {
            Log.d(TAG, "no id is in the hashset"); // Logging if the IDs set is empty
            return true;
        }

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            if(id == Notes.ID_ROOT_FOLDER) {
                Log.e(TAG, "Don't delete system folder root"); // Logging not to delete system folder root
                continue;
            }
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newDelete(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id)); // Creating a delete operation for the note
            operationList.add(builder.build()); // Adding the operation to the list
        }
        try {
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList); // Applying batch deletion
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "delete notes failed, ids:" + ids.toString()); // Logging if deleting notes failed
                return false; // Returning false if deletion fails
            }
            return true; // Returning true if deletion is successful
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage())); // Handling RemoteException
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage())); // Handling OperationApplicationException
        }
        return false; // Returning false if any exception occurs
    }


    public static void moveNoteToFoler(ContentResolver resolver, long id, long srcFolderId, long desFolderId) {
        ContentValues values = new ContentValues();
        values.put(NoteColumns.PARENT_ID, desFolderId);
        values.put(NoteColumns.ORIGIN_PARENT_ID, srcFolderId);
        values.put(NoteColumns.LOCAL_MODIFIED, 1);
        resolver.update(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id), values, null, null);
    }

    // Method to batch move notes to a specific folder using ContentResolver
    public static boolean batchMoveToFolder(ContentResolver resolver, HashSet<Long> ids,
            long folderId) {
        if (ids == null) {
            Log.d(TAG, "the ids is null"); // Logging if the IDs set is null
            return true;
        }

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            ContentProviderOperation.Builder builder = ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, id)); // Creating an update operation for the note
            builder.withValue(NoteColumns.PARENT_ID, folderId); // Setting the new parent ID for the note
            builder.withValue(NoteColumns.LOCAL_MODIFIED, 1); // Marking as locally modified
            operationList.add(builder.build()); // Adding the operation to the list
        }

        try {
            ContentProviderResult[] results = resolver.applyBatch(Notes.AUTHORITY, operationList); // Applying batch update
            if (results == null || results.length == 0 || results[0] == null) {
                Log.d(TAG, "delete notes failed, ids:" + ids.toString()); // Logging if moving notes fails
                return false; // Returning false if moving notes fails
            }
            return true; // Returning true if moving notes is successful
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage())); // Handling RemoteException
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage())); // Handling OperationApplicationException
        }
        return false; // Returning false if any exception occurs
    }


    /**
     * Get the all folder count except system folders {@link Notes#TYPE_SYSTEM}}
     */
    public static int getUserFolderCount(ContentResolver resolver) {
        Cursor cursor =resolver.query(Notes.CONTENT_NOTE_URI,
                new String[] { "COUNT(*)" },
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>?",
                new String[] { String.valueOf(Notes.TYPE_FOLDER), String.valueOf(Notes.ID_TRASH_FOLER)},
                null);

        int count = 0;
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                try {
                    count = cursor.getInt(0);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "get folder count failed:" + e.toString());
                } finally {
                    cursor.close();
                }
            }
        }
        return count;
    }

    // Check if a note with a specific type is visible in the note database
    public static boolean visibleInNoteDatabase(ContentResolver resolver, long noteId, int type) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null,
                NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER,
                new String[]{String.valueOf(type)},
                null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    // Check if a note exists in the note database
    public static boolean existInNoteDatabase(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId),
                null, null, null, null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    // Check if data exists in the data database
    public static boolean existInDataDatabase(ContentResolver resolver, long dataId) {
        Cursor cursor = resolver.query(ContentUris.withAppendedId(Notes.CONTENT_DATA_URI, dataId),
                null, null, null, null);

        boolean exist = false;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }


    public static boolean checkVisibleFolderName(ContentResolver resolver, String name) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI, null,
                NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER +
                " AND " + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER +
                " AND " + NoteColumns.SNIPPET + "=?",
                new String[] { name }, null);
        boolean exist = false;
        if(cursor != null) {
            if(cursor.getCount() > 0) {
                exist = true;
            }
            cursor.close();
        }
        return exist;
    }

    // Get widgets associated with notes in a specific folder from the database
    public static HashSet<AppWidgetAttribute> getFolderNoteWidget(ContentResolver resolver, long folderId) {
        Cursor c = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[]{NoteColumns.WIDGET_ID, NoteColumns.WIDGET_TYPE},
                NoteColumns.PARENT_ID + "=?",
                new String[]{String.valueOf(folderId)},
                null);

        HashSet<AppWidgetAttribute> set = null;
        if (c != null) {
            if (c.moveToFirst()) {
                set = new HashSet<AppWidgetAttribute>(); // Initializing the HashSet
                do {
                    try {
                        AppWidgetAttribute widget = new AppWidgetAttribute(); // Creating a new AppWidgetAttribute object
                        widget.widgetId = c.getInt(0); // Setting widget ID
                        widget.widgetType = c.getInt(1); // Setting widget type
                        set.add(widget); // Adding the widget to the HashSet
                    } catch (IndexOutOfBoundsException e) {
                        Log.e(TAG, e.toString()); // Handling IndexOutOfBoundsException
                    }
                } while (c.moveToNext());
            }
            c.close(); // Closing the cursor
        }
        return set; // Returning the HashSet of widgets associated with notes in the folder
    }


    // Retrieve call number associated with a specific note ID from the database
    public static String getCallNumberByNoteId(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String[]{CallNote.PHONE_NUMBER},
                CallNote.NOTE_ID + "=? AND " + CallNote.MIME_TYPE + "=?",
                new String[]{String.valueOf(noteId), CallNote.CONTENT_ITEM_TYPE},
                null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                return cursor.getString(0); // Return the call number retrieved from the database
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Get call number fails " + e.toString()); // Handle IndexOutOfBoundsException
            } finally {
                cursor.close(); // Close the cursor
            }
        }
        return ""; // Return an empty string if no call number is found
    }

    // Retrieve note ID based on a phone number and call date from the database
    public static long getNoteIdByPhoneNumberAndCallDate(ContentResolver resolver, String phoneNumber, long callDate) {
        Cursor cursor = resolver.query(Notes.CONTENT_DATA_URI,
                new String[]{CallNote.NOTE_ID},
                CallNote.CALL_DATE + "=? AND " + CallNote.MIME_TYPE + "=? AND PHONE_NUMBERS_EQUAL("
                        + CallNote.PHONE_NUMBER + ",?)",
                new String[]{String.valueOf(callDate), CallNote.CONTENT_ITEM_TYPE, phoneNumber},
                null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                try {
                    return cursor.getLong(0); // Return the note ID associated with the phone number and call date
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "Get call note id fails " + e.toString()); // Handle IndexOutOfBoundsException
                }
            }
            cursor.close(); // Close the cursor
        }
        return 0; // Return 0 if no note ID is found
    }

    // Retrieve snippet by note ID from the database
    public static String getSnippetById(ContentResolver resolver, long noteId) {
        Cursor cursor = resolver.query(Notes.CONTENT_NOTE_URI,
                new String[]{NoteColumns.SNIPPET},
                NoteColumns.ID + "=?",
                new String[]{String.valueOf(noteId)},
                null);

        if (cursor != null) {
            try {
                String snippet = ""; // Initialize snippet as an empty string
                if (cursor.moveToFirst()) {
                    snippet = cursor.getString(0); // Retrieve the snippet from the cursor
                }
                cursor.close(); // Close the cursor
                return snippet; // Return the snippet retrieved by note ID
            } catch (Exception e) {
                cursor.close(); // Close the cursor in case of an exception
                Log.e(TAG, "Error getting snippet: " + e.toString()); // Log the exception
            }
        }
        throw new IllegalArgumentException("Note is not found with id: " + noteId); // Throw an exception if note is not found
    }


    public static String getFormattedSnippet(String snippet) {
        if (snippet != null) {
            snippet = snippet.trim();
            int index = snippet.indexOf('\n');
            if (index != -1) {
                snippet = snippet.substring(0, index);
            }
        }
        return snippet;
    }
}
