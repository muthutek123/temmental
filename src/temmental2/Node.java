package temmental2;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Node {
	
	enum Type { Literral, Section, Sentence, Text, Unknown, UnknownFilter, Variable, Quote, QuoteMessage, VariableMessage, VariableFilter, QuoteFilter, Array, Command, CommandClose/*, CommandSection*/, ArrayExpansion, QuoteFilterDyn, VariableFilterDyn, Expression };

	private Type type; 
	
	private String fileInformation;
	private int lineInformation;
	private int columnInformation;
	StringWriter buffer;
	StringWriter bufferError;
	private Node parent;
	private List<Node> children;
	
	private enum RenderType { 
	    Optional(",norenderifnotpresent"), 
	    OptionalMessage(",norenderifpropertynamenotpresent"),
	    Required(""), 
	    ReplacedByNameIfNotPresent(",rendernameifnotpresent"),
	    ReplacedByPropertyNameIfNotPresent(",renderpropertynameifnotpresent");
	    final String code;
	    RenderType(String code) {
	        this.code = code;
	    }
	    @Override
	    public String toString() {
	        return code;
	    }
	};
	private RenderType optional;
	private RenderType messageOptional;
	
	private boolean opened;
	private boolean closed;
	private boolean startTransform;
	private BracketType bracketType;

	// round () // square [] // curly {} // angle <>
	enum BracketType { Round, Square, Curly, Angle };
	
	Node(Type type, String file, int line, int column, boolean isFilter) {
		this.type = type;
		this.fileInformation = file;
		this.lineInformation = line;
		this.columnInformation = column;
		parent = null;
		buffer = new StringWriter();
		bufferError = new StringWriter();
		optional = RenderType.Required;
		messageOptional = RenderType.Required;
		children = new ArrayList<Node>();
		startTransform = isFilter;
		if (type == Type.QuoteMessage || type == Type.VariableMessage) {
			opened = true;
			closed = true;
			bracketType = BracketType.Square;
		} else {
			opened = false;
			closed = false;
			bracketType = null;
		}
	}
	
	static String positionInformation(String file, int line, int column) {
		return file + ":l" + line + ":c" + column;
	}
	
	Node write(String file, int line, int column, int c, NewTemplate template) throws TemplateException {
		bufferError.write(c);
	    if (type == Type.Text || type == Type.Sentence || type == Type.Expression) {
	        buffer.write(c);
	        return this;
	    }
		if (type == Type.Unknown || type == Type.UnknownFilter) {
			if (c == '@') {
				type = Type.ArrayExpansion;
			} else if (c == '$') {
				type = startTransform ? Type.VariableFilter : Type.Variable;
			} else if (c == '\'') {
				type = startTransform ? Type.QuoteFilter : Type.Quote;
			} else {
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), c);
			}
			return this;
		} else if (type == Type.Literral) {
			if ((c >= '0' && c <= '9') /*|| (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')*/) {
				buffer.write(c);
			} else {
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), c);
			}
			return this;
		} else if (c == '$') {
			throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), c);
		} else if (c == '\'') {
			throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), c);
		}
		if (c == '?' || c == '!') {
		    if ((type == Type.Variable || type == Type.VariableFilter) && (optional != RenderType.Required)) {
		        throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), c);
		    } else if ((type == Type.VariableMessage) && (messageOptional != RenderType.Required)) {
		        throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), c);
		    }
		    if (type == Type.VariableMessage) {
		        if (c == '?') {
                    this.messageOptional = RenderType.OptionalMessage;
                } else {
                    this.messageOptional = RenderType.ReplacedByPropertyNameIfNotPresent;
                }
		    } else if (type == Type.Variable || type == Type.VariableFilter) {
				validateName(line, column, c, template);
				if (c == '?') {
				    this.optional = RenderType.Optional;
				} else {
				    this.optional = RenderType.ReplacedByNameIfNotPresent;
				}
			} else {
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), c);
			}
		} else {
		    if (closed) {
		        throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), c);
		    }
            if (optional != RenderType.Required) {
                throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), c);
            }
		    buffer.write(c);
		}
		return this;
	}

	private String cons_representation(int from) {
	    return xxx_representation("constructor", "noparam", from, children);
	}
	
