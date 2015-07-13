package nez.checker.js;

import java.util.ArrayList;

import nez.ast.Source;
import nez.checker.ModifiableTree;
import nez.checker.SourceGenerator;

public class FSharpGenerator extends SourceGenerator {

	private static boolean UseExtend;
	
	private FSharpModifier modifier;
	/**
	 * List of scopes in JavaScript. Finally, we output information which this
	 * list has.
	 **/
	private ArrayList<FSharpScope> fsClasses;
	private int lambdaIdentifier = 0;

	public FSharpGenerator() {
		FSharpGenerator.UseExtend = false;
		fsClasses = new ArrayList<FSharpScope>();
		modifier = new FSharpModifier();
	}

	private FSharpScope addFunctionToList(ModifiableTree node,FSharpScope parentScope) {
		FSharpScope newScope = null; // newScope is the node which will be added to List

		// case: Define by the code, such as ( function $NAME (...){...}; )
		if (node.get(2).is(JSTag.TAG_NAME) && !node.getParent().is(JSTag.TAG_ASSIGN)) {
			newScope = new FSharpScope(node.get(2).getText(), node, parentScope);
		} else {
			ModifiableTree parent = node.getParent();
			// case: function is assigned to the Variable by Tag.VarDecl
			// case: function is assigned to the Property by Tag.Property
			// case: function is assigned to the Variable by Tag.Assign
			if (parent.is(JSTag.TAG_VAR_DECL) || parent.is(JSTag.TAG_PROPERTY) || parent.is(JSTag.TAG_ASSIGN)) {
				// case: function is lambda
				if (!node.get(2).is(JSTag.TAG_NAME)) {
					if(parent.get(0).is(JSTag.TAG_NAME)){
						newScope = new FSharpScope(parent.get(0).getText(), node, parentScope);
					} else if(parent.get(0).is(JSTag.TAG_FIELD)){
						ArrayList<String> fields = getFieldElements(parent.get(0));
						FSharpScope ownerClass = searchScopeByObjName(fields.get(1), parentScope);
						if(ownerClass != null){
							newScope = new FSharpScope(parent.get(0).get(1).getText(), node, ownerClass);
							parentScope = ownerClass;
						} else {
							newScope = new FSharpScope(parent.get(0).get(1).getText(), node, parentScope);
						}
					}
				}
				// case: function is not lambda. function is named local name.
				else {
					newScope = new FSharpScope(parent.get(0).getText(), node.get(2).getText(), node, parentScope);
				}
			}
			// case: function is lambda, and is not assigned
			else {
				newScope = new FSharpScope("lambda" + lambdaIdentifier++, node, parentScope);
			}
		}

		fsClasses.add(newScope);
		parentScope.add(newScope);
		parentScope.funcList.add(new FSharpFunc(newScope));
		return newScope;
	}

	private FSharpScope addObjectToList(ModifiableTree node,
			FSharpScope parentScope) {
		FSharpScope newScope = null;
		ModifiableTree parent = node.getParent();

		// case: object is not lambda.
		if (parent.is(JSTag.TAG_ASSIGN) || parent.is(JSTag.TAG_PROPERTY) || parent.is(JSTag.TAG_VAR_DECL)) {
			if(parent.get(0).is(JSTag.TAG_NAME)){
				newScope = new FSharpScope(parent.get(0).getText(), node, parentScope);
			}
		}
		// case: object is lambda. example) arguments etc...
		else {
			newScope = new FSharpScope("lambda" + lambdaIdentifier++, node, parentScope);
		}

		fsClasses.add(newScope);
		parentScope.add(newScope);
		parentScope.varList.add(new FSharpVar(newScope.name, newScope.getInnerPath(), node));
		return newScope;
	}

	/**
	 * Find recursively JavaScript Function and Object from ModifiableTree, and
	 * assign them to fsClasses
	 **/
	private void findScope(ModifiableTree node, FSharpScope currentScope) {
		FSharpScope nextScope = currentScope;
		if (node.is(JSTag.TAG_FUNC_DECL)) {
			nextScope = addFunctionToList(node, currentScope);
		} else if (node.is(JSTag.TAG_OBJECT)) {
			nextScope = addObjectToList(node, currentScope);
		}

		for (int child_i = 0; child_i < node.size(); child_i++) {
			findScope(node.get(child_i), nextScope);
		}
	}

