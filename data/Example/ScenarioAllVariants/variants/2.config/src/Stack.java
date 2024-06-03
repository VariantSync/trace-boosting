import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;

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
	
	public void encrypt() {
		log = "encrypt";
		System.out.println("encrypt");
	}
	
	public void decrypt() {
		log = "decrypt";
		System.out.println("decrypt");
	}
}