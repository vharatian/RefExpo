package java8invokedynamics.lambda2;

// id/Class.java


//import lib.annotations.callgraph.IndirectCall;

class Class {

    public static void doSomething(){ }

//    @IndirectCall(name = "doSomething", line = 12, resolvedTargets = "Lid/LambdaProvider;")
    public static void main(String[] args) {
        Runnable lambda = LambdaProvider.getRunnable();
        lambda.run();
    }
}

class LambdaProvider {

    public static void doSomething(){
        /* do something */
    }

    public static java8invokedynamics.lambda2.Runnable getRunnable(){
        return () -> LambdaProvider.doSomething();
    }
}