package temmental2.rpl;

import java.util.ArrayList;
import java.util.List;

import temmental2.Reader;
import temmental2.StackException;

public class WhileCmd  extends Reader implements Command {

	private List<Object> loopst;
	private List<Object> condition;
	
	public WhileCmd() {
		super(new ArrayList<>());
		this.condition = new ArrayList<>();
		this.loopst = new ArrayList<>();
	}

	public void tocond() {
		this.condition = operations;
		operations = new ArrayList<>();
	}
	
	public void toloopst() {
		this.loopst = operations;
		operations = new ArrayList<>();
	}

	@Override
	public void apply(RplStack stack) throws StackException {
		RplStack.push_operations(stack, condition, false, true);
		boolean r = (Boolean) stack.pop();
		while (r) {
			RplStack.push_operations(stack, loopst, false, true);
			RplStack.push_operations(stack, condition, false, true);
			r = (Boolean) stack.pop();
		}
	}

}
