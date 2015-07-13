package nez.checker.js;

import nez.checker.ModifiableTree;

public class JSRewriter {
	public JSRewriter() {
		// TODO Auto-generated constructor stub
	}
	

	private void find(ModifiableTree node, JSData scope){
		JSData nextScope = scope;
		Boolean continueFind = true;
		if(node.is(JSTag.TAG_FUNC_DECL)){
			findFunction(node, nextScope);
			continueFind = false;
			find(node.get(6), nextScope);
		} else if(node.is(JSTag.TAG_OBJECT)){
			findObject(node, nextScope);
		} else if(node.is(JSTag.TAG_PROPERTY)){
			findProperty(node, nextScope);
		} else if(node.is(JSTag.TAG_VAR_DECL) && !node.get(1).is(JSTag.TAG_FUNC_DECL) && !node.get(1).is(JSTag.TAG_OBJECT)){
			findVarible(node, nextScope);
		}
		if(node.size() > 0 && continueFind){
			for(int i = 0; i < node.size(); i++){
				find(node, nextScope);
			}
		}
	}

	private JSFunction findFunction(ModifiableTree node, JSData scope){
		JSFunction newFunction = null;
		//TODO
		return newFunction;
	}

	private JSObject findObject(ModifiableTree node, JSData scope){
		JSObject newObject = null;
		//TODO
		return newObject;
	}
	
	private JSVariable findVarible(ModifiableTree node, JSData scope) {
		JSVariable newVariable = null;
		//TODO
		return newVariable;
	}
	
	private JSData findProperty(ModifiableTree node, JSData scope){
		JSData newProperty = null;
		//TODO
		return newProperty;
	}
}
