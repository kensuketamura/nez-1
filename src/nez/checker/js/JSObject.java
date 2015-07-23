package nez.checker.js;

import nez.checker.ModifiableTree;
import nez.checker.SourceBuilder;

public class JSObject extends JSData {
	private PArrayList<JSData> properties;
	private PArrayList<JSVariable> localVars;
	private PArrayList<JSFunction> localFuncs;
	private PArrayList<JSObject> localObjs;
	
	public JSObject(String name, JSData parent, ModifiableTree node){
		this.name = name;
		this.parent = parent;
		this.node = node;
		this.localFuncs = new PArrayList<JSFunction>();
		this.localObjs = new PArrayList<JSObject>();
		this.localVars = new PArrayList<JSVariable>();
		PArrayList<JSData> path = (PArrayList<JSData>)parent.getPath();
		path.add(parent);
		this.path = path;
		if(parent.getClass() == JSFunction.class || parent.getClass() == JSObject.class){
			parent.addObj(this);
			this.fixedName = "OBJ" + parent.issueLocalObjIndentifier();
		}
	}
	
	public Boolean addFunc(JSFunction jf){
		return this.localFuncs.add(jf);
	}
	
	public Boolean addVar(JSVariable jv){
		return this.localVars.add(jv);
	}
	
	public Boolean addObj(JSObject jo){
		return this.localObjs.add(jo);
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
	
	public void printVarDecl(SourceBuilder builder){
		for(JSVariable var : this.localVars){
			builder.appendNewLine("var " + var.getFixedFullName() + ";  //" + var.getPath().toString() + "::" + var.getOriginalName());
		}
		for(JSObject obj : this.localObjs){
			builder.appendNewLine("var " + obj.getFixedFullName() + ";  //" + obj.getPath().toString() + "::" + obj.getOriginalName());
		}
	}
}
