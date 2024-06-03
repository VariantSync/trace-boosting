import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;

public class Stack {

	private List<Object> elements = new ArrayList<Object>();
	
	public void push(Object element) {
		elements.add(element);
	}

	public Object pop() {
		return elements.get(0);
	}
	
	public int size() {
		return elements.size();
	}
	
	public void encrypt() {
		System.out.println("encrypt");
	}
	
	public void decrypt() {
		System.out.println("decrypt");
	}
}