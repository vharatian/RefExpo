package library.lib5;

// lib5/collude/Demo.java


//import lib.annotations.callgraph.DirectCall;

public class Demo {
    
//    @DirectCall(name = "method", line = 9, resolvedTargets = "Llib5/internal/InternalClass;")
    public static void interfaceCallSite(PotentialInterface pi){
        pi.method();
    }
}

interface PotentialInterface {
    
    void method();
}