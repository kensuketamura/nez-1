package nez.checker.js;

import java.util.ArrayList;

import nez.checker.ModifiableTree;

public class JSRewriter {
	
	private StringBuilder fixedSource;
	private ArrayList<JSFunction> funs;
	private ArrayList<JSObject> objs;
	private ArrayList<JSVariable> vars;
	private ArrayList<JSData> undecidedData;
	
	public JSRewriter() {
		this.fixedSource = new StringBuilder();
		this.funs = new ArrayList<JSFunction>();
		this.objs = new ArrayList<JSObject>();
		this.vars = new ArrayList<JSVariable>();
		this.undecidedData = new ArrayList<JSData>();
	}
	
	public String rewrite(ModifiableTree po) {
		JSFunction top = new JSFunction("TOP", po);
		this.find(po, top);
		return fixedSource.toString();
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
		String name = "";
		String localName = "";
		//TODO
		ModifiableTree parentNode = node.getParent();
		if(parentNode.is(JSTag.TAG_ASSIGN)){
			if(node.get(2).is(JSTag.TAG_NAME)){
				localName = node.get(2).getText();
			}
			if(parentNode.get(0).is(JSTag.TAG_NAME)){
				name = parentNode.get(0).getText();
			} else if(parentNode.get(0).is(JSTag.TAG_FIELD)){
				//TODO field
				scope = scope.searchAvailableFunc(localName);
				name = parentNode.get(0).get(1).getText();
			}
		}
		if(node.get(2).is(JSTag.TAG_NAME)){
			name = node.get(2).getText();
		} else {}
		JSFunction newFunc = new JSFunction(name, scope, node);
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
