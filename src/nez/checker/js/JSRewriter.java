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
		this.funs.add(top);
		this.find(po, top);
		System.out.println(funs.toString());
		FixedJSGenerator generator = new FixedJSGenerator(this);
		generator.visit(po);
		return generator.toString();
	}
	
	public ArrayList<JSFunction> getFunctionList(){
		return this.funs;
	}
	public ArrayList<JSObject> getObjectList(){
		return this.objs;
	}
	public ArrayList<JSVariable> getVariableList(){
		return this.vars;
	}
	
	private void find(ModifiableTree node, JSData scope){
		JSData nextScope = scope;
		Boolean continueFind = true;
		if(node.is(JSTag.TAG_FUNC_DECL) && !node.getParent().is(JSTag.TAG_PROPERTY)){
			nextScope = findFunction(node, nextScope);
		} else if(node.is(JSTag.TAG_OBJECT) && !node.getParent().is(JSTag.TAG_PROPERTY)){
			nextScope = findObject(node, nextScope);
		} else if(node.is(JSTag.TAG_PROPERTY)){
			JSData prop = findProperty(node, nextScope);
			if(prop.getClass() == JSFunction.class || prop.getClass() == JSObject.class){
				nextScope = prop;
			}
		} else if(node.is(JSTag.TAG_VAR_DECL) && !node.get(1).is(JSTag.TAG_FUNC_DECL) && !node.get(1).is(JSTag.TAG_OBJECT)){
			findVariable(node, nextScope);
		} else if(node.is(JSTag.TAG_NEW)){
			JSObject newObj = findNewObject(node, nextScope);
			this.objs.add(newObj);
		}
		if(node.size() > 0 && !isAssignToThisProperty(node) && continueFind){
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
			newProperty = new JSFunction(name, scope, valueNode);
			this.funs.add((JSFunction)newProperty);
		} else if(valueNode.is(JSTag.TAG_OBJECT)){
			newProperty = new JSObject(name, scope, node);
			this.objs.add((JSObject)newProperty);
		} else {
			newProperty = new JSVariable(name, scope, valueNode);
			this.vars.add((JSVariable)newProperty);
		}
		return newProperty;
	}
	
	private JSObject findNewObject(ModifiableTree node, JSData scope){
		String name = "";
		ModifiableTree parentNode = node.getParent();
		if(parentNode.is(JSTag.TAG_ASSIGN) || parentNode.is(JSTag.TAG_VAR_DECL)){
			if(node.get(0).is(JSTag.TAG_NAME)){
				name = node.get(0).getText();
			} else if(node.get(0).is(JSTag.TAG_FIELD)){
				ArrayList<String> elements = NodeUtil.getFieldElements(node.get(0));
				name = elements.get(0);
			}
			JSFunction constructor = scope.searchAvailableFunc(name);
			return analyzeFunctionAsConstructor(constructor);
		}
		return null;
	}
	
	private JSObject analyzeFunctionAsConstructor(JSFunction function){
		JSObject obj = new JSObject(function.name + "_new", function.parent, function.node);
		findAssignToThisProperty(function.node.get(6), obj);
		//TODO
		return obj;
	}
	
	private void findAssignToThisProperty(ModifiableTree node, JSObject scope){
		if(node.is(JSTag.TAG_ASSIGN)){
			if(node.get(0).is(JSTag.TAG_FIELD)){
				if(node.get(0).get(0).is(JSTag.TAG_THIS)){
					JSData newProperty = null;
					String name = node.get(0).get(1).getText();
					ModifiableTree valueNode = node.get(1);
					if(valueNode.is(JSTag.TAG_FUNC_DECL)){
						newProperty = new JSFunction(name, scope, node.get(1));
						scope.add(newProperty);
						find(node.get(6), newProperty);
						this.funs.add((JSFunction)newProperty);
					} else if(valueNode.is(JSTag.TAG_OBJECT)){
						newProperty = findObject(valueNode, scope);
					} else {
						newProperty = findVariable(node, scope);
					}
				}
			}
		}
		if(node.size() > 0 && !node.is(JSTag.TAG_OBJECT) && !node.is(JSTag.TAG_FUNC_DECL)){
			for(int i = 0; i < node.size(); i++){
				findAssignToThisProperty(node.get(i), scope);
			}
		}
	}
	
	private boolean isAssignToThisProperty(ModifiableTree assignNode){
		if(assignNode.is(JSTag.TAG_ASSIGN)){
			if(assignNode.get(0).is(JSTag.TAG_FIELD)){
				if(assignNode.get(0).get(0).is(JSTag.TAG_THIS)){
					return true;
				}
			}
		}
		return false;
	}

}
