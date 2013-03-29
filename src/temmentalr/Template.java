package temmentalr;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Template extends Stack {
	
	private static final boolean debug = true;

	private Map<String, Transform> functions;
	private TemplateMessages messages;
	
	public Template(TemplateMessages messages) {
		this(new ArrayList());
		this.messages = messages;
	}

	public Template(List<Object> tocopy) {
		super(tocopy);
		functions = new HashMap<String, Transform>();
	}

	private List<Integer> chars(int ... chars) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		for (int c : chars) {
			result.add(c);
		}
		return result;
	}
	
	private void debug(String format, Object ... parameters) {
		if (debug)
			System.err.println(String.format(format, parameters));
	}
	
	public void parse(String expression, String file, int line, int column) throws IOException, TemplateException {
		StringReader sr = new StringReader(expression);
		StringWriter buffer = new StringWriter();
		boolean outsideAnExpression = true;
		boolean sentence = false;
		try {
			int previousChar = 0;
			int currentChar = sr.read(); 
			while (currentChar != -1) {
				column++;
				if (outsideAnExpression) {
					if (currentChar != '~') {
						buffer.write(currentChar);
						if (currentChar == '\n') {
							line++;
							column = 0;
						} 
					} else {
						int nextChar = sr.read();
						if (nextChar == -1) {
							outsideAnExpression = false;
							String word = buffer.toString();
							if (! "".equals(word)) {
								change_word(word, file, line, column, currentChar, true);
							}
							buffer = new StringWriter();
							break;
						} else {
							if (nextChar == '~' && currentChar == '~') {
								buffer.write(currentChar);
								previousChar = currentChar;
								currentChar = sr.read();
								continue;
							} else {
								outsideAnExpression = false;
								String word = buffer.toString();
								if (! "".equals(word)) {
									change_word(word, file, line, column, currentChar, true);
								}
								buffer = new StringWriter();
								previousChar = currentChar;
								currentChar = nextChar;
								continue;
							}
						}
					}
				} else {
					if (chars('"').contains(currentChar) || sentence) {
						buffer.write(currentChar);
						if (currentChar == '"' && previousChar != '\\') {
							if (sentence) {
								sentence = false;
								String word = buffer.toString();
								if (! "".equals(word)) {
									change_word(word, file, line, column, currentChar, outsideAnExpression);
								}
								buffer = new StringWriter();
							} else {
								sentence = true;
							}
						}
						previousChar = currentChar;
						currentChar = sr.read(); 
						continue;
					} else if (chars('<', '>', '[', ']', ',', ':', '~').contains(currentChar)) {
						String word = buffer.toString();
						if (! "".equals(word)) {
							change_word(word, file, line, column, currentChar, outsideAnExpression);
						}
						buffer = new StringWriter();
						if (currentChar == ':') {
							push("#func");
						} else if (currentChar == '<') {
							push("#<");
						} else if (currentChar == '>') {
							push("#>");
							eval();
						} else if (currentChar == '[') {
							push("#[");
						} else if (currentChar == ']') {
							push("#]");
							eval();
						}
					} else {
						buffer.write(currentChar);
					}
					if (currentChar == '~') {
						outsideAnExpression = true;
					}
				}
				previousChar = currentChar;
				currentChar = sr.read(); 
			}
			String word = buffer.toString();
			if (! "".equals(word)) {
				change_word(word, file, line, column, currentChar, outsideAnExpression);
				buffer = new StringWriter();
			}
		} finally {
			sr.close();
		}
	}

	private void eval() throws TemplateException {
		if (depth()>1) {
			Object last = value();
			if (last.equals("#func")) {
				// var 'func #func
				drop(); // var 'func 
				Element func = (Element) pop(); // var 
				tolist(1); // [ var ]
				List parameters = (List) pop();
				push(new Function(func, parameters));
			} else if (last.equals("#>")) {
				// $text #func $funcname #< $p1 $p2 #>
				create_list("#<", "#>"); // $text #func $funcname [$p1, $p2]
				List parameters = (List) pop(); // $text #func $funcname 
				Element func = (Element) pop(); // $text #func 
				push(new Function(func, parameters)); // $text #func RpnFunc
				swap(); // $text RpnFunc #func 
				eval();
			} else if (last.equals("#]")) {
				create_list("#[", "#]");
				List parameters = (List) pop();  
				Identifier word = (Identifier) pop();  
				push(new Message(word, parameters)); // $text #func RpnFunc
				
				
				try {
					printStack(System.out);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}

	private void create_list(String start, String end) {
		drop();  
		int i=1;
		while (i<=depth() && ! value(i).equals(start)) {
			i++;
		}
		tolist(i-1);
		nip();
	}
	
	private void change_word(String word, String file, int line, int column, int currentChar, boolean outsideAnExpression) throws TemplateException {
		
		if (outsideAnExpression) {
			push(word);
		} else {
			if (currentChar != '<' /*&& currentChar != '>'*/ && depth() > 0 && value().equals("#func")) {
				push_word(word, file, line, column);
				swap(); // [ word pos #eval ] #func 
			} else {
				push_word(word, file, line, column);
			}
			eval();
		}
	}

	private void push_word(String word, String file, int line, int column) throws TemplateException {
		if (word.startsWith("\"") && word.endsWith("\"")) {
			push(word.substring(1, word.length()-1));
		} else {
			push(new Identifier(word, file, line, column-word.length()-1));
		}
	}
	
	private static Object writeObject(Writer out, Map<String, Transform> functions, Map<String, Object> model, TemplateMessages messages, Object value) throws IOException, TemplateException {
		
		if (value instanceof String)
			return value;

		if (value instanceof Identifier) {
			return ((Identifier) value).writeObject(functions, model, messages);
		}
		
		if (value instanceof Function) {
			return ((Function) value).writeObject(functions, model, messages);
		}
		
		if (value instanceof Message) {
			return ((Message) value).writeObject(functions, model, messages);
		}
		
		throw new TemplateException("Unsupported operation for class '%s'", value.getClass().getName());
	}
	
	public void write(Writer out, Map<String, Object> model) throws IOException, TemplateException {
		printStack(System.out);
		
		for (int i=depth(); i>0; i--) {
			Object o = writeObject(out, functions, model, messages, value(i));
			if (o != null) {
				out.write(o.toString());
			}
		}
	}

	public void addFunction(String name, Transform function) {
		functions.put(name, function);		
	}

	public void addFunction(final String name, final Method method) {
		if (method.getParameterTypes().length>1) {
			addFunction(name, new Transform<Object[], Transform>() {
				@Override
				public Transform apply(final Object[] value) {
					return new Transform<Object, Object>() {
						@Override
						public Object apply(Object text) {
							try {
								return method.invoke(text, value);
							} catch (Exception e) {
								e.printStackTrace();
							}
							return text;
						}
					};
				}
			});
		} else {
			addFunction(name, new Transform<Object, Object>() {
				@Override
				public Object apply(Object value) {
					try {
						return method.invoke(value);
					} catch (Exception e) {
						e.printStackTrace();
					}
					return value;
				}
			});
		}
	}

}
