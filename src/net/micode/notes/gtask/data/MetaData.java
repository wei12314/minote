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

import net.micode.notes.tool.GTaskStringUtils;

import org.json.JSONException;
import org.json.JSONObject;


// Defines a class named 'MetaData' that extends 'Task'
public class MetaData extends Task {
    // Declares a private static final String 'TAG' and initializes it with the simple name of the class
    private final static String TAG = MetaData.class.getSimpleName();

    // Declares a private String 'mRelatedGid' and initializes it to null
    private String mRelatedGid = null;

    // Defines a method 'setMeta' that takes in parameters 'gid' and 'metaInfo' of type String and JSONObject respectively
    public void setMeta(String gid, JSONObject metaInfo) {
        try {
            // Puts the 'gid' value into 'metaInfo' JSON object with a specific key
            metaInfo.put(GTaskStringUtils.META_HEAD_GTASK_ID, gid);
        } catch (JSONException e) {
            // Logs an error if failed to put the 'gid' into 'metaInfo'
            Log.e(TAG, "failed to put related gid");
        }
        // Sets the 'metaInfo' JSON as notes
        setNotes(metaInfo.toString());
        // Sets a specific name for the 'MetaData' object
        setName(GTaskStringUtils.META_NOTE_NAME);
    }

    // Defines a method 'getRelatedGid' that returns the 'mRelatedGid' value
    public String getRelatedGid() {
        return mRelatedGid;
    }

    // Overrides the 'isWorthSaving' method from the 'Task' class
    @Override
    public boolean isWorthSaving() {
        // Returns true if the 'notes' are not null
        return getNotes() != null;
    }

    // Overrides the 'setContentByRemoteJSON' method from the 'Task' class
    @Override
    public void setContentByRemoteJSON(JSONObject js) {
        // Calls the superclass method
        super.setContentByRemoteJSON(js);
        // Checks if 'notes' are not null
        if (getNotes() != null) {
            try {
                // Parses the 'notes' JSON string and retrieves a specific value to set 'mRelatedGid'
                JSONObject metaInfo = new JSONObject(getNotes().trim());
                mRelatedGid = metaInfo.getString(GTaskStringUtils.META_HEAD_GTASK_ID);
            } catch (JSONException e) {
                // Logs a warning if failed to retrieve 'mRelatedGid'
                Log.w(TAG, "failed to get related gid");
                mRelatedGid = null;
            }
        }
    }

    // Overrides the 'setContentByLocalJSON' method from the 'Task' class
    @Override
    public void setContentByLocalJSON(JSONObject js) {
        // Throws an IllegalAccessError indicating this method should not be called
        throw new IllegalAccessError("MetaData:setContentByLocalJSON should not be called");
    }

    // Overrides the 'getLocalJSONFromContent' method from the 'Task' class
    @Override
    public JSONObject getLocalJSONFromContent() {
        // Throws an IllegalAccessError indicating this method should not be called
        throw new IllegalAccessError("MetaData:getLocalJSONFromContent should not be called");
    }

    // Overrides the 'getSyncAction' method from the 'Task' class
    @Override
    public int getSyncAction(Cursor c) {
        // Throws an IllegalAccessError indicating this method should not be called
        throw new IllegalAccessError("MetaData:getSyncAction should not be called");
    }
}
