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

    public ApiErrorCode get_error_code()
    {
        return errorCode;
    }

    public String get_message()
    {
        return message;
    }

    public boolean is_open()
    {
        return open;
    }

    private @SerializedName("err") ApiErrorCode errorCode;
    private @SerializedName("msg") String message;
    private @SerializedName("open") boolean open;
}