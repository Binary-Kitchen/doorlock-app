package de.binary_kitchen.doorlock_app.doorlock_api;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public LockState getStatus() {
        return status;
    }

    private @SerializedName("err") ApiErrorCode errorCode;
    private @SerializedName("msg") String message;
    private @SerializedName("status") LockState status;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append("errorCode: ")
                .append(errorCode)
                .append(", message: ")
                .append(message)
                .append(", status: ");
        if(status == null){
            sb.append("null");
        } else{
            sb.append(status.toString());
        }

        return sb.toString();
    }
}