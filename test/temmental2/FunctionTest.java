package temmental2;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

public class FunctionTest extends AbstractTestElement {

	private Transform<String,String> tupper;
	private Transform<String,String> tquote;
	private Method mupper;
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		tupper = new Transform<String, String>() {
			@Override
			public String apply(String value) throws TemplateException {
				return value.toUpperCase();
			}
		};
		tquote = new Transform<String, String>() {
			@Override
			public String apply(String value) throws TemplateException {
				return "'" + value + "'";
			}
		};
		mupper = String.class.getDeclaredMethod("toUpperCase");
	}
	
	@Test
	public void testFunctionQuote() throws TemplateException {
		Function f = function(identifier("'upper", "-:l1:c2"), identifier("$text", "-:l1:c2"));
		
		populateTransform("upper", tupper);
		populateModel("text", "Something...");
		
		assertEquals("SOMETHING...", f.writeObject(transforms, model, null));
	}
	
	@Test
	public void testFunctionQuoteWithMethod() throws TemplateException {
		Function f = function(identifier("'upper", "-:l1:c2"), identifier("$text", "-:l1:c2"));
		
		populateTransform("upper", mupper);
		populateModel("text", "Something...");
		
		assertEquals("SOMETHING...", f.writeObject(transforms, model, null));
	}
	
	@Test
	public void testFunctionVarRequiredAndFound() throws TemplateException {
		Function f = function(identifier("$f", "-:l1:c2"), identifier("$text", "-:l1:c2"));
		
		populateTransform("upper", tupper);
		
		populateModel("f", "upper");
		populateModel("text", "Something...");
		
		assertEquals("SOMETHING...", f.writeObject(transforms, model, null));
	}
	
	@Test
	public void testFunctionVarRequiredAndNotFound_AnExceptionIsThrown() throws TemplateException {
		Function f = function(identifier("$f", "-:l1:c2"), identifier("$text", "-:l1:c1"));
		
		populateTransform("upper", tupper);
		
		populateModel("f", "lower");
		populateModel("text", "Something...");
		
		try {
			f.writeObject(transforms, model, null);
			fail("An exception must be raised.");
		} catch (TemplateException e) {
			assertEquals("No transform function named 'lower' is associated with the template for rendering '\u2026:$f' at position '-:l1:c2'.", e.getMessage());
		}
	}
	
	@Test
	public void testFunctionVarNotFoundButOptional() throws TemplateException {
		Function f = function(identifier("$f", "-:l1:c2"), 
				function(identifier("$g?", "-:l1:c2"), identifier("$text", "-:l1:c2")));
		
		populateTransform("upper", tupper);
		populateTransform("quote", tquote);
		
		populateModel("f", "upper");
		populateModel("text", "Something...");
		
		assertNull(f.writeObject(transforms, model, null));
	}
	
	@Test
	public void testFunctionNoInput() throws TemplateException {
		Function f = function(identifier("'upper", "-:l1:c2"), identifier("$text?", "-:l1:c2"));
		
		populateTransform("upper", tupper);
		
		assertNull(f.writeObject(transforms, model, null));
	}
	
	@Test
	public void testFunctionChain() throws TemplateException {
		Function f = function(identifier("'upper", "-:l1:c2"), 
				function(identifier("'quote", "-:l1:c2"), identifier("$text", "-:l1:c2")));
		
		populateTransform("upper", tupper);
		populateTransform("quote", tquote);
		populateModel("text", "Something...");
		
		assertEquals("'SOMETHING...'", f.writeObject(transforms, model, null));
	}
	
	@Test
	public void testFunctionChainMethod1() throws TemplateException {
		Function f = function(identifier("'upper", "-:l1:c2"), 
				function(identifier("'quote", "-:l1:c2"), identifier("$text", "-:l1:c2")));
		
		populateTransform("upper", mupper);
		populateTransform("quote", tquote);
		populateModel("text", "Something...");
		
		assertEquals("'SOMETHING...'", f.writeObject(transforms, model, null));
	}
	
	@Test
	public void testFunctionChainMethod2() throws TemplateException {
		Function f = function(identifier("'quote", "-:l1:c2"), 
				function(identifier("$f", "-:l1:c2"), identifier("$text", "-:l1:c2")));
		
		populateTransform("upper", mupper);
		populateTransform("quote", tquote);
		
		populateModel("text", "Something...");
		populateModel("f", "upper");
		
		assertEquals("'SOMETHING...'", f.writeObject(transforms, model, null));
	}
	
	@Test
	public void testInvalidInputTypeMethod() throws TemplateException, IOException {
		Function f = function(identifier("$f", "-:l1:c2"), 2);
		
		populateTransform("upper", mupper);
		populateModel("f", "upper");
		
		try {
			f.writeObject(transforms, model, null);
			fail("An exception must be raised.");
		} catch (TemplateException e) {
			assertEquals("Unable to render '…:$f' at position '-:l1:c2'. The function upper expects java.lang.String. It receives java.lang.Integer.", e.getMessage());
		}
	}
	
	@Test
	public void testInvalidInputTypeTransform() throws TemplateException, IOException {
		Function f = function(identifier("$f", "-:l1:c2"), 2);
		
		populateTransform("upper", tupper);
		populateModel("f", "upper");
		
		try {
			f.writeObject(transforms, model, null);
			fail("An exception must be raised.");
		} catch (TemplateException e) {
			assertEquals("Unable to render '…:$f' at position '-:l1:c2'. The function upper expects java.lang.String. It receives java.lang.Integer.", e.getMessage());
		}
	}
	
	@Test
	public void testFunctionCharAtMethodNoParameter() throws TemplateException, NoSuchMethodException, SecurityException {
		Function f = function(identifier("'charat", "-:l1:c2"), identifier("$text", "-:l1:c2"));
		
		Method mcharat = String.class.getDeclaredMethod("charAt", int.class);
					
		populateTransform("charat", mcharat);
		
		populateModel("text", "Something...");
		
		try {
			f.writeObject(transforms, model, null);
			fail("An exception must be raised.");
		} catch (TemplateException e) {
			assertEquals("Unable to render '…:'charat' at position '-:l1:c2'. The function charat expects one parameter but is called without parameter!", e.getMessage());
		}
	}
	
	@Test
	public void testFunctionSubstringTransformNoParameter() throws TemplateException, NoSuchMethodException, SecurityException {
		Function f = function(identifier("'substr", "-:l1:c2"), identifier("$text", "-:l1:c2"));
		
		Transform tsubstr = new Transform<Integer[], Transform>() {
			@Override
			public Transform apply(final Integer[] indexes) throws TemplateException {
				return new Transform<String, String>() {
					@Override
					public String apply(String value) throws TemplateException {
						return value.substring(indexes[0], indexes[1]);
					}
				};
			}
		};
					
		populateTransform("substr", tsubstr);
		
		populateModel("text", "Something...");
		
		try {
			f.writeObject(transforms, model, null);
			fail("An exception must be raised.");
		} catch (TemplateException e) {
			assertEquals("Unable to render '…:'substr' at position '-:l1:c2'. The function substr expects java.lang.Integer[]. It receives java.lang.String.", e.getMessage());
		}
	}
	

}
