package signaturepolymorphicmethods.spm5;

// spm5/VirtualSPMCall.java


//import lib.annotations.callgraph.IndirectCall;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

class VirtualSPMCall {
    
//       @IndirectCall(
//            name = "method", returnType = void.class, parameterTypes = {byte.class}, line = 19,
//            resolvedTargets = "Lspm5/Class;", prohibitedTargets = "Lspm5/SuperClassWithMethod;")
       public static void main(String[] args) throws Throwable {
           MethodType descriptor = MethodType.methodType(void.class, byte.class);
           MethodHandle mh = MethodHandles.lookup().findVirtual(SuperClassWithMethod.class,"method", descriptor);
           Class callOnMe = new Class();
           byte paramValue = 42;
           mh.invoke(callOnMe, paramValue);
       }
}

class Class  extends SuperClassWithMethod { 
    
    public void method(byte b){
        /* do something */
    }
}

class SuperClassWithMethod {
    
    public void method(byte b){
        /* do something */
    }
}