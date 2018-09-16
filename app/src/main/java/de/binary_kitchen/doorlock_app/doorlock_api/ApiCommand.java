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