package serialization.ser1;

// ser/Demo.java


import java.io.Serializable;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
//import lib.annotations.callgraph.DirectCall;

public class Demo implements Serializable {
    
    static final long serialVersionUID = 42L;
    
//    @DirectCall(name = "defaultWriteObject", resolvedTargets = "Ljava/io/ObjectOutputStream;", line = 15)
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    	out.defaultWriteObject();
    }

    public static void main(String[] args) throws Exception {
    	Demo serialize = new Demo();
    	FileOutputStream fos = new FileOutputStream("test.ser");
    	ObjectOutputStream out = new ObjectOutputStream(fos);
    	out.writeObject(serialize); // triggers serialization
    	out.close();
    }
}