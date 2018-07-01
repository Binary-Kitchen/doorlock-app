package de.binary_kitchen.doorlock_app.doorlock_api;

import com.google.gson.annotations.SerializedName;

public enum LockState{
    @SerializedName("Offen") OPEN,
    @SerializedName("Geschlossen") CLOSED
}