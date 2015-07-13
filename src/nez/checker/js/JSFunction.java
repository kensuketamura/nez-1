package nez.checker.js;

import java.util.ArrayList;

import nez.checker.ModifiableTree;

public class JSFunction extends JSData {
	private ArrayList<JSVariable> localVars;
	private ArrayList<JSFunction> localFuncs;
	private ArrayList<JSObject> localObjs;
	
	public JSFunction(String name, JSData parent, ModifiableTree node) {
		this.name = name;
		this.node = node;
		ArrayList<JSData> path = parent.getPath();
		path.add(parent);
		this.path = path;
	}
	
	public String issueLocalFuncIndentifier(){
		return this.issueIndentifier(DATATYPE.FUNCTION);
	}
	public String issueLocalVarIndentifier(){
		return this.issueIndentifier(DATATYPE.VARIABLE);
	}
	public String issueLocalObjIndentifier(){
		return this.issueIndentifier(DATATYPE.OBJECT);
	}
	
	private enum DATATYPE {
		FUNCTION,
		OBJECT,
		VARIABLE
	}
	
	private String issueIndentifier(DATATYPE type){
		String identifier = "";
		int num = 0;
		switch(type){
		case FUNCTION:
			num = this.localFuncs.size();
		case OBJECT:
			num = this.localObjs.size();
		case VARIABLE:
			num = this.localVars.size();
		}
		if(num >= 100){
			identifier = Integer.toString(num);
		} else if(100 > num && num >= 10){
			identifier = "0" + Integer.toString(num);
		} else if(10 > num){
			identifier = "00" + Integer.toString(num);
		}
		return identifier;
	}
}
