package nez.checker.js;

import java.util.ArrayList;

public class JSObject extends JSData {
	private ArrayList<JSData> properties;
	private ArrayList<JSVariable> localVars;
	private ArrayList<JSFunction> localFuncs;
	private ArrayList<JSObject> localObjs;
	
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
}
