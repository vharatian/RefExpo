package dynamicproxies.dp1;

// dp/Main.java


import java.lang.reflect.Method;

public class Main {
//	@IndirectCall(
//        name = "bar", returnType = Object.class, parameterTypes = Object.class, line = 17,
//        resolvedTargets = "Ldp/FooImpl;"
//    )
//    @IndirectCall(
//        name = "invoke", returnType = Object.class, parameterTypes = {Object.class, Method.class, Object[].class}, line = 17,
//        resolvedTargets = "Ldp/DebugProxy;"
//    )
	public static void main(String[] args) {
		Foo foo = (Foo) DebugProxy.newInstance(new FooImpl());
		foo.bar(null);
	}
}