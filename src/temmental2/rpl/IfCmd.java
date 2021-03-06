package temmental2.rpl;

import java.util.ArrayList;
import java.util.List;

import temmental2.Reader;
import temmental2.StackException;

public class IfCmd extends Reader implements Command {

	private List condition;
	private List iftrue;
	private List iffalse;

	public IfCmd() {
		super(new ArrayList<>());
		condition = new ArrayList<>();
		iftrue = new ArrayList<>();
		iffalse = new ArrayList<>();
	}

	public void tocond() {
		this.condition = operations;
		operations = new ArrayList<>();
	}

	public void totrue() {
		this.iftrue = operations;
		operations = new ArrayList<>();
	}
	
	public void tofalse() {
		this.iffalse = operations;
		operations = new ArrayList<>();
	}

	@Override
	public void apply(RplStack stack) throws StackException {
		RplStack.push_operations(stack, condition, false, true);
		boolean r = (Boolean) stack.pop();
        if (r) {
        	RplStack.push_operations(stack, iftrue, false, true);
        } else {
        	RplStack.push_operations(stack, iffalse, false, true);
        }
	}

}