	/**
	 * format ModifiedTrees, which is the flow of curerntScope operation in
	 * JavaScript program, for F# code generation
	 **/
	private void formatTree(FSharpScope currentScope) {
		ModifiableTree node = currentScope.node;
		if (node.size() > 0) {
			for (int i = 0; i < node.size(); i++) {
				formatTree(currentScope, node.get(i));
			}
		}
		// TODO
	}

	private void findUndefinedVar() {
		for(FSharpScope currentScope : this.fsClasses){
			findUndefinedVar(currentScope.node, currentScope);
		}
	}
	
	private void findUndefinedVar(ModifiableTree node, FSharpScope currentScope) {
		if (node.is(JSTag.TAG_ASSIGN) && !node.get(1).is(JSTag.TAG_FUNC_DECL) && !node.get(1).is(JSTag.TAG_OBJECT)) {
			ModifiableTree nameNode = node.get(0);
			if(nameNode.is(JSTag.TAG_NAME)){
				if(currentScope.getAvailableVar(nameNode.getText()) == null){
					currentScope.addUndefinedMember(node);
				}
			} else if(nameNode.is(JSTag.TAG_FIELD)){
				ArrayList<String> fields = getFieldElements(nameNode);
				FSharpScope ownerClass = searchScopeByObjName(fields.get(1), currentScope);
				if(ownerClass != null){
					ownerClass.addUndefinedMember(node);
				}
			}
		}
		ModifiableTree nextNode;
		if(node.size() > 0){
			for(int i = 0; i < node.size(); i++){
				nextNode = node.get(i);
				if(!nextNode.is(JSTag.TAG_FUNC_DECL) && !nextNode.is(JSTag.TAG_OBJECT)){
					findUndefinedVar(nextNode, currentScope);
				}
			}
		}
	}
	
	private void findNewStmt(){
		for(FSharpScope fs : this.fsClasses){
			findNewStmt(fs.node, fs);
		}
	}
	
	private void findNewStmt(ModifiableTree node, FSharpScope currentScope) {
		if(node.is(JSTag.TAG_NEW)){
			ModifiableTree classNode = node.get(0);
			if(classNode.is(JSTag.TAG_NAME)){
				FSharpScope fsClass = searchScopeByObjName(classNode.getText(), currentScope);
				FSharpVar fv = currentScope.getAvailableVar(classNode.getText());
				if(fv != null){
					fsClass.addInstance(fv);
				}
			} else if(classNode.is(JSTag.TAG_FIELD)){
				// can't add the instance to instanceList
			}
		}
		if(node.size() > 0 && !node.is(JSTag.TAG_FUNC_DECL) && !node.is(JSTag.TAG_OBJECT)){
			for(int i = 0; i < node.size(); i++){
				findNewStmt(node.get(i), currentScope);
			}
		}
	}
	