//	private String children_representation() {
//	    return xxx_representation("parameters", "noparam", 0, children);
//	}
	
	private String children_representation(int from) {
        return xxx_representation("children", "nochild", from, children);
    }
	
	private String xxx_representation(String with, String without, int from, List<Node> items) {
		if (items.size() - from == 0) {
			return "," + without;
		} else {
			String s = "," + with + "=[";
			for (int i=from; i<items.size(); i++) {
				Node node = items.get(i);
				if (i != from) {
					s += ",,";
				}
				s += node.representation();
			}
			s += "]";
			return s;
		}
	}
	
	String representationTree(int n) {
	    String prefix = "";
	    for (int i=0; i<n*2; i++)
	        prefix += " ";
	    String s = prefix + "<" + buffer.toString() + "> [" + type + "] @" + (this) + "\n";
	    for (Node c : children) {
	        s += c.representationTree(n+1);
	    }
	    return s;
	}
	
	Map<String,Node> sections() {
		Map<String,Node> h = new HashMap<String, Node>();
		if (type == Type.Section) {
			h.put(buffer.toString(), this);
		}
		for (Node c : children) {
	        h.putAll(c.sections());
	    }
		return h;
	}
	
	String representation() {
		if (type == Type.Section) {
			String s = "";
			for (int i=0; i<children.size(); i++) {
				Node child = children.get(i);
				if (i != 0) {
					s += "|";
				}
				s += child.representation();
			}
			return s;
		} else if (type == Type.Text) {
			return "text=" + buffer.toString();
		} else if (type == Type.Sentence) {
			return "string=" + buffer.toString();
		} else if (type == Type.Quote) {
			return "quote=" + buffer.toString();
		} else if (type == Type.QuoteMessage) {
			return "message,quote=" + buffer.toString() + optional + messageOptional + children_representation(0);
		} else if (type == Type.VariableMessage) {
			return "message,variable=" + buffer.toString() + optional + messageOptional + children_representation(0);
		} else if (type == Type.Variable) {
			return "variable=" + buffer.toString() + optional;
		} else if (type == Type.VariableFilter) {
			return children.get(0).representation() + "#transform,variable=" + buffer.toString() + optional;
		} else if (type == Type.QuoteFilter) {
			return children.get(0).representation() + "#transform,quote=" + buffer.toString() + optional;
		} else if (type == Type.VariableFilterDyn) {
			return children.get(0).representation() + "#transform,variable=" + buffer.toString() + optional + cons_representation(1);
		} else if (type == Type.QuoteFilterDyn) {
			return children.get(0).representation() + "#transform,quote=" + buffer.toString() + optional + cons_representation(1);
		} else if (type == Type.Array) {
		    return "array" + children_representation(0);
		} else if (type == Type.Literral) {
			return "number=" + buffer.toString();
		} else if (type == Type.ArrayExpansion) {
		    return "expansion,variable=" + buffer.toString() + optional;
		} else if (type == Type.Command) {
		    return "command[open]=" + buffer.toString() + "," + children.get(0).representation() + children_representation(1);
		} else if (type == Type.CommandClose) {
            return "command[close]=" + buffer.toString();
        } /*else if (type == Type.CommandSection) {
            return "xxxxxxx";
        }*/ else if (type == Type.Unknown) {
			return "??? " + children_representation(0);
		} else if (type == Type.UnknownFilter) {
			return "unknownfilter";
		} else if (type == Type.Expression) {
			return "expr=[" + buffer.toString() + "]";
		} else {
			throw new RuntimeException("Unsupported node type '" + type + "'.");
		}
	}

	void setParent(Node parent) {
		this.parent = parent;
	}

	void add(Node node) {
		children.add(node);
	}
	
	void remove(Node node) {
		children.remove(node);
	}

	void validateAll(int line, int column, int c, boolean checkAncestors, NewTemplate template) throws TemplateException {
		if (type == Type.Literral) {
			String value = buffer.toString();
			if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
			} else if (value.matches("\\d+\\.?\\d*")) {
				 
			} else {
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(fileInformation, line, column - value.length()), value.getBytes()[0]);
			}
		} else if (type == Type.Sentence) { 
	        if (! closed) {
	            throw new TemplateException("Invalid syntax at position '%s' - reach character '%c', string not closed!", positionInformation(fileInformation, line, column), c);
	        }
	        return;
	    }
		checkNotUnknown(line, column, c);
		if (! checkAncestors) {
		    validateSyntax(line, column, c);
		} else {
		    Node tmp = this;
		    while (tmp != null) {
		        tmp.validateSyntax(line, column, c);
		        tmp = tmp.parent;
		    }
		}
		validateName(line, column, c, template);
	}
	
	private void checkNotUnknown(int line, int column, int c) throws TemplateException {
		if (type == Type.Unknown) {
			if (c == ':' && (parent.type == Type.QuoteFilterDyn || parent.type == Type.VariableFilterDyn) && parent.opened && ! parent.closed) {
				type = Type.UnknownFilter;
			} else if (c == '~' && parent.opened && ! parent.closed) {
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c', bracket not closed!", positionInformation(fileInformation, line, column), c);
			} else if (c == '~' && ! parent.opened && parent.closed) {
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c', bracket not opened!", positionInformation(fileInformation, line, column), c);
			} else {
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(fileInformation, line, column), c);
			}
		}
	}

	private void validateName(int line, int column, int c, NewTemplate template) throws TemplateException {
		String name = buffer.toString();
		
		List<String> availableCommands = Arrays.asList("if", "iter");
		if (type == Type.CommandClose || type == Type.Command) {
            if (! availableCommands.contains(name)) {
                throw new TemplateException("Invalid syntax at position '%s' - invalid command name '%s'!", positionInformation(fileInformation, line, column), name);
            }
            
            if (type == Type.CommandClose) {
                String parentName = parent.buffer.toString();
                if (! name.equals(parentName)) {
                    throw new TemplateException("Invalid syntax at position '%s' - bad close tag (expected='%s', actual='%s')", positionInformation(fileInformation, line, column), parentName, name);
                }
            }
		} else if (type == Type.QuoteFilter) {
			if (template.getTransform(name) == null) {
				throw new TemplateException("Unknown filter name '%s' at position '%s'!", name, positionInformation(fileInformation, line, column));
			}
		} else if (type == Type.UnknownFilter) {
		} else if (type == Type.Expression) {
		} else if (type != Type.Text && type != Type.Sentence && type != Type.Array) {
			if (name.equals("")) {
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c', empty name!", positionInformation(fileInformation, line, column), c);
			}
			if (! name.matches("^\\w[\\w\\.]*$")) {
				throw new TemplateException("Invalid syntax at position '%s' - invalid name '%s'", positionInformation(fileInformation, lineInformation, columnInformation + 2), name);
			}
		} 
	}
	
	private void validateSyntax(int line, int column, int c) throws TemplateException {
		// TODO valider parametres...
		if (type == Type.Text) {
			throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(fileInformation, line, column), c);
		}
		if (type == Type.VariableMessage || type == Type.QuoteMessage) {
			if (! closed) {
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(fileInformation, line, column), c);
			}
		} else if (type == Type.Quote) {
			throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(fileInformation, line, column), c);
		} else if (c == ']' && type == Type.Array && (parent.type == Type.VariableMessage || parent.type == Type.QuoteMessage)) {
		    throw new TemplateException("Invalid syntax at position '%s' - reach character '%c', a parameter can not be an array!", positionInformation(fileInformation, line, column), c);
		}
	}

	Node parentNode() {
		return parent;
	}

	Node startTransform(String file, int line, int column, int currentChar, NewTemplate template) throws TemplateException {
		validateAll(line, column, currentChar, false, template);
		Node newFilter = new Node(Type.UnknownFilter, file, line, column, true);
		Node _parent = parentNode();
		newFilter.setParent(_parent);
		_parent.remove(this);
		_parent.add(newFilter);
		setParent(newFilter);
		newFilter.add(this);
		return newFilter;
	}

	Node openBracket(BracketType bracketType, String file, int line, int column, int currentChar) throws TemplateException {
		
		if (type == Type.QuoteFilter) {
			if (bracketType == BracketType.Angle)
				type = Type.QuoteFilterDyn;
			else
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
		} else if (type == Type.VariableFilter) {
			if (bracketType == BracketType.Angle)
				type = Type.VariableFilterDyn;
			else
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
		} else if (type == Type.Quote) {
			if (bracketType == BracketType.Square)
				type = Type.QuoteMessage;
			else
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
		} else if (type == Type.Variable) {
			if (bracketType == BracketType.Square)
				type = Type.VariableMessage;
			else
				throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
		} else if (! startTransform && type == Type.Unknown && bracketType == BracketType.Curly) {
			type = Type.Expression;
		} else if (! startTransform && type == Type.Unknown && bracketType == BracketType.Round) {
		    type = Type.Array;
		} else {
			throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
		}
		
		Node newParameter = new Node(Type.Unknown, file, line, column, false);
		newParameter.setParent(this);
		add(newParameter);
		this.bracketType = bracketType;
		opened = true;
		return newParameter;
	}
	
	Node closeBracket(BracketType bracketType, String file, int line, int column, int currentChar) throws TemplateException {
		
		Node _parent = parent;
		
		if (parent == null) {
			throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
		}
		
		if (! parent.opened) {
			throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
		}
		
		if (parent.closed) {
			throw new TemplateException("Invalid syntax at position '%s' - reach character '%c', bracket already closed!", positionInformation(file, line, column), currentChar);
		}
		
		if (parent.bracketType != bracketType) {
		    throw new TemplateException("Invalid syntax at position '%s' - reach character '%c', invalid bracket type!", positionInformation(file, line, column), currentChar);
		}
		
		parent.closed = true;
		
		if (type == Type.Unknown) {
		    if (parent.children.size() == 1)
		        parent.remove(this);
		    else 
		        throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
		} else {
		    validateSyntax(line, column, currentChar);
		}
//		setParent(null);
		
		return _parent;
	}

    Node newSibling(String file, int line, int column, int currentChar, NewTemplate template) throws TemplateException {
        validateAll(line, column, currentChar, false, template);
        if (parent.type != Type.QuoteMessage && parent.type != Type.VariableMessage && parent.type != Type.Array && parent.type != Type.QuoteFilterDyn && parent.type != Type.VariableFilterDyn) {
            throw new TemplateException("Invalid syntax at position '%s' - reach character '%c', this character is not allowed here!", positionInformation(file, line, column), currentChar);
        }
        Node newParameter = new Node(Type.Unknown, file, line, column, false);
        newParameter.setParent(parent);
        parent.add(newParameter);
        return newParameter;
    }

    Node openCommand(String file, int line, int column, int currentChar) throws TemplateException {
        if (type != Type.Unknown) {
            throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
        }
        type = Type.Command;
        return this;
    }
    
    Node closeCommand(String file, int line, int column, int currentChar) throws TemplateException {
        if (type != Type.Command) {
            throw new TemplateException("Invalid syntax at position '%s' - reach character '%c', no opened command!", positionInformation(file, line, column), currentChar);
        }
        type = Type.CommandClose;
        
        Node p = this;
        while (p != null && p.getType() != Type.Command) {
            p = p.parent;
        }
        if (p == null) {
            throw new TemplateException("Invalid syntax at position '%s' - reach close tag without opened tag!", positionInformation(fileInformation, line, column));
        }
        
        parent.remove(this);
        p.add(this);
        setParent(p);
        
        return this;
    }

    Type getType() {
        return type;
    }

    void setBuffer(String s) {
        buffer.append(s);
    }

    Node startCondition(String file, int line, int column, int currentChar) throws TemplateException {
        if (type != Type.Command) {
            throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
        }
        Node newParameter = new Node(Type.Unknown, file, line, column, false);
        newParameter.setParent(this);
        add(newParameter);
        return newParameter;
    }

	Node startSentence(String file, int line, int column, int currentChar) throws TemplateException {
		if (type != Type.Unknown) {
            throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
        }
		type = Type.Sentence;
		opened = true;
		return this;
	}

	Node stopSentence(String file, int line, int column, int currentChar) throws TemplateException {
		if (type != Type.Sentence) {
            throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
        }
		closed = true;
		return this;
	}

	Node startExpression(String file, int line, int column, int currentChar) throws TemplateException {
		if (type != Type.Unknown) {
            throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
        }
		type = Type.Expression;
		opened = true;
		return this;
	}

	Node stopExpression(String file, int line, int column, int currentChar) throws TemplateException {
		if (type != Type.Expression) {
            throw new TemplateException("Invalid syntax at position '%s' - reach character '%c'", positionInformation(file, line, column), currentChar);
        }
		closed = true;
		return this;
	}
	
	boolean allow(int currentChar) {
		if (type == Type.Text)
			return true;
		
		return (currentChar >= 'a' && currentChar <= 'z') 
				|| (currentChar >= 'A' && currentChar <= 'Z') 
				|| (currentChar >= '0' && currentChar <= '9')
				|| currentChar == '_'
				|| currentChar == '.'
				|| currentChar == '$'
				|| currentChar == '@'
				|| currentChar == '\''
				|| currentChar == '?'
				|| currentChar == '!'
				;
	}

    boolean isClosed() {
        return closed;
    }

    boolean isOpened() {
        return opened;
    }

