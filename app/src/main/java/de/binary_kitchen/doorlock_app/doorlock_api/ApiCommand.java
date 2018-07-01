package de.binary_kitchen.doorlock_app.doorlock_api;

public enum ApiCommand{
    LOCK("lock"),
    UNLOCK("unlock"),
    STATUS("status");

    private final String value;

    ApiCommand(final String value){
        this.value = value;
    }

    @Override
    public String toString(){
        return this.value;
    }
}