package ist.meic.pa;


import javassist.*;

/* 
 * In this class, we create a translator for our annotation and we add it to a class loader in order to
 * be able to listen and intercept when the classes are being loaded by the JVM
 */
public class KeyConstructors {
	
	public static void main(String[] args) throws Exception {
		   Translator trans = new KeywordArgsTranslator();
		   ClassPool pool = ClassPool.getDefault();
		   Loader classLoader = new Loader();
		   classLoader.addTranslator(pool, trans);
		   try {
			classLoader.run("ist.meic.pa." + args[0], null);
		} catch (Throwable e) {
			e.printStackTrace();
		  }  
	   }
	
}
