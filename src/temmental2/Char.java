package temmental2;

import java.util.Map;

class Char extends Element {
	
	private char expr;
	
	Char(char expr, Cursor cursor) {
		super(cursor);
		this.expr = expr;
	}
	
	@Override
	public String toString() {
		return "@" + cursor.getPosition() + "\tChar(" + expr + ")";
	}

	public boolean equals(Object o) {
		if (o == null || ! (o instanceof Char))
			return false;
		Char oc = (Char) o;
		return oc.expr == expr && oc.cursor.equals(cursor);
	}

	@Override
	String getIdentifier() {
		throw new RuntimeException("Characters have no identifier!");
	}

	@Override
	Object writeObject(Map<String, Object> functions, Map<String, Object> model, TemplateMessages messages) throws TemplateException {
		return expr;
	}

}
