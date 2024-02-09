package classloading.cl4;

// cl4/Demo.java


import java.util.Comparator;

public class Demo {

//	@IndirectCall(name = "callback", line = 14, resolvedTargets = "Lcl4/Demo;")
    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        ClassLoader cl = new ByteClassLoader(ClassLoader.getSystemClassLoader());
        Class<?> cls = cl.loadClass("lib.IntComparator");
        Comparator<Integer> comparator = (Comparator<Integer>) cls.newInstance();
        Integer one = 1;
        comparator.compare(one, one);
    }

    public static void callback() { }
}