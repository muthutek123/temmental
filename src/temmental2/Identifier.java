package temmental2;

import java.util.Map;

class Identifier extends Element {

	private String identifier;
	
	Identifier(String expr, Cursor cursor) throws TemplateException {
		super(cursor);
		this.identifier = expr;
		
		boolean valid = (expr.matches("'\\w+") || expr.matches("\\$\\w+(\\?)?"));
		if (! valid) {
			throw new TemplateException("Invalid identifier syntax for '%s' at position '%s'.", expr, cursor.getPosition());
		} 
	}
	
	@Override
	public String toString() {
		return "@" + cursor.getPosition() + "\tIdentifier(" + identifier + ")";
	}

	public boolean equals(Object o) {
		if (o == null || ! (o instanceof Identifier))
			return false;
		Identifier oc = (Identifier) o;
		return oc.identifier.equals(identifier) && oc.cursor.equals(cursor);
	}

	@Override
	Object writeObject(Map<String, Object> functions, Map<String, Object> model, TemplateMessages messages) throws TemplateException {
		if (identifier.startsWith("'")) {
			return identifier.substring(1);
		} else if (identifier.startsWith("$")) {
			return getInModel(model);
		} else {
			throw new TemplateException("Unsupported case #eval for '%s'", identifier);
		}
	}

	boolean isRequired() {
		return identifier != null && (identifier.startsWith("'") || ! identifier.endsWith("?"));
	}
	
	@Override
	String getIdentifier() {
		return identifier;
	}
	
}
