package staticinitializers.si2;

// si/Interface.java


//import lib.annotations.callgraph.DirectCall;
public interface Interface {

	static String name = init();

//    @DirectCall(name = "callback", line = 10, resolvedTargets = "Lsi/Interface;")
	static String init() {
		callback();
		return "Demo";
	}

	static void callback() {}
}
class Demo {
    
	public static void main(String[] args) {
		Interface.callback();
	}
}