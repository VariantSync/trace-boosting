import java.util.ArrayList;
import java.util.List;

public class Stack {

	private List<Object> elements = new ArrayList<Object>();
	
	public void push(Object element) {
		elements.add(element);
	}

	public Object pop() {
		return elements.get(0);
	}
}