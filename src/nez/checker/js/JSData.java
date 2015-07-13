package nez.checker.js;

import java.util.ArrayList;

public class JSData {
	protected ArrayList<JSData> children;
	protected String name;
	protected String fixedName;
	protected ArrayList<JSData> path;
	protected JSData parent;
	
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
}