//    private boolean writeVariable(Writer out, Map<String, ? extends Object> model) throws TemplateException, IOException {
//    	String varname = buffer.toString();
//    	if (optional == RenderType.Optional) {
//    		if (model.containsKey(varname)) {
//    			out.write(model.get(varname).toString());
//    			return true;
//    		}
//    	} else if (optional == RenderType.ReplacedByNameIfNotPresent) { 
//    		if (! model.containsKey(varname)) {
//    			out.write("#" + varname + "#");
//    			return true;
//    		} else {
//    			out.write(model.get(varname).toString());
//    			return true;
//    		}
//    	} else if (optional == RenderType.Required) {
//    		if (model.containsKey(varname)) {
//    			out.write(model.get(varname).toString());
//    			return true;
//    		} else {
//    			throw new TemplateException("Key '%s' is not present or has null value in the model map to render '%s' at position '%s'.", varname, bufferError, posinf());
//    		}
//    	} else {
//    		throw new TemplateException("writeSection type=" + type + " optional=" + optional);
//    	}
//    	return false;
//    }

	private String posinf() {
		return positionInformation(fileInformation, lineInformation, columnInformation);
	}
    
	/*
    protected Object applyFilters(Object s, List<Transform> functions) throws TemplateException {
        if (functions == null || functions.size() == 0)
            return s;
        Iterator<Transform> it = functions.iterator();
        if (it.hasNext()) {
        	Transform firstFilter = it.next();
            s = applyFilter(firstFilter, s);
            while (it.hasNext()) {
            	Transform secondFilter = it.next();
                s = applyFilter(secondFilter, s);
                firstFilter = secondFilter;
            };
        }
        return s;
    }*/
    
    private Object applyFilter(String filterName, Transform filter, Object s, boolean dyn) throws TemplateException {
        Class typeIn = Object.class; 
        boolean isArray = false;
//        String filterName="?????";
        try {
            Method firstMethod = getApply(filter);
            typeIn = firstMethod.getParameterTypes()[0]; 
            
            isArray = typeIn.isArray();
            if (isArray)
                typeIn = typeIn.getComponentType();
            boolean convertToString = typeIn == String.class;
            
            if (! isArray) {
            	if (! dyn) {
            		if (convertToString) {
            			if (s.getClass().isArray()) {
            				throw new TemplateException("Invalid filter chain. Filter '%s' expects '%s%s'. It receives '%s'. Unable to render '%s' at position '%s'.", filterName, typeIn.getCanonicalName(), isArray ? "[]" : "", 
            						s.getClass().getCanonicalName(), renderBufferError(), posinf());
            			} else {
            				s = filter.apply(s.toString());
            			}
            		} else {
                		s = filter.apply(s);
                	}
            	} else {
            		if (s.getClass().isArray() && ((Object[]) s).length == 1) {
            			Object o = ((Object[]) s)[0];
            			try {
            				s = filter.apply(o);
            			} catch (ClassCastException e) {
            	            throw new TemplateException("Invalid filter chain. Filter '%s' expects '%s%s'. It receives '%s'. Unable to render '%s' at position '%s'.", filterName, typeIn.getCanonicalName(), isArray ? "[]" : "", o.getClass().getCanonicalName(), renderBufferError(), posinf());
            	        }
            		} else {
            			s = filter.apply(s);
            		}
            	}
            } else {
                //http://www.java2s.com/Tutorial/Java/0125__Reflection/CreatearraywithArraynewInstance.htm
                Object[] objs = (Object[]) s;
                Object o = Array.newInstance(typeIn, objs.length);
                for (int i = 0; i < objs.length; i++) {
                    Object val = objs[i];
                    if (convertToString) {
                        Array.set(o, i, val.toString());
                    } else {
                        Array.set(o, i, val);
                    }
                }
                s = filter.apply(o);
            }
            return s;
        } catch (ClassCastException e) {
            throw new TemplateException("Invalid filter chain. Filter '%s' expects '%s%s'. It receives '%s'. Unable to render '%s' at position '%s'.", filterName, typeIn.getCanonicalName(), isArray ? "[]" : "", s.getClass().getCanonicalName(), renderBufferError(), posinf());
        } catch (TemplateException e) {
            throw e; 
        } catch (Exception e) {
        	e.printStackTrace();
            throw new TemplateException(e, "Unable to apply filter to render '%s' at position '%s' (%s).", renderBufferError(), posinf(), e.getMessage());
        }
    }

    private static Method getApply(Transform filter) {
        Method[] methods = filter.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals("apply"))
                return method;
        }
        return null;
    }

	
	Object value(Writer out, Map<String, ? extends Object> model, NewTemplate template) throws TemplateException, IOException {
		if (type == Type.Section) {
			for (Node c : children) {
				Object o = c.value(out, model, template);
				if (o != null) {
					out.write(String.valueOf(o));
				}
			}
			return null;
		} else if (type == Type.Text) {
			return buffer.toString();
		} else if (type == Type.Sentence) {
			return buffer.toString();
		} else if (type == Type.Variable) {
		 	return getInModel(model, buffer.toString());
		} else if (type == Type.VariableFilterDyn) {
			//FIXME
			
			String propertyKey = buffer.toString();
			Transform function = (Transform) getInModel(model, buffer.toString());
			
			if (function == null) {
				throw new TemplateException("No transform function '%s' to render '%s' at position '%s'.", propertyKey, renderBufferError(), posinf());
			}
			
			List<Object> parameters = createParameterList(model, template, out, children.subList(1, children.size()));

			function = (Transform) applyFilter(propertyKey, function, parameters.toArray(new Object[1]), true);
			
			if (function != null) {
				Object o = children.get(0).value(out, model, template);
				if (o != null) {
					return applyFilter(propertyKey, function, o, false);
				} else {
					return null;
				}
			} else {
				throw new TemplateException("No transform function named '%s' is associated with the template to render '%s' at position '%s'.", propertyKey, renderBufferError(), posinf());
			}

		} else if (type == Type.QuoteFilterDyn) {
			//FIXME
			
			String propertyKey = buffer.toString();
			if (propertyKey == null) {
				return null;
			}
			
			
			Transform function = template.getTransform(propertyKey); 
			if (function == null) {
				throw new TemplateException("No transform function '%s' to render '%s' at position '%s'.", propertyKey, renderBufferError(), posinf());
			}

			List<Object> parameters = createParameterList(model, template, out, children.subList(1, children.size()));
			
			function = (Transform) applyFilter(propertyKey, function, parameters.toArray(new Object[1]), true);
			
			if (function != null) {
				Object o = children.get(0).value(out, model, template);
				if (o != null) {
					return applyFilter(propertyKey, function, o, false);
				} else {
					return null;
				}
			} else {
				throw new TemplateException("No transform function named '%s' is associated with the template to render '%s' at position '%s'.", propertyKey, renderBufferError(), posinf());
			}
			
			
		} else if (type == Type.VariableFilter) {
			return applyTransformOnNode(buffer.toString(), children.get(0), model, template, out, false);
		} else if (type == Type.QuoteFilter) {
			return applyTransformOnNode(buffer.toString(), children.get(0), model, template, out, true);
		} else if (type == Type.VariableMessage) { 
			return applyMessage(model, template, out, false);
		} else if (type == Type.QuoteMessage) { 
			return applyMessage(model, template, out, true);
		} else if (type == Type.Array/* || type == Type.ArrayExpansion*/) {
			List<Object> parameters = createParameterList(model, template, out, children);
			if (parameters == null)
				return null;
			else
				return parameters.toArray(new Object[1]);
		} else if (type == Type.Command) {
			String command = buffer.toString();
			if ("if".equals(command)) {
				Node test = children.get(0);
				Boolean result = (Boolean) test.value(out, model, template);
				if (result) {
					for (int i=1; i<children.size(); i++) {
						Object o = children.get(i).value(out, model, template);
						if (o != null) {
							out.write(String.valueOf(o));
						}
					}
				} 
				return null;
			} else {
				throw new TemplateException("Unsupported command " + command);
			}
		} else if (type == Type.CommandClose) {
			return null;
		} else if (type == Type.Literral) {
			String value = buffer.toString();
			if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
				return Boolean.parseBoolean(value);
			return Integer.parseInt(value);
		} else if (type == Type.Expression) { 
			System.out.println("bufer=" + buffer.toString());
			String[] output = ReversePolishNotation.infixToRPN(buffer.toString().split(" "));
			Node tmp = new Node(Type.Text, "-", 0, 0, false); // TODO
			for (String part : output) {
				System.out.println("part=" + part);
				StringReader sr = new StringReader("~" + part + "~");
				template.parse(tmp, "-", 0, 0, sr);
				System.out.println("part=" + part + " ==> " + tmp.representation());
			}
			throw new TemplateException("A implementer " + buffer.toString());
		} else {
			throw new TemplateException("Unsupported node type=" + type);
		}
	}

	private Object applyMessage(Map<String, ? extends Object> model, NewTemplate template, Writer out, boolean quote) throws TemplateException, IOException {
		String propertyKey = ! quote ? (String) getInModel(model, buffer.toString()) : buffer.toString();
		if (propertyKey == null) {
			return null;
		}
		if (! template.messages.containsKey(propertyKey)) {
			if (messageOptional == RenderType.OptionalMessage) {
				return null;
			} else if (messageOptional == RenderType.ReplacedByPropertyNameIfNotPresent) {
				return propertyKey;
			}
			throw new TemplateException("No property key '%s' to render '%s' at position '%s'.", propertyKey, renderBufferError(), posinf());
		}
		List<Object> parameters = createParameterList(model, template, out, children);
		if (parameters == null)
			return null;
		else
			return template.messages.format(propertyKey, parameters);
	}

	private static boolean isFilterTypeNode(Node n) {
		return Arrays.asList(Type.UnknownFilter, Type.QuoteFilter, Type.VariableFilter, Type.QuoteFilterDyn, Type.VariableFilterDyn).contains(n.type);
	}
	
	private List<Object> createParameterList(Map<String, ? extends Object> model, NewTemplate template, Writer out, List<Node> items) throws TemplateException, IOException {
		List<Object> parameters = new ArrayList<Object>();
		for (Node child : items) {
			System.out.println(child.representation());
			System.out.println(isFilterTypeNode(child));
			if (isFilterTypeNode(child)) {
				if (child.children.get(0).type == Type.UnknownFilter) {
					String varname = child.buffer.toString();
					Transform transform;
					System.out.println("## " + varname + " " + child.type + " " );
					if (child.type == Type.QuoteFilterDyn || child.type == Type.QuoteFilter) {
						transform = template.getTransform(varname);
					} else {
						transform = (Transform) getInModel(model, varname);
					}
					boolean dynFilter = (child.type == Type.QuoteFilterDyn || child.type == Type.VariableFilterDyn);
					if (dynFilter) {
						List<Object> pam = createParameterList(model, template, out, child.children.subList(1, child.children.size()));
						parameters.add(applyFilter(varname, transform, pam.toArray(new Object[1]), dynFilter));
					} else {
						parameters.add(transform);
					}
				} else {
					Object o = child.value(out, model, template);
					if (o == null) {
						return null;
					}
					parameters.add(o);
				}
			} else  if (child.type != Type.ArrayExpansion) {
				Object o = child.value(out, model, template);
				if (o == null) {
					return null;
				}
				parameters.add(o);
			} else {
				Object o = child.getInModel(model, child.buffer.toString());
				if (o.getClass().isArray()) {
					for (Object p : (Object[]) o) {
						parameters.add(p);
					}
				} else {
					for (Object p : (Iterable) o) {
						parameters.add(p);
					}
            	}
			}
			System.out.println("--");
		}
		return parameters;
	}
	

	
	private Object applyTransformOnNode(String varname, Node node, Map<String, ? extends Object> model, NewTemplate template, Writer out, boolean quote) throws TemplateException, IOException {
		Transform transform = ! quote ? (Transform) getInModel(model, buffer.toString()) : template.getTransform(varname);
		if (transform != null) {
			Object o = node.value(out, model, template);
			if (o != null) {
				return applyFilter(varname, transform, o, false);
			} else {
				return null;
			}
		} else {
			if (quote) {
				throw new TemplateException("No transform function named '%s' is associated with the template to render '%s' at position '%s'.", varname, renderBufferError(), posinf());
			} else {
				return null;
			}
		}
	}
	
	private Object getInModel(Map<String, ? extends Object> model, String varname) throws TemplateException {
//		String varname = buffer.toString();
		if (optional == RenderType.Optional) {
			if (model.containsKey(varname)) {
				return model.get(varname);
			}
		} else if (optional == RenderType.ReplacedByNameIfNotPresent) { 
			if (! model.containsKey(varname)) {
				return varname;
			} else {
				return model.get(varname);
			}
		} else if (optional == RenderType.Required) {
			if (model.containsKey(varname)) {
				return model.get(varname);
			} else {
				throw new TemplateException("Key '%s' is not present or has null value in the model map (needed for '%s' at position '%s').", varname, renderBufferError(), posinf());
			}
		} else {
			throw new TemplateException("writeSection type=" + type + " optional=" + optional);
		}
		return null;
	}

	private Object renderBufferError() {
		String b = bufferError.toString();
//		for (Node c : children) {
//			b += c.renderBufferError();
//		}
		return b;
	}

	public Node startLitteral(String file, int line, int column, int currentChar) {
		type = Type.Literral;
		buffer.write(currentChar);
		return this;
	}

}
