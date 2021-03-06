package temmental2;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractTestTemplate {

	// ===============================================================================================================================================
	// functions for testing...
	// ===============================================================================================================================================

	protected Cursor c(int line, int column) {
		return new Cursor(p(line, column));
	}

	protected String p(int line, int column) {
		return "-:l" + line + ":c" + column;
	}

	protected List<Object> list(Object ... objects) {
		return Arrays.asList(objects);
	}

	protected Array array(String position, Object ... objects) {
		return new Array(Arrays.asList(objects), new Cursor(position));
	}
	
	protected Bracket bracket(char name, String position) {
		return new Bracket(name, new Cursor(position));
	}

	protected Identifier identifier(String name, String position) throws TemplateException {
		return new Identifier(name, new Cursor(position));
	}
	
	protected Keyword keyword(String name, String position) throws TemplateException {
		return new Keyword(name, new Cursor(position));
	}
	
	protected Function function(Identifier name, Object input) {
		return new Function(name, input);
	}
	
	protected Functionp functionp(Identifier name, List<Object> initParameters, Element input) {
		return new Functionp(name, initParameters, input);
	}

	protected Message message(Identifier name, List<Object> parameters) {
		return new Message(name, parameters);
	}

	protected ToApply toapply(String position) {
		return new ToApply(new Cursor(position));
	}

	protected Comma comma(String position) {
		return new Comma(new Cursor(position));
	}

	protected Expression toparse(String expr, String position) {
		return new Expression(expr, new Cursor(position));
	}

	protected Text text(String name, String position) {
		return new Text(name, new Cursor(position));
	}
	
	protected Char character(char c, String position) {
		return new Char(c, new Cursor(position));
	}

	protected void displayRule(String s) {
		StringWriter swu = new StringWriter();
		StringWriter swd = new StringWriter();
		StringWriter swl = new StringWriter();
		for (int i=0, u=0, d=0; i<s.length(); i++) {
			if (s.charAt(i) == '\n') {
				u = 0;
				d = 0;
				swu.append(' ');
				swd.append(' ');
				swl.append('*');
				continue;
			} else {
				u = (u+1) % 10;
			}
			if (u != 0) {
				swu.append(String.valueOf(u).charAt(0));
				swd.append(' ');
				swl.append(' ');
			} else {
				swu.append(String.valueOf(u).charAt(0));
				d += 1;
				swd.append(String.valueOf(d).charAt(0));
				swl.append(' ');
			}
		}
		System.out.println(s);
		System.out.println(swu.toString());
		System.out.println(swd.toString());
		System.out.println(swl.toString());
	}

}
