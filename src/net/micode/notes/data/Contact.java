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

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

// Contact class to retrieve contact name for a given phone number 
public class Contact {
    // Cache contacts in a HashMap to avoid repeat lookups
    private static HashMap<String, String> sContactCache;
    // Tag for logging
    private static final String TAG = "Contact";
    // Selection query to search for a matching contact
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
    + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
    + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";
    // Get contact name for the given phone number
    public static String getContact(Context context, String phoneNumber) {
        // Initialize cache if needed
        if(sContactCache == null) {
            sContactCache = new HashMap<String, String>();
        }
        // Check if result is cached 
        if(sContactCache.containsKey(phoneNumber)) {
            return sContactCache.get(phoneNumber);
        }
        // Modify selection for phone number
        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        // Query contacts database
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                new String [] { Phone.DISPLAY_NAME },
                selection,
                new String[] { phoneNumber },
                null);
        // Check if query returned a result
        if (cursor != null && cursor.moveToFirst()) {
            try {
                // Get contact name 
                String name = cursor.getString(0);
                // Cache result
                sContactCache.put(phoneNumber, name);
                return name;
            } catch (IndexOutOfBoundsException e) {
                // Log error  
                Log.e(TAG, " Cursor get string error " + e.toString());
                return null;
            } finally {
                // Close cursor
                cursor.close();
            }
        } else {
            // Log no match found
            Log.d(TAG, "No contact matched with number:" + phoneNumber);
            return null;
        }
    }
}
