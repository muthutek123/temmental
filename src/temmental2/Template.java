
package temmental2;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template {

	private TemplateMessages messages;
	private String filepath;
	private Map<String, ? extends Object> transforms;
    private static final String DEFAULT_SECTION = "__default_section";

    private HashMap<String, Stack> sections = new HashMap<>();
    
    
    /**
     * Create a template with the given parameters.
     * @param filepath the path to the template file to parse
     * @param transforms the map of transform functions
     * @param properties the messages
     * @param locale locale to use to format messages (date, numbers...)
     * @throws IOException if an I/O error occurs when reading the template file
     * @throws TemplateException if an other error occurs when reading the template file
     */
    public Template(String filepath, Map<String, ? extends Object> transforms, Properties properties, Locale locale)
    throws IOException, TemplateException {
        this(filepath, transforms, new TemplateMessages(properties, locale));
    }
    
    
	private Template(String filepath, Map<String, ? extends Object> transforms, TemplateMessages messages) 
	throws IOException, TemplateException {
		sections.put(DEFAULT_SECTION, new Stack());
		this.transforms = transforms;
		this.messages = messages;
		this.filepath = filepath;
		if (filepath != null) {
            readFile(filepath);
        }
	}
	
    /**
     * Create a template with the given parameters. The default locale is used to retrieve localized messages and format messages (date, numbers...).
     * @param filepath the path to the template file to parse
     * @param transforms the map of transform functions
     * @param properties the messages
     * @throws IOException if an I/O error occurs when reading the template file
     * @throws TemplateException if an other error occurs when reading the template file
     */
    public Template(String filepath, Map<String, ? extends Object> transforms, Properties properties) 
    throws IOException, TemplateException {
        this(filepath, transforms, properties, Locale.getDefault());
    }
    
    public Template(String filepath, Map<String, ? extends Object> transforms, Locale locale, Object ... resourcesContainers) 
    throws IOException, TemplateException {
        this(filepath, transforms, new TemplateMessages(locale, resourcesContainers));
    }

    /**
     * Create a template with the given parameters.
     * @param filepath the path to the template file to parse
     * @param transforms the map of transform functions
     * @param bundle the messages
     * @throws IOException if an I/O error occurs when reading the template file
     * @throws TemplateException if an other error occurs when reading the template file
     */
    public Template(String filepath, Map<String, ? extends Object> transforms, ResourceBundle bundle) 
    throws IOException, TemplateException {
        this(filepath, transforms, new TemplateMessages(bundle));
    }
    
    /**
     * Create a template with the given parameters. The default locale is used to retrieve localized messages and format messages (date, numbers...).
     * @param filepath the path to the template file to parse
     * @param transforms the map of transform functions
     * @param resourcePath the messages (<code>classpath:path.to.my.file</code> or <code>file:/path/to/my/file.properties</code>)
     * @throws IOException if an I/O error occurs when reading the template file
     * @throws TemplateException if an other error occurs when reading the template file
     */
    public Template(String filepath, Map<String, ? extends Object> transforms, String resourcePath) 
    throws IOException, TemplateException {
        this(filepath, transforms, resourcePath, Locale.getDefault());
    }
    
    /**
     * Create a template with the given parameters. 
     * @param filepath the path to the template file to parse
     * @param transforms the map of transform functions
     * @param resourcePath the messages (<code>classpath:path.to.my.file</code> or <code>file:/path/to/my/file.properties</code>)
     * @param locale locale to retrieve localized messages and format messages (date, numbers...)
     * @throws IOException if an I/O error occurs when reading the template file
     * @throws TemplateException if an other error occurs when reading the template file
     */
    public Template(String filepath, Map<String, ? extends Object> transforms, String resourcePath, Locale locale) 
    throws IOException, TemplateException {
        this(filepath, transforms, new TemplateMessages(resourcePath, locale));
    }
    
	private void readFile(String filepath) throws IOException, TemplateException {
		FileReader fr = new FileReader(new File(filepath));
		try {
			readReader(fr, 1, 0, true);
		} finally {
			fr.close();
		}
	}

	void parseString(String expression, boolean parseExpression) throws IOException, TemplateException {
		StringReader sr = new StringReader(expression);
		readReader(sr, 1, 0, parseExpression);
	}
	
	private void readReader(Reader sr, int line, int column, boolean parseExpression) throws IOException, TemplateException {
		Stack stack = sections.get(DEFAULT_SECTION);
		Stack taeStack = parseToTextAndExpressions(sr, new Cursor(filepath, line, column));
		stack.clear();
		while (! taeStack.empty()) {
			Object o = taeStack.pop();
			if (o instanceof Expression) {
				if (parseExpression) {
					stack.push(((Expression) o).parse());
				} else {
					stack.push(o);
				}
			} else {
				parseToSections(stack, (Text) o);
			}
		}
	}

	private Stack parseToSections(Stack stack, Text o) throws TemplateException {
		String s = (String) o.writeObject(null, null, null);
    	Pattern p = Pattern.compile("<!--\\s*#section\\s+([a-zA-Z0-9_]+)\\s*-->");
        Matcher m = p.matcher(s);
        if (m.find()) {
        	stack.push(s.substring(0, m.start())); // before
        	String name = m.group(1);
        	int b = m.end();
        	while (m.find()) {
        		int e = m.start();
        		stack = new Stack();
        		sections.put(name, stack);
        		System.out.println("#section " + name);
        		stack.push(s.substring(b, e)); // after
        		name = m.group(1);
        		b = m.end();
        	}
    		stack = new Stack();
    		sections.put(name, stack);
    		stack.push(s.substring(b)); // after
        } else {
        	stack.push(o);
        }
        return stack;
	}


	Stack getStack() {
		return sections.get(DEFAULT_SECTION);
	}

	private static Stack parseToTextAndExpressions(Reader sr, Cursor cursor) throws IOException, TemplateException {
		Stack stack = new Stack();
		StringWriter buffer = new StringWriter();
		boolean opened = false;
		try {
			int currentChar = sr.read();                       
			boolean escape = false;
			while (currentChar != -1) {                        
				cursor.next(currentChar);
				if (currentChar == '\\') {
					escape = true;
					cursor.move1l();
				} else if (escape) {
					buffer.write(currentChar);
					escape = false;
				} else if (! opened && currentChar == '~') {
					String expr = buffer.toString();
					if (! expr.equals("")) {
						stack.push(new Text(expr, cursor.clone().movel(expr, 0)));
						buffer = new StringWriter();
					}
					buffer.write(currentChar);
					opened = true;
				} else if (opened && currentChar == '~') {
					buffer.write(currentChar);
					String expr = buffer.toString();
					if (! expr.equals("")) {
						stack.push(new Expression(expr, cursor.clone().movel(expr, 1)));
						buffer = new StringWriter();
					}
					opened = false;
				} else {
					buffer.write(currentChar);
				}
				currentChar = sr.read(); 
			}
		} finally {
			sr.close();
		}
		if (opened) {
			throw new TemplateException("End of parsing. Character '~' is not escaped at position '%s'.", cursor.getPosition());
		}
		String expr = buffer.toString();
		if (! expr.equals("")) {
			stack.push(new Text(expr, cursor.clone().movel(expr, 1)));
			buffer = new StringWriter();
		}
		return stack;
	}

	
	static Object writeObject(Map<String, Object> functions, Map<String, Object> model, TemplateMessages messages, Object value) throws TemplateException {
		
		if (value instanceof String || value instanceof Number)
			return value;

		if (value instanceof Identifier) {
			return ((Identifier) value).writeObject(functions, model, messages);
		}
		
		if (value instanceof Text) {
			return ((Text) value).writeObject(functions, model, messages);
		}
		
		if (value instanceof Function) {
			Function function = ((Function) value); 
			Object result = function.writeObject(functions, model, messages);
			if (result != null && result instanceof Transform) {
				throw new TemplateException("Unable to apply function '%s' at position '%s'. This function expects one or more parameters. It receives no parameter.",	function.getIdentifier(), function.cursor.getPosition());
			} 
			return result;
		}
		
		if (value instanceof Message) {
			return ((Message) value).writeObject(functions, model, messages);
		}
		
		throw new TemplateException("Unsupported operation for class '%s'", value.getClass().getName());
	}
	
	private void writeSection(Writer out, String sectionName, Map<String, Object> functions, Map<String, Object> model) throws IOException, TemplateException {
		Stack stack = sections.get(sectionName);
		for (int i=stack.depth(); i>0; i--) {
			Object o = writeObject(functions, model, messages, stack.value(i));
			if (o != null) {
				out.write(o.toString());
			}
		}
	}
	
    public TemplateMessages getMessages() {
        return messages;
    }

	public String getFilepath() {
		return filepath;
	}

    String formatForTest(String format, HashMap<String, Object> model) throws IOException, TemplateException {
    	parseString(format, true);
        StringWriter out = new StringWriter();
        writeSection(out, DEFAULT_SECTION, (Map<String, Object>) transforms, model);
        TemplateRecorder.log(this, DEFAULT_SECTION, model);
        return out.toString();
    }

    /** Prints the whole file on the stream.
     * @param out the stream
     * @throws TemplateException if an error is detected by the template engine 
     * @throws java.io.IOException if an I/O error occurs
     */
    public void printFile(Writer out) throws TemplateException, java.io.IOException {
        printSection(out, DEFAULT_SECTION, new HashMap<String, Object>());
    }

   /**
    * Prints the whole file on the stream.
    * @param out the stream
    * @param model the model  
    * @throws TemplateException if an error is detected by the template engine 
    * @throws java.io.IOException if an I/O error occurs
    */
   public void printFile(Writer out, Map<String, ? extends Object> model) throws TemplateException,
   java.io.IOException {
       printSection(out, DEFAULT_SECTION, model);
   }
    
   /**
    * Prints a section of the file on the stream. The tags are replaced by the corresponding values in the model. 
    * @param out the stream
    * @param sectionName the section to display
    * @param model the model  
    * @throws TemplateException if an error is detected by the template engine 
    * @throws java.io.IOException if an I/O error occurs
    */
   public void printSection(Writer out, String sectionName, Map<String, ? extends Object> model)
   throws TemplateException, java.io.IOException {
       if (sectionName == null || !hasSection(sectionName)) {
           throw new TemplateException("Section '" + sectionName + "' not found.");
       }
       writeSection(out, sectionName, (Map<String, Object>) transforms, (Map<String, Object>) model);
       TemplateRecorder.log(this, sectionName, model);
   }

   /**
    * Prints a section of the file on the stream.  
    * @param out the stream
    * @param sectionName the section to display
    * @throws TemplateException if an error is detected by the template engine 
    * @throws java.io.IOException if an I/O error occurs
    */
   public void printSection(Writer out, String sectionName) throws TemplateException, java.io.IOException {
       printSection(out, sectionName, new HashMap<String, Object>());
   }
   
   /**
    * Tests if the given section exists in the template
    * @param sectionName the possible section name
    * @return <code>true</code> if the section exists, <code>false</code> otherwise.
    */
   public boolean hasSection(String sectionName) {
       return sections.containsKey(sectionName);
   }
}
