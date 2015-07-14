package nez.checker.js;

import java.util.ArrayList;

import nez.checker.ModifiableTree;

public class JSVariable extends JSData {

	public JSVariable(String name, JSData parent, ModifiableTree node){
		this.name = name;
		this.node = node;
		ArrayList<JSData> path = parent.getPath();
		path.add(parent);
		this.path = path;
		if(parent.getClass() == JSFunction.class){
			this.init(name, (JSFunction)parent, node);
		} else if(parent.getClass() == JSObject.class){
			this.init(name, (JSObject)parent, node);
		}
	}
	
	private void init(String name, JSFunction parent, ModifiableTree node){
		parent.addVar(this);
		this.fixedName = "VAR" + parent.issueLocalVarIndentifier();
	}
	
	private void init(String name, JSObject parent, ModifiableTree node){
		parent.addVar(this);
		this.fixedName = "VAR" + parent.issueLocalVarIndentifier();
	}
}
