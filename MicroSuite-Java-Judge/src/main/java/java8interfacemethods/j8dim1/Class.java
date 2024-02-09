package java8interfacemethods.j8dim1;

// j8dim/Class.java


//import lib.annotations.callgraph.DirectCall;

class Class implements Interface {

//    @DirectCall(name = "method", line = 10, resolvedTargets = "Lj8dim/Interface;")
    public static void main(String[] args){
        Interface i = new Class();
        i.method();
    }
}

interface Interface {
    default void method() {
        // do something
    }
}