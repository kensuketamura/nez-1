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
			nextScope = findFunction(node, nextScope);
		} else if(node.is(JSTag.TAG_OBJECT)){
			nextScope = findObject(node, nextScope);
		} else if(node.is(JSTag.TAG_PROPERTY)){
			JSData prop = findProperty(node, nextScope);
			if(prop.getClass() == JSFunction.class || prop.getClass() == JSObject.class){
				nextScope = prop;
			}
		} else if(node.is(JSTag.TAG_VAR_DECL) && !node.get(1).is(JSTag.TAG_FUNC_DECL) && !node.get(1).is(JSTag.TAG_OBJECT)){
			findVariable(node, nextScope);
		}
		if(node.size() > 0 && continueFind){
			for(int i = 0; i < node.size(); i++){
				if(node.get(i) != null){
					find(node.get(i), nextScope);
				}
			}
		}
	}

	private JSFunction findFunction(ModifiableTree node, JSData scope){
		JSFunction newFunction = null;
		String name = "";
		String localName = "";
		ModifiableTree parentNode = node.getParent();
		if(parentNode.is(JSTag.TAG_ASSIGN) || parentNode.is(JSTag.TAG_VAR_DECL) || parentNode.is(JSTag.TAG_PROPERTY)){
			if(node.get(2).is(JSTag.TAG_NAME)){
				localName = node.get(2).getText();
			}
			if(parentNode.get(0).is(JSTag.TAG_NAME)){
				name = parentNode.get(0).getText();
			} else if(parentNode.get(0).is(JSTag.TAG_FIELD)){
				ArrayList<String> fieldelements = NodeUtil.getFieldElements(parentNode.get(0));
				scope = scope.searchAvailableFunc(fieldelements.get(1));
				name = fieldelements.get(0);
			}
		} else {
			if(node.get(2).is(JSTag.TAG_NAME)){
				name = node.get(2).getText();
			} else if(node.get(2).is(JSTag.TAG_FIELD)){
				ArrayList<String> fieldElements = NodeUtil.getFieldElements(node.get(2));
				scope = scope.searchAvailableFunc(fieldElements.get(1));
				name = fieldElements.get(0);
			} else
			// lambda function
			{
				name = scope.getFixedFullName() + "_FUN" + scope.issueLocalFuncIndentifier();
			}
		}
		newFunction = new JSFunction(name, scope, node);
		this.funs.add(newFunction);
		return newFunction;
	}

	private JSObject findObject(ModifiableTree node, JSData scope){
		JSObject newObject = null;
		String name = "";
		//TODO
		ModifiableTree parentNode = node.getParent();
		if(parentNode.is(JSTag.TAG_ASSIGN) || parentNode.is(JSTag.TAG_VAR_DECL)){
			if(parentNode.get(0).is(JSTag.TAG_NAME)){
				name = parentNode.get(0).getText();
			} else if(parentNode.get(0).is(JSTag.TAG_FIELD)){
				ArrayList<String> fieldelements = NodeUtil.getFieldElements(parentNode.get(0));
				scope = scope.searchAvailableFunc(fieldelements.get(1));
				name = fieldelements.get(0);
			}
		} else
		// no name object
		{
			name = scope.getFixedFullName() + "_OBJ" + scope.issueLocalObjIndentifier();
		}
		newObject = new JSObject(name, scope, node);
		this.objs.add(newObject);
		return newObject;
	}
	
	private JSVariable findVariable(ModifiableTree node, JSData scope) {
		JSVariable newVariable = null;
		String name = "";
		if(node.get(0).is(JSTag.TAG_NAME)){
			name = node.get(0).getText();
		} else if(node.get(0).is(JSTag.TAG_FIELD)){
			ArrayList<String> elements = NodeUtil.getFieldElements(node.get(0));
			name = elements.get(0);
		}
		newVariable = new JSVariable(name, scope, node);
		this.vars.add(newVariable);
		return newVariable;
	}
	
	private JSData findProperty(ModifiableTree node, JSData scope){
		JSData newProperty = null;
		//TODO
		String name = node.get(0).getText();
		ModifiableTree valueNode = node.get(1);
		if(valueNode.is(JSTag.TAG_FUNC_DECL)){
			newProperty = findFunction(valueNode, scope);
		} else if(valueNode.is(JSTag.TAG_OBJECT)){
			newProperty = findObject(valueNode, scope);
		} else {
			newProperty = findVariable(node, scope);
		}
		return newProperty;
	}

}
