package nez.checker.js;

import nez.checker.ModifiableTree;

public class JSVariable extends JSData {

	public JSVariable(String name, JSData parent, ModifiableTree node){
		this.name = name;
		this.node = node;
		this.parent = parent;
		PArrayList<JSData> path = (PArrayList<JSData>)parent.getPath();
		path.add(parent);
		this.path = path;
		if(parent.getClass() == JSFunction.class || parent.getClass() == JSObject.class){
			parent.addVar(this);
			this.fixedName = "VAR" + parent.issueLocalVarIndentifier();
		}
	}
}
