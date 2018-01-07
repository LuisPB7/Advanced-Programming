package ist.meic.pa;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;
import javassist.Translator;

/*
 * This is the class that allows us to inject code into a compiled class
 */
public class KeywordArgsTranslator implements Translator{
	
	/*
	 * Method that checks the loaded classes and verifies whether its constructors have the KeywordArgs
	 * annotation, or not. If they do, we change it, calling setConstructor
	 */
	@Override
	public void onLoad(ClassPool pool, String className) throws NotFoundException, CannotCompileException {
		CtClass ctClass = pool.get(className);
		CtConstructor cons = ctClass.getConstructors()[0];
		try {
			Object[] annotations = cons.getAnnotations();
			for(Object a: annotations){
				if(a instanceof KeywordArgs){
					setConstructor(ctClass, cons, ((KeywordArgs) a).value());
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Method that injects code into the compiled class' constructors, using Javassist
	 */
	private static void setConstructor(CtClass c, CtConstructor constructor, String annotationValue) throws CannotCompileException, NotFoundException {
		String body = "{ ";
		
		//If the class is subclass of another other than Object, we call super in order 
		//to let the other class make its assignments
		if(!c.getSuperclass().getSimpleName().equals("Object")){
			body += "super($1);";
		}
		
		//We inject the class with its own class object and all its public and private fields using Reflection
		//We iterated through all the superclass's fields, since the getFields() method only returns
		//the public fields, and Java does not set it public by default
		body += "Class ownClass = $class;" + 
				"java.util.List allFields = new java.util.ArrayList(); " +
				"java.util.List allFieldNames = new java.util.ArrayList(); " +
				"for (Class c = ownClass; c != null; c = c.getSuperclass()) {" +
				"    allFields.addAll(java.util.Arrays.asList(c.getDeclaredFields())); " +
				"}" +
				"for (int i = 0; i < allFields.size(); i++) {" +
				"    allFieldNames.add(((java.lang.reflect.Field)allFields.get(i)).getName()); " +
				"}";
		
		//The first thing we do is check the annotation value, and injecting the assignments it does
		String[] attributes = annotationValue.split(",");
		for(String assignment: attributes){
			try{
					body += assignment.split("=")[0] + "= " + assignment.split("=")[1] + ";";
			}catch(Exception e) {}
		}
		
		//Next, we override (if it's the case) the annotation's value, by looping through the
		//constructor's parameters and using the fields list previously defined to set the fields
		body += "for(int i = 0; i < $1.length; i = i + 2){ " +
					"if(!(ownClass.getSuperclass().getSimpleName().equals(\"Object\")) && !(allFieldNames.contains($1[i]))){" +
						"throw new RuntimeException(\"Unrecognized keyword: \" +$1[i]);}" +
					"for(int k = 0; k < allFields.size(); k++){ " +
						"try{\n" + 
							 "if(((java.lang.reflect.Field)(allFields.get(k))).getName().equals($1[i])){ " +
								"((java.lang.reflect.Field)(allFields.get(k))).set(this, $1[i+1]); " +
							  "} " +
						"}catch(Exception e){System.out.println(\"entrou na excecao\");}" +
					"} " + 
				"}";
		
		body += "}";
		
		//Inject the previously defined body to the constructor
		try {
			constructor.setBody(body);
		} catch (CannotCompileException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start(ClassPool pool) throws NotFoundException, CannotCompileException {}

}
