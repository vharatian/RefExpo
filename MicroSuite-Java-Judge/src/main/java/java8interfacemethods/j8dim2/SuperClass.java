package java8interfacemethods.j8dim2;

// j8dim/SuperClass.java


//import lib.annotations.callgraph.DirectCall;

class SuperClass {

    public void method(){
        // do something
    }

//    @DirectCall(
//            name = "method",
//            line = 19,
//            resolvedTargets = "Lj8dim/SuperClass;",
//            prohibitedTargets = {"Lj8dim/Interface;"}
//    )
    public static void main(String[] args){
        Interface i = new SubClass();
        i.method();
    }
}

interface Interface {
    default void method() {
        // do something
    }
}

class SubClass extends SuperClass implements Interface {

}