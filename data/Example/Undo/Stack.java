import java.util.ArrayList;
import java.util.List;

public class Stack {

	private List<Object> elements = new ArrayList<Object>();
	
	private String log;
	
	public void push(Object element) {
		log = "push" + element;
		elements.add(element);
	}

	public Object pop() {
		log = "pop";
		return elements.get(0);
	}

	public int size() {
		return elements.size();
	}
	
	public void undo() {
		System.out.println("undo " + log);
	}
}