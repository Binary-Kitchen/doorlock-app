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

import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    public enum ApiErrorCode {
        @SerializedName("0") SUCCESS(0),
        @SerializedName("1") PERMISSION_DENIED(1),
        @SerializedName("2") ALREADY_LOCKED(2),
        @SerializedName("3") ALREADY_OPEN(3),
        @SerializedName("4") INVALID(4),
        @SerializedName("5") LDAP_ERROR(5);

        ApiErrorCode(int value)
        {
        }
    }

    public enum DoorState {
        @SerializedName("0") Open(0),
        @SerializedName("1") Present(1),
        @SerializedName("2") Closed(2);

        DoorState(int value)
        {
        }
    }

    public @SerializedName("err") ApiErrorCode error_code;
    public @SerializedName("msg") String message;
    public @SerializedName("status") DoorState status;
}
