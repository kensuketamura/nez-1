package nez.checker.js;

import nez.checker.ModifiableTree;
import nez.checker.js.FSharpScope.ScopeType;


public class FSharpFunc {
	public String name;
	public String fullname;
	public String argsStr;
	public boolean isMember = false;
	public int uniqueKey = 0;
	public FSharpScope scope;
	ModifiableTree node;
	
	public FSharpFunc(String name){
		this.name = name;
	}
	public FSharpFunc(String name, String prefixName, boolean isMember, ModifiableTree node){
		this.name = name;
		if(isMember){
			this.fullname = prefixName.substring(0, prefixName.length() - 1) + "0." + name;
		} else {
			this.fullname = prefixName + name;
		}
		this.isMember = isMember;
		this.node = node;
		this.setArgsString();
	}
	public FSharpFunc(FSharpScope scope){
		this.scope = scope;
		this.name = scope.name;
		this.fullname = scope.getFullname();
		this.node = scope.node;
		if(scope.parent.type == ScopeType.OBJECT){
			this.isMember = true;
		} else {
			this.isMember = false;
		}
		this.setArgsString();
	}
	public FSharpFunc(ModifiableTree node){
		
		this.isMember = false;
		
		ModifiableTree nameNode = null;
		if(node.get(2).is(JSTag.TAG_NAME)){
			nameNode = node.get(2);
		} else {
			ModifiableTree parentNode = node.getParent();
			if(parentNode.is(JSTag.TAG_VAR_DECL)){
				if(node.getParent().get(0).is(JSTag.TAG_NAME)){
					nameNode = node.getParent().get(0);
				}
			} else if(parentNode.is(JSTag.TAG_ASSIGN)){
				if(parentNode.get(0).is(JSTag.TAG_NAME)){
					nameNode = parentNode.get(0);
				} else if(parentNode.get(0).is(JSTag.TAG_FIELD)){
					nameNode = parentNode.get(0).get(1);
				}
			} else if(parentNode.is(JSTag.TAG_PROPERTY)){
				if(parentNode.get(0).is(JSTag.TAG_NAME)){
					nameNode = parentNode.get(0);
					this.isMember = true;
				}
			}
		}
		if(nameNode == null){
			//lambda
		}
		this.name = nameNode.getText();
		this.node = node;
		this.setArgsString();
	}
	
	
	public String addChild(){
		String name = this.name + this.uniqueKey;
		this.uniqueKey++;
		return name;
	}
	
	public String getCurrentName(){
		return this.name + this.uniqueKey;
	}
	
	public String getTrueName(){
		return this.name;
	}
	
	public String getFullname(){
		return this.fullname;
	}
	
	private void setArgsString(){
		String argsStr = "";
		ModifiableTree argsNode = this.node.get(4);
		if(argsNode.is(JSTag.TAG_LIST)){
			for(int i = 0; i < argsNode.size(); i++){
				if(i != 0){
					argsStr += " ";
				}
				argsStr += argsNode.get(i).getText();
			}
		}
		this.argsStr = argsStr;
	}
	
	public String toString(){
		return name;
	}
}