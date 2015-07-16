package nez.checker.js;

import java.util.ArrayList;

import nez.ast.Tag;
import nez.checker.ModifiableTree;

public class NodeUtil {
	public NodeUtil(){
	}
	
	public final static ArrayList<String> getFieldElements(ModifiableTree fieldNode){
		ArrayList<String> elements = new ArrayList<String>();
		while(fieldNode.is(JSTag.TAG_FIELD)){
			elements.add(fieldNode.get(1).getText());
			fieldNode = fieldNode.get(0);
		}
		elements.add(fieldNode.getText());
		return elements;
	}
	
	public final static void fillNullNode(ModifiableTree node){
		if(node.size() > 0){
			for(int i = 0; i < node.size(); i++){
				if(node.get(i) == null){
					node.set(i, new ModifiableTree(Tag.tag("Text"), null, 0, 0, 0, null));
				} else {
					fillNullNode(node.get(i));
				}
			}
		}
	}
	
}
