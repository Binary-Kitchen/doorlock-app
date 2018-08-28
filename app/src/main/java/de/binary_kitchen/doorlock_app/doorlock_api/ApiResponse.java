package de.binary_kitchen.doorlock_app.doorlock_api;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public boolean is_open() {
        return open;
    }

    private @SerializedName("err") ApiErrorCode errorCode;
    private @SerializedName("msg") String message;
    private @SerializedName("open") boolean open;
}