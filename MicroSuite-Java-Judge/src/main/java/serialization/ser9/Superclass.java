package serialization.ser9;

// ser/Superclass.java


//import lib.annotations.callgraph.DirectCall;

public class Superclass {
    public void callback() { }

//    @DirectCall(name = "callback", resolvedTargets = "Lser/Superclass;", line = 10)
    public Superclass() {
        callback();
    }
}