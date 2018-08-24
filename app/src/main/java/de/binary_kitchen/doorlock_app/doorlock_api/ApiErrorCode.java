package de.binary_kitchen.doorlock_app.doorlock_api;

import com.google.gson.annotations.SerializedName;

public enum ApiErrorCode{
    @SerializedName("0") SUCCESS(0),
    @SerializedName("1") PERMISSION_DENIED(1),
    @SerializedName("2") ALREADY_LOCKED(2),
    @SerializedName("3") ALREADY_OPEN(3),
    @SerializedName("4") INVALID(4),
    @SerializedName("5") LDAP_ERROR(5);

    final private int value;

    ApiErrorCode(int value){
        this.value=value;
    }
    public int getValue(){
        return this.value;
    }
}
