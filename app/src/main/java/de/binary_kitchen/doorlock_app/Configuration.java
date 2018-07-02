package de.binary_kitchen.doorlock_app;

public class Configuration {
    public static boolean SIMULATION = true;


    public static final String getBaseUrl(){
        if(SIMULATION){
            return "http://10.0.2.2:8080/";
        }

        return "https://lock.binary.kitchen/";
    }
}
