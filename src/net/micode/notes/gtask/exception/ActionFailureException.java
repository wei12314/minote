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

package net.micode.notes.gtask.exception;

public class ActionFailureException extends RuntimeException {
    private static final long serialVersionUID = 4425249765923293627L; // Serial version ID for serialization

    public ActionFailureException() { // Constructor without parameters
        super(); // Calls the constructor of the superclass (RuntimeException)
    }

    public ActionFailureException(String paramString) { // Constructor with a string parameter
        super(paramString); // Calls the constructor of the superclass (RuntimeException) with a message
    }

    public ActionFailureException(String paramString, Throwable paramThrowable) { // Constructor with string and throwable parameters
        super(paramString, paramThrowable); // Calls the constructor of the superclass (RuntimeException) with a message and a throwable
    }
}
