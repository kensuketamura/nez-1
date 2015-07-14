package nez.checker.js;

import java.util.ArrayList;

import nez.checker.ModifiableTree;

public class JSData {
	protected ArrayList<JSData> children;
	protected String name;
	protected String fixedName;
	protected ArrayList<JSData> path;
	protected JSData parent;
	protected ModifiableTree node;
	
	public String getOriginalName(){
		return name;
	}
	
	public String getFixedName(){
		return fixedName;
	}
	
	public String getFixedFullName(){
		String fixedFullName = "";
		for(JSData jd : this.path){
			fixedFullName += jd.getFixedName() + "_";
		}
		fixedFullName += this.getFixedName();
		return fixedFullName;
	}
	
	public JSData getParent(){
		return this.parent;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<JSData> getPath(){
		return (ArrayList<JSData>) this.path.clone();
	}
	
	public Boolean addFunc(JSFunction jf){
		return false;
	}
	
	public Boolean addVar(JSVariable jv){
		return false;
	}
	
	public Boolean addObj(JSObject jo){
		return false;
	}
	
	public String issueLocalFuncIndentifier(){
		return null;
	}
	public String issueLocalVarIndentifier(){
		return null;
	}
	public String issueLocalObjIndentifier(){
		return null;
	}
	
	public JSData searchAvailableData(String name){
		return null;
	}
	public JSFunction searchAvailableFunc(String name){
		return null;
	}
	public JSObject searchAvailableObj(String name){
		return null;
	}
	public JSVariable searchAvailableVar(String name){
		return null;
	}
}
