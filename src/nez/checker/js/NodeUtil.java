package nez.checker.js;

import java.util.ArrayList;

import nez.checker.ModifiableTree;

public class NodeUtil {
	public NodeUtil(){
	}
	
	public ArrayList<String> getFieldElements(ModifiableTree fieldNode){
		ArrayList<String> elements = new ArrayList<String>();
		while(fieldNode.is(JSTag.TAG_FIELD)){
			elements.add(fieldNode.get(1).getText());
			fieldNode = fieldNode.get(0);
		}
		elements.add(fieldNode.getText());
		return elements;
	}
}
