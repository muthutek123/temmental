package temmental2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

class Function extends Element {

	protected Object input;
	protected Identifier function;

	public Function(Identifier func, Object input) {
		super(func.cursor);
		this.input = input;
		this.function = func;
	}
	
	@Override
	String getIdentifier() {
		return function.getIdentifier();
	}
	
	@Override
	public String toString() {
		return "@" + cursor.getPosition() + "\tFunction(" + function + "," + input + ")";
	}

	@Override
	Object writeObject(Map<String, Object> functions, Map<String, Object> model, TemplateMessages messages) throws TemplateException {
		Object result = function.writeObject(functions, model, messages);
		
		String o = (String) result;

		Object fp = functions.get(o);
				
		if (fp == null && function.isRequired()) {
			throw new TemplateException("No transform function named '%s' is associated with the template for rendering '\u2026:%s' at position '%s'.", o, function.getIdentifier(), function.cursor.getPosition());
		} else if (fp == null) {
			return null;
		}

		Object arg = ((input instanceof Element) 
						? ((Element) input).writeObject(functions, model, messages)
						: input);

		if (arg == null) {
			return null;
		}

		if (fp instanceof Method) {
			Method method = ((Method) fp);
			return callMethod(method, o, arg, null);
		} else if (fp instanceof Transform) {
			Method method = getApplyMethod((Transform) fp);
			return callMethod(method, o, fp, arg);
		} else {
			throw new TemplateException("Invalid transform function type '%s'.", fp.getClass().getCanonicalName());
		}
		
	}

	protected Object callMethod(Method method, String o, Object obj, Object params)
			throws TemplateException {
		
		
		if (params != null && ! ((Class<?>)method.getParameterTypes()[0]).isAssignableFrom(((Class<?>)params.getClass()))) {
			throw new TemplateException("Unable to render '\u2026:%s' at position '%s'. The function %s expects %s. It receives %s.", 
					getIdentifier(),
					cursor.getPosition(),
					o,
					method.getParameterTypes()[0].getCanonicalName(),  
					params.getClass().getCanonicalName()); 
		}
		
		Exception occured = null;
		try {
			if (params == null)
				return method.invoke(obj);
			else 
				return method.invoke(obj, params);
		} catch (IllegalAccessException e) {
			occured = e;
		} catch (IllegalArgumentException e) {
			occured = e;
		} catch (InvocationTargetException e) {
			occured = e;
		}
		
		if (! ((Class<?>)method.getDeclaringClass()).isAssignableFrom(((Class<?>)obj.getClass()))) {
			throw new TemplateException("Unable to render '\u2026:%s' at position '%s'. The function %s expects %s. It receives %s.", 
					getIdentifier(),
					cursor.getPosition(),
					o,
					method.getDeclaringClass().getCanonicalName(),  
					obj.getClass().getCanonicalName()); 
		} else {
			if (method.getParameterTypes().length != 0) {
				throw new TemplateException("Unable to render '\u2026:%s' at position '%s'. The function %s expects %s parameter%s but is called without parameter!", 
						getIdentifier(),
						cursor.getPosition(),
						o,
						(method.getParameterTypes().length == 1 ? "one" : Integer.toString(method.getParameterTypes().length)),
						(method.getParameterTypes().length > 1 ? "s" : "")); 
			} else {
				throw new TemplateException(occured, "Unable to determine reason.");
			}
		}
	} 
	
	protected Method getApplyMethod(Transform t) {
		Method[] methods = t.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals("apply"))
                return method;
        }
        return null;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o instanceof Function) {
			Function oc = (Function) o;
			return oc.input.equals(input) && oc.function.equals(function);
		} 
		return false;
	}

}
