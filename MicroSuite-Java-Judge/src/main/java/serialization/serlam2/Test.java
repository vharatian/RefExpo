package serialization.serlam2;

// serlam/Test.java


import java.io.Serializable;

public @FunctionalInterface interface Test extends Serializable{
    String concat(Integer seconds);
}