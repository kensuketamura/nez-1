package nez.checker.js;

import nez.checker.ModifiableTree;

public class JSFunction extends JSData {
	private PArrayList<JSVariable> localVars;
	private PArrayList<JSFunction> localFuncs;
	private PArrayList<JSObject> localObjs;
	private PArrayList<JSVariable> args;
	public String localName;
	public Boolean isConstructor = false;
	
	public JSFunction(String name, JSData parent, ModifiableTree node) {
		this.name = name;
		this.node = node;
		this.parent = parent;
		this.localFuncs = new PArrayList<JSFunction>();
		this.localObjs = new PArrayList<JSObject>();
		this.localVars = new PArrayList<JSVariable>();
		this.args = new PArrayList<JSVariable>();
		PArrayList<JSData> path = (PArrayList<JSData>)parent.getPath();
		path.add(parent);
		this.path = path;
		
		ModifiableTree argsNode = node.get(4);
		analyzeArgs(argsNode);
		if(parent.getClass() == JSFunction.class || parent.getClass() == JSObject.class){
			parent.addFunc(this);
			this.fixedName = "FUN" + parent.issueLocalFuncIndentifier();
		}
	}
	
	public JSFunction(String name, ModifiableTree node){
		this.name = name;
		this.node = node;
		this.fixedName = "TOP";
		this.path = new PArrayList<JSData>();
		this.localFuncs = new PArrayList<JSFunction>();
		this.localObjs = new PArrayList<JSObject>();
		this.localVars = new PArrayList<JSVariable>();
		this.args = new PArrayList<JSVariable>();
		this.parent = null;
	}
	
	private void analyzeArgs(ModifiableTree argsNode){
		ModifiableTree arg = null;
		for(int i = 0; i < argsNode.size(); i++){
			arg = argsNode.get(i);
			this.args.add(new JSVariable(arg.getText(), this, arg));
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
			break;
		case OBJECT:
			num = this.localObjs.size();
			break;
		case VARIABLE:
			num = this.localVars.size();
			break;
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
	
	@Override
	public JSData searchAvailableData(String name){
		JSData result = null;
		result = this.searchAvailableFunc(name);
		if(result == null){
			result = this.searchAvailableObj(name);
		}
		if(result == null){
			result = this.searchAvailableVar(name);
		}
		return result;
	}
	
	@Override
	public JSFunction searchAvailableFunc(String name){
		JSFunction result = null;
		for(JSFunction func : this.localFuncs){
			if(func.getOriginalName().contentEquals(name)){
				result = func;
			}
		}
		if(this.parent != null && result == null){
			result = this.parent.searchAvailableFunc(name);
		}
		return result;
	}
	
	@Override
	public JSObject searchAvailableObj(String name){
		JSObject result = null;
		for(JSObject obj : this.localObjs){
			if(obj.getOriginalName().contentEquals(name)){
				result = obj;
			}
		}
		if(this.parent != null && result == null){
			result = this.parent.searchAvailableObj(name);
		}
		return result;
	}
	
	@Override
	public JSVariable searchAvailableVar(String name){
		JSVariable result = null;
		for(JSVariable var : this.localVars){
			if(var.getOriginalName().contentEquals(name)){
				result = var;
			}
		}
		if(this.parent != null && result == null){
			result = this.parent.searchAvailableVar(name);
		}
		return result;
	}
	
	public String toString(){
		StringBuilder out = new StringBuilder();
		out.append("\nName     : " + this.name + "\n");
		out.append("FixedName: " + this.getFixedFullName() + "\n");
		out.append("Args     : " + this.args.toString() + "\n");
		out.append("Path     : " + this.path.toString() + "\n");
		return out.toString();
	}
}
