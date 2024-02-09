package library.lib1;

// lib1/Demo.java


//import lib.annotations.callgraph.DirectCall;

public class Demo {
    
//    @DirectCall(name = "method", line = 10, resolvedTargets = {"Llib1/Type;", "Llib1/Subtype;"},
//    prohibitedTargets = "Llib1/SomeType;")
    public void libraryEntryPoint(Type type){
        type.method();
    }
}