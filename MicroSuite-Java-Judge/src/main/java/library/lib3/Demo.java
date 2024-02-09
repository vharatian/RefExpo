package library.lib3;

// lib3/Demo.java


//import lib.annotations.callgraph.DirectCall;

public class Demo {
    
//    @DirectCall(name = "method", line = 10, resolvedTargets = "Llib3/PotentialSuperclass;",
//    prohibitedTargets = "Llib3/DismissedSuperlass;")
    public static void libraryCallSite(Interface i){
        i.method();
    }
}