package java8interfacemethods.j8sim1;

// j8sim/Class.java


//import lib.annotations.callgraph.DirectCall;

class Class {

//    @DirectCall(name = "method", line = 9, resolvedTargets = "Lj8sim/Interface;")
    public static void main(String[] args){
        Interface.method();
    }
}

interface Interface {
    static void method() {
        // do something
    }
}