package temmental2;

import java.util.List;
import java.util.Map;

class Array extends Element {

	private List<Object> parameters;
	
	public Array(List<Object> parameters, Cursor cursor) {
		super(cursor); 
		this.parameters = parameters;
	}

	@Override
	String getIdentifier() {
		throw new RuntimeException("Sould not be called!");
	}

	@Override
	Object writeObject(Map<String, Object> functions, Map<String, Object> model, TemplateMessages messages) throws TemplateException {
		return create_parameters_after_process(parameters, functions, model, messages);
	}
	
	public boolean equals(Object o) {
		if (o == null || ! (o instanceof Array))
			return false;
		Array oc = (Array) o;
		return oc.cursor.equals(cursor) && oc.parameters.size() == parameters.size() && oc.parameters.equals(parameters);

	}
}