	private void formatField(ModifiableTree node, FSharpScope currentScope) {
		ModifiableTree fieldNode = node;
		FSharpScope classScope = null;
		ArrayList<String> fieldElements = new ArrayList<String>();
		int elementNumFromRight = 0;
		while ( ( fieldNode.is(JSTag.TAG_FIELD) || fieldNode.is(JSTag.TAG_APPLY) )
				&& classScope == null ) {
			if (fieldNode.is(JSTag.TAG_FIELD)) {
				elementNumFromRight++;
				fieldElements.add(fieldNode.get(1).getText());
				classScope = searchScopeByFuncOrVarName(fieldNode.get(1).getText(), currentScope);
			}
			fieldNode = fieldNode.get(0);
		}
		if (classScope != null && elementNumFromRight < 2) {
			Source src = node.getSource();
			long spos = node.getSourcePosition();
			int len = node.getLength();
			ModifiableTree fixedNode = new ModifiableTree(JSTag.TAG_FIELD, src, spos, spos, 2, "");
			ModifiableTree newNode = new ModifiableTree(JSTag.TAG_NEW, src, spos, spos, 2, "");
			ModifiableTree constructorNode = new ModifiableTree(JSTag.TAG_NAME, src, spos, spos + len, 0, classScope.getScopeName());
			ModifiableTree argsNode = new ModifiableTree(JSTag.TAG_LIST, src, spos, spos, 0, null);
			//ArrayList<String> fieldStrings = getFieldElements(node);
			ModifiableTree callNode = new ModifiableTree(JSTag.TAG_NAME, src, spos, spos + len, 0, fieldElements.get(0));
			fixedNode.set(0, newNode);
			fixedNode.set(1, callNode);
			newNode.set(0, constructorNode);
			newNode.set(1, argsNode);
			node.getParent().set(node.getIndexInParentNode(), fixedNode);
		} else if (classScope != null && elementNumFromRight >= 2) {
			for (int i = fieldElements.size() - elementNumFromRight - 1; i < fieldElements.size(); i++) {
				if (searchScopeByFuncOrVarName(fieldElements.get(i), currentScope) == null) {
					Source src = node.getSource();
					long spos = node.getSourcePosition();
					int len = node.getLength();
					ModifiableTree fixedNode = new ModifiableTree(JSTag.TAG_APPLY, src, spos, spos + len, 2, "");
					ModifiableTree funcNode = new ModifiableTree(JSTag.TAG_NAME, src, spos, spos + len, 0, fieldElements.get(i));

				}
			}
		}
	}

	private FSharpScope searchScopeByFuncOrVarName(String name, FSharpScope currentScope) {
		FSharpScope result = null;
		// search variable and function from current scope
		for (FSharpFunc ff : currentScope.funcList) {
			if (ff.getTrueName() == name) {
				result = currentScope;
			}
		}
		for (FSharpVar fv : currentScope.varList) {
			if (fv.getTrueName() == name) {
				result = currentScope;
			}
		}
		// search it from parent scope
		if (result == null && currentScope.parent != null) {
			result = searchScopeByFuncOrVarName(name, currentScope.parent);
		}
		return result;
	}
	
	private FSharpScope searchScopeByObjName(String name, FSharpScope currentScope) {
		FSharpScope result = null;
		// search variable and function from current scope
		for (FSharpVar instVar : currentScope.instanceList) {
			if(instVar.getTrueName().contentEquals(name)){
				result = currentScope;
			}
		}
		for(FSharpScope fs : currentScope.children){
			for (FSharpVar instVar : fs.instanceList) {
				if(instVar.getTrueName().contentEquals(name)){
					result = fs;
				}
			}
		}
		// search it from parent scope
		if (result == null && currentScope.parent != null) {
			result = searchScopeByFuncOrVarName(name, currentScope.parent);
		}
		return result;
	}

	private ArrayList<String> getFieldElements(ModifiableTree node) {
		ModifiableTree fieldNode = node;
		ArrayList<String> fieldElements = new ArrayList<String>();
		while (fieldNode.is(JSTag.TAG_FIELD)) {
			fieldElements.add(fieldNode.getText());
			fieldNode = node.get(0);
		}
		fieldElements.add(fieldNode.getText());
		return fieldElements;
	};

	private boolean formatTree(FSharpScope currentScope, ModifiableTree node) {
		if (node.is(JSTag.TAG_FIELD)) {
			formatField(node, currentScope);
		}
		if (node.size() > 0) {
			for (int i = 0; i < node.size(); i++) {
				formatTree(currentScope, node.get(i));
			}
		}
		return true;
	}

	private void generateFSCode(FSharpScope currentScope) {
		// TODO
	}

	private void generatePrintCode() {
		// TODO
	}

	public void toSource(ModifiableTree node) {
		FSharpScope topScope = new FSharpScope("TOPLEVEL", node, null);
		fsClasses.add(topScope);
		findScope(node, topScope);
		findUndefinedVar();
		findNewStmt();
		// print debug
		for (FSharpScope fs : fsClasses) {
			System.out.println(fs.toString());
		}
		//
		for (FSharpScope fsClass : fsClasses) {
			formatTree(fsClass);
		}
		for (FSharpScope fsClass : fsClasses) {
			generateFSCode(fsClass);
		}
		generatePrintCode();
	}

}
