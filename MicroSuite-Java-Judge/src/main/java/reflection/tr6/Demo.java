package reflection.tr6;

// tr/Demo.java


//import lib.annotations.callgraph.DirectCall;

class Demo {
    public static void verifyCall(){ /* do something */ }

//    @DirectCall(name="verifyCall", line=9, resolvedTargets = "Ltr/Demo;")
    public Demo(String s) { Demo.verifyCall(); }

    public static void main(String[] args) throws Exception {
        Demo.class.getConstructor(String.class).newInstance("42");
    }
}