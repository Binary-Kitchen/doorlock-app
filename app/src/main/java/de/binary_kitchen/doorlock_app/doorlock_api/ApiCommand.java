/*
 * Doorlock, Binary Kitchen's Open Sesame
 *
 * Copyright (c) Binary Kitchen e.V., 2018
 *
 * Authors:
 *  Ralf Ramsauer <ralf@binary-kitchen.de>
 *  Thomas Schmid <tom@binary-kitchen.de>
 *
 * This work is licensed under the terms of the GNU GPL, version 2.  See
 * the COPYING file in the top-level directory.
 */

package de.binary_kitchen.doorlock_app.doorlock_api;

public enum ApiCommand{
    LOCK("lock"),
    PRESENT("present"),
    UNLOCK("unlock"),
    STATUS("status");

    private final String value;

    ApiCommand(final String value){
        this.value = value;
    }

    @Override
    public String toString()
    {
        return this.value;
    }

    public static ApiCommand fromString(String text)
    {
        for (ApiCommand e: ApiCommand.values()) {
            if (e.value.equals(text)) {
                return e;
            }
        }
        return null;
    }
}