package nez.fsharp;

import java.util.ArrayList;
import java.util.List;

import nez.ast.CommonTree;
import nez.ast.Tag;


public class FSharpGenerator extends SourceGenerator {
	private static boolean UseExtend;
	public ArrayList<FSharpVar> varList = new ArrayList<FSharpVar>();
	public ArrayList<FSharpScope> scopeList = new ArrayList<FSharpScope>();
	private String prefixName = "";
	private int ifResultKey = 0;
	private int lambdaKey = 0;
	private boolean forFlag = false;
	private boolean objFlag = false;
	private boolean assignFlag= false;
	private boolean letFlag = false;
	private ArrayList<String> addedGetterList = new ArrayList<String>();
	private String forConunter = "";
	private FSharpScope currentScope;
	
	private final Tag TAG_LAMBDA_NAME = Tag.tag("LambdaName");

	public FSharpGenerator() {
		FSharpGenerator.UseExtend = false;
	}
	
	protected void initialSetting(CommonTree node){
		CommonTree target;
		FSharpScope topScope = new FSharpScope("TOPLEVEL", node, new ArrayList<String>());
		this.scopeList.add(topScope);
		for(int i = 0; i < node.size(); i++){
			target = node.get(i);
			if(target.is(JSTag.TAG_VAR_DECL_STMT)){
				
			}
			this.findScope(target, new ArrayList<String>(), topScope);
		}
		for(FSharpScope fs : this.scopeList){
			this.checkPrototype(node, fs);
		}
	}
	
	protected void findScope(CommonTree node, List<String> path, FSharpScope parentScope){
		boolean addScope = false;
		String scopeName = "";
		ArrayList<String> nextPath = (ArrayList<String>)path;
		boolean isFuncDecl = node.is(JSTag.TAG_FUNC_DECL);
		boolean isObject = node.is(JSTag.TAG_OBJECT);
		FSharpScope fs = parentScope;
		List<String> prototypePath = null;
		
		if(isFuncDecl){
			addScope = true;
			scopeName = node.get(2).getText();
			if(scopeName.isEmpty()){
				//sNode = superNode
				CommonTree sNode = node.getParent();
				CommonTree ssNode = sNode.getParent();
				CommonTree sssNode = ssNode.getParent();
				if(sNode.is(JSTag.TAG_ASSIGN)){
					scopeName = sNode.get(0).get(1).getText();
					prototypePath = new ArrayList<String>();
					for(String pathElement : this.getFullFieldText(sNode.get(0), fs).split("_")){
						prototypePath.add(pathElement);
					}
					FSharpScope prototypeScope = this.searchScopeFromList(prototypePath.get(0));
					nextPath = new ArrayList<String>();
					if(prototypeScope != null){
						nextPath.addAll(prototypeScope.path);
					}
					for(int i = 0; i < prototypePath.size() - 1; i++){
						nextPath.add(prototypePath.get(i));
					}
				} else if(sNode.is(JSTag.TAG_APPLY)){
					scopeName = ssNode.get(0).getText();
				} else if(sssNode.is(JSTag.TAG_VAR_DECL_STMT)){
					scopeName = sNode.get(0).getText();
				} else if(sNode.is(JSTag.TAG_PROPERTY)) {
					scopeName = sNode.get(0).getText();
				} else {
					scopeName = "lambda" + this.lambdaKey++;
					CommonTree nameNode = new CommonTree(Tag.tag("Name"), null, 0, 0, 0, scopeName);
					CommonTree parent = node.getParent();
//					int i = 0;
//					for(String pathElement : path){
//						nameNode.add(new CommonTree(new Tag("Name"), null, 0));
//						nameNode.get(i).setValue(pathElement);
//						i++;
//					}
//					nameNode.add(new CommonTree(new Tag("Name"), null, 0));
					parent.set(this.indexOf(node), nameNode);
				}
				CommonTree scopeNameNode = node.get(2);
				node.set(2, new CommonTree(Tag.tag("Name"), null, 0, 0, 0, scopeName));
			} else if(node.get(2).is(this.TAG_LAMBDA_NAME)){
				//node.get(2).setTag(new Tag("Name"));
				CommonTree parent = node.getParent();
				String fullName = "";
				String lambdaName = node.get(2).getText();
				for(String pathElement : path){
					fullName += pathElement + "_";
				}
				fullName += lambdaName;
				CommonTree nameNode = new CommonTree(Tag.tag("Name"), null, 0, 0, 0, "(new ScopeOf" + fullName + "())." + lambdaName);
				parent.set(this.indexOf(node), nameNode);
			}
		} else if(isObject){
			addScope = true;
			CommonTree sNode = node.getParent();
			if(sNode.is(JSTag.TAG_ASSIGN) || sNode.is(JSTag.TAG_VAR_DECL)){
				scopeName = sNode.get(0).getText();
			} else {
				scopeName = "lambda" + this.lambdaKey++;
			}
		}
		if(addScope){
			if(prototypePath == null){
				fs = new FSharpScope(scopeName, node, (ArrayList<String>)path);
			} else {
				fs = new FSharpScope(scopeName, node, nextPath);
			}
			if(parentScope != null){
				fs.parent = parentScope;
				parentScope.add(fs);
			}
			this.scopeList.add(fs);
			
			if(isFuncDecl){
				CommonTree argsNode = node.get(4);
				FSharpVar argVar;
				for(int i = 0; i < argsNode.size(); i++){
					argVar = new FSharpVar(argsNode.get(i).getText(), fs.getPathName());
					fs.varList.add(argVar);
					this.varList.add(argVar);
					fs.numOfArgs++;
				}
				this.findReturn(node.get(6), fs, false);
			} else if(isObject){
				CommonTree objNode = new CommonTree(Tag.tag("ObjectName"), null, 0, 0, 0, "new " + fs.getScopeName()+"()");
				node.getParent().set(this.indexOf(node), objNode);
			}
			
			if(prototypePath == null){
				nextPath = new ArrayList<String>();
				//deep copy path -> cpath
				for(String pathElement : path){
					nextPath.add(pathElement);
				}
				nextPath.add(scopeName);
			}
			if(isFuncDecl){
				this.checkVarDecl(node.get(6), fs);
			} else if(isObject){
				this.checkProperty(node, fs);
			}
		}
		if(node.size() > 0){
			for(int i = 0; i < node.size(); i++){
				this.findScope(node.get(i), nextPath, fs);
			}
		}
	}
	
	private boolean isNullOrEmpty(CommonTree node){
		return node == null || node.is(Tag.tag("TAG_TEXT")) && node.size() == 0 && node.size() == 0 && node.getText().length() == 0;
	}
	private boolean isNullOrEmpty(CommonTree node, int index){
		return node.size() <= index || isNullOrEmpty(node.get(index));
	}
	
	private CommonTree lastNodeOfField(CommonTree node){
		if(node.getParent().is(JSTag.TAG_FIELD)){
			return this.lastNodeOfField(node.getParent());
		} else if(node.is(JSTag.TAG_FIELD)){
			return node.get(1);
		}
		return null;
	}
	
	private void checkPrototype(CommonTree node, FSharpScope fs){
		CommonTree parent = node.getParent();
		if(node.is(JSTag.TAG_NAME) && parent.is(JSTag.TAG_FIELD) && node.getText().contentEquals("prototype")){
			int node_i = this.indexOf(node);
			if(0 < node_i){
				if(parent.get(node_i - 1).getText().contentEquals(fs.name)){
					CommonTree pNameNode = this.lastNodeOfField(node);
					String prototypeName = pNameNode.getText();
					CommonTree valueNode = pNameNode.getParent().getParent().get(1);
					if(valueNode.is(JSTag.TAG_FUNC_DECL)){
						valueNode.get(2).setValue(prototypeName);
					}
					fs.funcList.add(new FSharpFunc(prototypeName, fs.getPathName(), false, valueNode));
				}
			}
		}
		for(int i = 0; i < node.size(); i++){
			checkPrototype(node.get(i), fs);
		}
	}
	
	public void toObjectName(CommonTree node){
		this.currentBuilder.append(node.getText());
	}
	
	private int indexOf(CommonTree node){
		CommonTree parent = node.getParent();
		for(int i = 0; i < parent.size(); i++){
			if(parent.get(i) == node){
				return i;
			}
		}
		return -1;
	}
	
	protected void checkProperty(CommonTree node, FSharpScope fs){
		CommonTree propertyNode;
		FSharpVar fv;
		FSharpFunc ff;
		for(int i = 0; i < node.size(); i++){
			propertyNode = node.get(i);
			if(!propertyNode.get(1).is(JSTag.TAG_FUNC_DECL)){
				fv = new FSharpVar(propertyNode.get(0).getText(), fs.getPathName(), propertyNode.get(1));
				fs.varList.add(fv);
				this.varList.add(fv);
			} else {
				ff = new FSharpFunc(propertyNode.get(0).getText(), fs.getPathName(), false, propertyNode.get(1));
				fs.funcList.add(ff);
			}
		}
	}
	
	protected boolean checkVarDecl(CommonTree node, FSharpScope fs){
		if(node.is(JSTag.TAG_VAR_DECL_STMT)){
			CommonTree listNode = node.get(2);
			CommonTree varDeclNode;
			for(int i = 0; i < listNode.size(); i++){
				varDeclNode = listNode.get(i);
				try{
					if(!varDeclNode.get(1).is(JSTag.TAG_FUNC_DECL)){
						FSharpVar fv = new FSharpVar(varDeclNode.get(0).getText(), fs.getPathName());
						this.varList.add(fv);
						fs.varList.add(fv);
						varDeclNode.setTag(Tag.tag("Assign"));
						node.getParent().insert(this.indexOf(node) + i, varDeclNode);
					} else {
						FSharpFunc ff = new FSharpFunc(varDeclNode.get(0).getText(), fs.getPathName(), false, varDeclNode.get(1));
						fs.funcList.add(ff);
						return false;
					}
				} catch(ArrayIndexOutOfBoundsException e){
					return false;
				}
			}
			node.getParent().remove(this.indexOf(node));
		} else
		if(node.is(JSTag.TAG_FUNC_DECL)){
			CommonTree nameNode = node.get(2);
			if(nameNode.getText().isEmpty()){
				nameNode.setValue("lambda" + this.lambdaKey++);
				nameNode.setTag(Tag.tag("LambdaName"));
			}
			FSharpFunc ff = new FSharpFunc(nameNode.getText(), fs.getPathName(), false, node);
			fs.funcList.add(ff);
			return false;
		} else
		if(node.is(JSTag.TAG_ASSIGN)){
			if(node.get(0).is(JSTag.TAG_NAME)){
				String varName = this.getFieldText(node.get(0));
				boolean isExist = false;
				for(FSharpVar searchTarget : fs.varList){
					if(searchTarget.name.contentEquals(varName)){
						isExist = true;
					}
				}
				if(!isExist){
					FSharpVar fv = new FSharpVar(varName, fs.getPathName());
					this.varList.add(fv);
					fs.varList.add(fv);
				}
			}
		}
		if(node.size() > 0){
			for(int i = 0; i < node.size(); i++){
				checkVarDecl(node.get(i), fs);
			}
		}
		return true;
	}
	
	protected String typeCode(FSharpScope fs){
		String name = fs.getScopeName();
		String pathString = "";
		for(String pathElement : fs.path){
			pathString += this.currentBuilder.quoteString + pathElement + this.currentBuilder.quoteString + ";";
		}
		this.currentBuilder.appendNewLine("let " + name + "0 = " + "new " + name + "()");
		String printStr = "fsLib.fl.printObject " + this.currentBuilder.quoteString + name + this.currentBuilder.quoteString + ((fs.type==fs.type.OBJECT)? " true":" false") + " [|"+ pathString + "|] (" + name + "0.GetType().GetMethods())";
		return printStr;
	}
	
	protected String typeCode(FSharpFunc ffunc){
		String printStr = "printfn " + this.currentBuilder.quoteString + "%s" + this.currentBuilder.quoteString;
		String argBeginStr = " (" + this.currentBuilder.quoteString + ffunc.getFullname() + " : " + this.currentBuilder.quoteString + " + ";
		String argStr = "";
		if(ffunc.isMember){
			argStr = ffunc.getFullname() + ".GetType().ToString()";
		} else {
			argStr = ffunc.getFullname() + ".GetType().GetMethods().[0].ToString()";
		}
		String argEndStr = ")";
		return printStr + argBeginStr + argStr + argEndStr;
	}
	
	protected String typeCode(FSharpVar fvar){
		String printStr = "printfn " + this.currentBuilder.quoteString + "%s" + this.currentBuilder.quoteString;
		String argBeginStr = " (" + this.currentBuilder.quoteString + fvar.getTrueName() + " : " + this.currentBuilder.quoteString;
		String argStr = "";
		for(int i = 0; i <= fvar.uniqueKey; i++){
			argStr += " + " + fvar.getFullname() + i + ".GetType().ToString()";
		}
		String argEndStr = ")";
		return printStr + argBeginStr + argStr + argEndStr;
	}
	
	protected void generateTypeCode(){
		for(FSharpScope fs : this.scopeList){
			this.currentBuilder.appendNewLine(typeCode(fs));
		}
	}
	
	protected boolean checkReturn(CommonTree node, boolean result){
		if(result){
			return true;
		}
		result = node.is(JSTag.TAG_RETURN);
		if(node.size() >= 1 && !result) {
			for(int i = 0; i < node.size(); i++){
				result = checkReturn(node.get(i), result);
			}
		}
		return result;
	}
	
	protected boolean findReturn(CommonTree node, FSharpScope fs, boolean result){
		boolean res = false;
		if(result){
			res = true;
		}
		if(node.is(JSTag.TAG_RETURN)){
			res = true;
			fs.returnList.add(node);
		}
		if(node.size() >= 1 && !node.is(JSTag.TAG_FUNC_DECL)) {
			for(int i = 0; i < node.size(); i++){
				res = findReturn(node.get(i), fs, res);
			}
		}
		return res;
	}
	
	protected void checkAssignVarName(CommonTree node, FSharpVar targetVar){
		if(node.size() < 1){
			if(targetVar.getTrueName().contentEquals(node.getText())){
				node.setValue(targetVar.getCurrentName());
			}
		} else {
			for(int i = 0; i < node.size(); i++){
				if(node.get(i).size() == 0){
					if(targetVar.getTrueName().contentEquals(node.get(i).getText())){
						node.get(i).setValue(targetVar.getCurrentName());
					}
				} else {
					checkAssignVarName(node.get(i), targetVar);
				}
			}
		}
	}
	
	protected boolean checkApplyFuncName(String funcName){
		for(FSharpScope target : this.scopeList){
			if(target.getPathName().contentEquals(funcName + ".")){
				return true;
			}
			if(funcName.startsWith("_") && target.getPathName().contentEquals(funcName.substring(1) + ".")){
				return true;
			}
			if(target.getPathName().contentEquals(this.prefixName + funcName + ".")){
				return true;
			}
			if(funcName.startsWith("_") && target.getPathName().contentEquals(this.prefixName + funcName.substring(1) + ".")){
				return true;
			}
		}
		return false;
	}
	
	protected FSharpVar searchVarFromList(String varName, boolean fieldFlag){
		String prefixName = this.prefixName;
		String[] prefixNameElements = prefixName.split(".");
		if(prefixNameElements.length == 0){
			int num = prefixName.indexOf(".");			
			if(num > 0 && prefixName.length() - 1 > num){
				prefixNameElements = new String[2];
				prefixNameElements[0] = prefixName.substring(0, num);
				prefixNameElements[1] = prefixName.substring(num + 1, prefixName.length() - 1);
			} else {
				prefixNameElements = new String[1];
				prefixNameElements[0] = prefixName.substring(0, prefixName.length() - 1);
			}
		}
		for(FSharpVar element : varList){
			if(element.getFullname().contentEquals(prefixName + varName)){
				return element;
			} else if(element.getFullname().contentEquals(varName) && fieldFlag){
				return element;
			}
		}
		if(prefixNameElements != null){
			for(int i = prefixNameElements.length - 1; i >= 0; i--){
				if(prefixName.length() > 0){
					prefixName = prefixName.substring(0, prefixName.length() - (prefixNameElements[i].length() + 1));
				}
				for(FSharpVar element : varList){
					if(element.getFullname().contentEquals(prefixName + varName)){
						return element;
					} else if(element.getFullname().contentEquals(varName) && fieldFlag){
						return element;
					}
				}
			}
		}
		return null;
	}
	
	private String getFullFieldText(CommonTree node, FSharpScope fs){
		if(node.is(JSTag.TAG_FIELD)){
			if(node.get(1).getText().contentEquals("prototype")){
				return this.getFullFieldText(node.get(0), fs);
			} else {
				return this.getFullFieldText(node.get(0), fs) + "_" + node.get(1).getText();
			}
		}
		return node.getText();
	}
	
	protected String getFieldText(CommonTree node){
		String result = "";
		if(node.is(JSTag.TAG_FIELD)){
			for(int i = 0; i < node.size(); i++){
				result += node.get(i).getText();
				if(i < node.size() - 1){
					result += ".";
				}
			}
		} else if(node.is(JSTag.TAG_NAME)){
			result += node.getText();
		}
		return result;
	}
	
	protected void setVarNameInBinary(CommonTree node, boolean isAssign){
		String varName = this.getFieldText(node.get(0));
		FSharpVar targetVar = searchVarFromList(varName, node.get(0).is(JSTag.TAG_FIELD));
		if(targetVar == null && isAssign){
			this.varList.add(new FSharpVar(varName, this.prefixName));
			targetVar = this.varList.get(this.varList.size()-1);
			checkAssignVarName(node.get(1), targetVar);
			targetVar.addChild();
		}
		varName = targetVar.getCurrentName();
		node.get(0).setValue(varName);
	}

/*
	public void toThrow(CommonTree node) {
		this.currentBuilder.append("throw ");
		this.to(node.get(0));
	}
	*/
	
	protected int getOperatorPrecedence(Tag tagId){
		if(tagId == JSTag.TAG_INTEGER) return 0;
		if(tagId == JSTag.TAG_BINARY_INTEGER) return 0;
		if(tagId == JSTag.TAG_OCTAL_INTEGER) return 0;
		if(tagId == JSTag.TAG_HEX_INTEGER) return 0;
		if(tagId == JSTag.TAG_LONG) return 0;
		if(tagId == JSTag.TAG_BINARY_LONG) return 0;
		if(tagId == JSTag.TAG_OCTAL_LONG) return 0;
		if(tagId == JSTag.TAG_HEX_LONG) return 0;
		if(tagId == JSTag.TAG_FLOAT) return 0;
		if(tagId == JSTag.TAG_HEX_FLOAT) return 0;
		if(tagId == JSTag.TAG_DOUBLE) return 0;
		if(tagId == JSTag.TAG_HEX_DOUBLE) return 0;
		if(tagId == JSTag.TAG_STRING) return 0;
		if(tagId == JSTag.TAG_REGULAR_EXP) return 0;
		if(tagId == JSTag.TAG_NULL) return 0;
		if(tagId == JSTag.TAG_TRUE) return 0;
		if(tagId == JSTag.TAG_FALSE) return 0;
		if(tagId == JSTag.TAG_THIS) return 0;
		if(tagId == JSTag.TAG_SUPER) return 0;
		if(tagId == JSTag.TAG_NAME) return 0;
		if(tagId == JSTag.TAG_ARRAY) return 0;
		if(tagId == JSTag.TAG_HASH) return 0;
		if(tagId == JSTag.TAG_TYPE) return 0;
		if(tagId == JSTag.TAG_SUFFIX_INC) return 2;
		if(tagId == JSTag.TAG_SUFFIX_DEC) return 2;
		if(tagId == JSTag.TAG_PREFIX_INC) return 2;
		if(tagId == JSTag.TAG_PREFIX_DEC) return 2;
		if(tagId == JSTag.TAG_PLUS) return 4;
		if(tagId == JSTag.TAG_MINUS) return 4;
		if(tagId == JSTag.TAG_COMPL) return 4;
		if(tagId == JSTag.TAG_ADD) return 6;
		if(tagId == JSTag.TAG_SUB) return 6;
		if(tagId == JSTag.TAG_MUL) return 5;
		if(tagId == JSTag.TAG_DIV) return 5;
		if(tagId == JSTag.TAG_MOD) return 5;
		if(tagId == JSTag.TAG_LEFT_SHIFT) return 7;
		if(tagId == JSTag.TAG_RIGHT_SHIFT) return 7;
		if(tagId == JSTag.TAG_LOGICAL_LEFT_SHIFT) return 7;
		if(tagId == JSTag.TAG_LOGICAL_RIGHT_SHIFT) return 7;
		if(tagId == JSTag.TAG_GREATER_THAN) return 8;
		if(tagId == JSTag.TAG_GREATER_THAN_EQUALS) return 8;
		if(tagId == JSTag.TAG_LESS_THAN) return 8;
		if(tagId == JSTag.TAG_LESS_THAN_EQUALS) return 8;
		if(tagId == JSTag.TAG_EQUALS) return 9;
		if(tagId == JSTag.TAG_NOT_EQUALS) return 9;
		if(tagId == JSTag.TAG_STRICT_EQUALS) return 9;
		if(tagId == JSTag.TAG_STRICT_NOT_EQUALS) return 9;
		//if(tagId == JSTag.TAG_COMPARE) return 9;
		//if(tagId == JSTag.TAG_INSTANCE_OF) return 8;
		if(tagId == JSTag.TAG_STRING_INSTANCE_OF) return 8;
		if(tagId == JSTag.TAG_HASH_IN) return 8;
		if(tagId == JSTag.TAG_BITWISE_AND) return 10;
		if(tagId == JSTag.TAG_BITWISE_OR) return 12;
		if(tagId == JSTag.TAG_BITWISE_NOT) return 4;
		if(tagId == JSTag.TAG_BITWISE_XOR) return 11;
		if(tagId == JSTag.TAG_LOGICAL_AND) return 13;
		if(tagId == JSTag.TAG_LOGICAL_OR) return 14;
		if(tagId == JSTag.TAG_LOGICAL_NOT) return 4;
		//if(tagId == JSTag.TAG_LOGICAL_XOR) return 14;
		if(tagId == JSTag.TAG_CONDITIONAL) return 16;
		if(tagId == JSTag.TAG_ASSIGN) return 17;
		if(tagId == JSTag.TAG_ASSIGN_ADD) return 17;
		if(tagId == JSTag.TAG_ASSIGN_SUB) return 17;
		if(tagId == JSTag.TAG_ASSIGN_MUL) return 17;
		if(tagId == JSTag.TAG_ASSIGN_DIV) return 17;
		if(tagId == JSTag.TAG_ASSIGN_MOD) return 17;
		if(tagId == JSTag.TAG_ASSIGN_LEFT_SHIFT) return 17;
		if(tagId == JSTag.TAG_ASSIGN_RIGHT_SHIFT) return 17;
		if(tagId == JSTag.TAG_ASSIGN_LOGICAL_LEFT_SHIFT) return 17;
		if(tagId == JSTag.TAG_ASSIGN_LOGICAL_RIGHT_SHIFT) return 17;
		if(tagId == JSTag.TAG_ASSIGN_BITWISE_AND) return 17;
		if(tagId == JSTag.TAG_ASSIGN_BITWISE_OR) return 17;
		if(tagId == JSTag.TAG_ASSIGN_BITWISE_XOR) return 17;
		//if(tagId == JSTag.TAG_ASSIGN_LOGICAL_AND) return 0;
		//if(tagId == JSTag.TAG_ASSIGN_LOGICAL_OR) return 0;
		//if(tagId == JSTag.TAG_ASSIGN_LOGICAL_XOR) return 0;
		//if(tagId == JSTag.TAG_MULTIPLE_ASSIGN) return 0;
		if(tagId == JSTag.TAG_COMMA) return 18;
		//if(tagId == JSTag.TAG_CONCAT) return 4;
		if(tagId == JSTag.TAG_FIELD) return 1;
		if(tagId == JSTag.TAG_INDEX) return 1;
		if(tagId == JSTag.TAG_MULTI_INDEX) return 1;
		if(tagId == JSTag.TAG_APPLY) return 2;
		if(tagId == JSTag.TAG_METHOD) return 2;
		if(tagId == JSTag.TAG_TYPE_OF) return 2;
		if(tagId == JSTag.TAG_NEW) return 1;
		return Integer.MAX_VALUE;
	}
	
	protected boolean shouldExpressionBeWrapped(CommonTree node){
		int precedence = getOperatorPrecedence(node.getTag());
		if(precedence == 0){
			return false;
		}else{
			CommonTree parent = node.getParent();
			if(parent != null && getOperatorPrecedence(parent.getTag()) >= precedence){
				return false;
			}
		} 
		return true;
	}
	
	protected void generateExpression(CommonTree node){
		if(this.shouldExpressionBeWrapped(node)){
			this.visit(node, '(', ')');
		}else{
			this.visit(node);
		}
	}
	
	protected void genaratePrefixUnary(CommonTree node, String operator){
		this.currentBuilder.append(operator);
		generateExpression(node.get(0));
	}

	protected void genarateSuffixUnary(CommonTree node, String operator){
		generateExpression(node.get(0));
		this.currentBuilder.append(operator);
	}

	protected void generateBinary(CommonTree node, String operator){
		if(!this.assignFlag){
			this.formatRightSide(node.get(0));
			this.formatRightSide(node.get(1));
		}
		generateExpression(node.get(0));
		this.currentBuilder.append(operator);
		generateExpression(node.get(1));
	}
	
	protected void generateTrinary(CommonTree node, String operator1, String operator2){
		generateExpression(node.get(0));
		this.currentBuilder.append(operator1);
		generateExpression(node.get(1));
		this.currentBuilder.append(operator2);
		generateExpression(node.get(2));
	}
	
	protected void generateTrinaryAddHead(CommonTree node, String operator1, String operator2, String operator3){
		this.currentBuilder.append(operator1);
		generateExpression(node.get(0));
		this.currentBuilder.append(operator2);
		generateExpression(node.get(1));
		this.currentBuilder.append(operator3);
		generateExpression(node.get(2));
	}
	
	protected void generateList(List<CommonTree> node, String delim){
		boolean isFirst = true;
		for(CommonTree element : node){
			if(!isFirst){
				this.currentBuilder.append(delim);
			}else{
				isFirst = false;
			}
			this.visit(element);
		}
	}
	
	protected void generateList(List<CommonTree> node, String begin, String delim, String end){
		this.currentBuilder.append(begin);
		this.generateList(node, delim);
		this.currentBuilder.append(end);
	}
	
	protected void generateClass(CommonTree node){
		String name = node.get(0).getText();
		this.objFlag = true;
		this.currentBuilder.appendNewLine("type ClassOf" + name + "(arg_for_object:int) = class");
		this.currentBuilder.indent();
		CommonTree objNode = node.get(1);
		CommonTree varDeclStmtNode = new CommonTree(Tag.tag("VarDeclStmt"), null, 0, 0, 0, null);
		varDeclStmtNode.set(0, new CommonTree(Tag.tag("Text"), null, 0, 0, 0, null));
		varDeclStmtNode.set(1, new CommonTree(Tag.tag("Text"), null, 0, 0, 0, null));
		varDeclStmtNode.set(2, new CommonTree(Tag.tag("List"), null, 0, 0, 0, null));
		this.prefixName += name + ".";
		String varName = "";
		for(int i = 0; i < objNode.size(); i++){
			this.currentBuilder.appendNewLine();
			varName = objNode.get(i).get(0).getText();
			objNode.get(i).setTag(Tag.tag("VarDecl"));
			objNode.get(i).get(0).setValue(varName + "0");
			varDeclStmtNode.get(2).set(0, objNode.get(i));
			this.visit(varDeclStmtNode);
			objNode.get(i).get(0).setValue(varName);
			this.objFlag = true;
		}
		for(String addedGetterName: this.addedGetterList){
			this.currentBuilder.appendNewLine(addedGetterName);
		}
		this.prefixName = this.prefixName.substring(0, this.prefixName.length() - (name + ".").length());
		this.currentBuilder.appendNewLine("end");
		this.currentBuilder.unIndent();
		this.varList.add(new FSharpVar(name, this.prefixName));
		this.currentBuilder.appendNewLine("let " + searchVarFromList(name, false).getCurrentName() + " = new ClassOf" + name + "(0)");
		this.objFlag = false;
	}
	
	protected void generateScope(FSharpScope fs, boolean isTopLevel){
		this.currentScope = fs;
		this.prefixName = fs.getPathName();
		String classType = isTopLevel? "type" : "and";
		this.currentBuilder.appendNewLine(classType + " " + fs.getScopeName() + " () = class");
		
		this.currentBuilder.indent();
		FSharpVar fv;

		if(fs.node.is(JSTag.TAG_FUNC_DECL)){
			CommonTree argsNode = fs.node.get(4);
			CommonTree arg;
			String argsStr = "";
			for(int i = 0; i < argsNode.size(); i++){
				arg = argsNode.get(i);
				argsStr += " " + arg.getText();
				this.currentBuilder.appendNewLine("let _" + arg.getText() + "_middle = ref None");
			}
			if(fs.recursive){
				this.currentBuilder.appendNewLine("let rec _" + fs.name + argsStr + " =");
			} else {
				this.currentBuilder.appendNewLine("member self." + fs.name + argsStr + " =");
			}
			this.currentBuilder.indent();
			for(int i = 0; i < argsNode.size(); i++){
				arg = argsNode.get(i);
				this.currentBuilder.appendNewLine("_" + arg.getText() + "_middle := " + "!" + arg.getText());
			}
			this.currentBuilder.unIndent();
			this.generateBlock(fs.node.get(6));
			this.currentBuilder.indent();
			if(fs.returnList.size() > 1) {
				this.assignFlag = true;
				CommonTree firstReturnNode = fs.returnList.get(0).get(0);
				for(int i = 1; i < fs.returnList.size(); i++){
					this.currentBuilder.appendNewLine();
					this.visit(firstReturnNode);
					this.visit(fs.returnList.get(i).get(0), " = ", "");
				}
				this.currentBuilder.appendNewLine();
				this.visit(firstReturnNode, "ref(", ")");
				this.assignFlag = false;
			}
			this.currentBuilder.unIndent();
			if(fs.recursive){
				this.currentBuilder.appendNewLine("member self." + fs.name + argsStr + " = _" + fs.name + argsStr);
			}
		}
//		for(FSharpScope child : fs.children){
//			this.currentBuilder.appendNewLine("member self." + child.getFullname() + "= new " + child.getScopeName() + "()");
//		}
//		FSharpScope parentScope = fs.parent;
//		while(parentScope != null){
//			this.currentBuilder.appendNewLine("member self." + parentScope.name + "= new " + parentScope.getScopeName() + "()");
//			parentScope = parentScope.parent;
//		}
		this.letFlag = true;
		for(int i = fs.numOfArgs; i < fs.varList.size(); i++){
			fv = fs.varList.get(i);
			if(fv.initialValue == null){
				this.currentBuilder.appendNewLine("member self." + fv.getTrueName() + " = ref None");
			} else {
				this.currentBuilder.appendNewLine("member self." + fv.getTrueName() + " = ref (Some(");
				CommonTree initParentNode = fv.initialValue.getParent();
				int initIndex = this.indexOf(fv.initialValue);
				this.formatRightSide(fv.initialValue);
				this.visit(initParentNode.get(initIndex));
				this.currentBuilder.append("))");
			}
		}
		this.letFlag = false;
		for(FSharpFunc ff : fs.funcList){
			this.currentBuilder.appendNewLine("member self." + ff.name + " " + ff.argsStr + " = (new ScopeOf" + fs.getFullname() + "_" + ff.name + "())." + ff.name + " " + ff.argsStr);
		}
//		for(int i = fs.numOfArgs; i < fs.varList.size(); i++){
//			fv = fs.varList.get(i);
//			this.currentBuilder.appendNewLine("member self.g_" + fv.getTrueName() + " = self." + fv.getTrueName());
//		}
		this.currentBuilder.appendNewLine("end");
		this.currentBuilder.unIndent();
		this.prefixName = "";
		this.currentScope = null;
	}
	
	public void toSource(CommonTree node) {
		this.initialSetting(node);
		this.generateScope(this.scopeList.get(0), true);
		for(int i = 1; i < this.scopeList.size(); i++){
			this.generateScope(this.scopeList.get(i), false);
		}
		this.generateTypeCode();
	}
	
	public void toName(CommonTree node) {
		String varName = node.getText();
		FSharpVar targetVar = searchVarFromList(varName, false);
		FSharpScope fs = null;
		if(targetVar != null){
			varName = targetVar.getCurrentName();
			for(FSharpScope targetScope : this.scopeList){
				for(FSharpVar fv : targetScope.varList){
					if(fv == targetVar && targetScope != this.currentScope){
						fs = targetScope;
						this.currentBuilder.append("(new " + fs.getScopeName() + "())." + node.getText());
						break;
					} else if(fv == targetVar && targetScope == this.currentScope && !this.currentScope.isArgumentVar(fv.getTrueName())){
						fs = targetScope;
						this.currentBuilder.append("self." + node.getText());
						break;
					}
				}
			}
		}
		if(fs == null){
			this.currentBuilder.append(node.getText());
		}
	}
	
	public void toInteger(CommonTree node) {
		this.currentBuilder.append(node.getText() + ".0");
	}
	
	public void toDecimalInteger(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void toOctalInteger(CommonTree node) {
		this.currentBuilder.append(node.getText());
		if(node.getText().contentEquals("0") && !forFlag){
			this.currentBuilder.append(".0");
		}
	}
	
	public void toHexInteger(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void toLong(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void toDecimalLong(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void toOctalLong(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void toHexLong(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void toFloat(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}

	public void toDouble(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void toHexFloat(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}

	public void toHexDouble(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void toString(CommonTree node) {
		this.currentBuilder.appendChar('"');
		this.currentBuilder.append(node.getText());
		this.currentBuilder.appendChar('"');
	}
	
	public void toRegularExp(CommonTree node) {
		this.currentBuilder.append(node.getText());
	}
	
	public void toText(CommonTree node) {
		/* do nothing */
	}
	
	public void toThis(CommonTree node) {
		this.currentBuilder.append("this");
	}
	
	public void toTrue(CommonTree node) {
		this.currentBuilder.append("true");
	}
	
	public void toFalse(CommonTree node) {
		this.currentBuilder.append("false");
	}
	
	public void toNull(CommonTree node) {
		this.currentBuilder.append("null");
	}
	
	public void toList(CommonTree node) {
		generateList(node, ", ");
	}
	
	public void toBlock(CommonTree node) {
		for(CommonTree element : node){
			this.currentBuilder.appendNewLine();
			this.visit(element);
		}
	}
	
	public void generateBlock(CommonTree node) {
		this.currentBuilder.indent();
		this.visit(node);
		if(!this.checkReturn(node, false)){
			this.currentBuilder.appendNewLine("ref(new fsLib.fl.Void(0))");
		}
		this.currentBuilder.unIndent();
	}
	
	public void toArray(CommonTree node){
		this.currentBuilder.append("[|");
		this.generateList(node, "; ");
		this.currentBuilder.append("|]");
	}
	
	@Deprecated
	public void toObject(CommonTree node){
		
		//this.generateClass(node.getParent());
	}
	
	public void toProperty(CommonTree node) {
		this.generateBinary(node, ": ");
	}

	public void toSuffixInc(CommonTree node) {
		//this.genarateSuffixUnary(node, "++");
	}

	public void toSuffixDec(CommonTree node) {
		//this.genarateSuffixUnary(node, "--");
	}

	public void toPrefixInc(CommonTree node) {
		//this.genaratePrefixUnary(node, "++");
	}

	public void toPrefixDec(CommonTree node) {
		//this.genaratePrefixUnary(node, "--");
	}

	public void toPlus(CommonTree node) {
		this.visit(node.get(0));
	}

	public void toMinus(CommonTree node) {
		this.genaratePrefixUnary(node, "-");
	}

	public void toAdd(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")+(");
		this.currentBuilder.append(")");
	}

	public void toSub(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")-(");
		this.currentBuilder.append(")");
	}

	public void toMul(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")*(");
		this.currentBuilder.append(")");
	}

	public void toDiv(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")/(");
		this.currentBuilder.append(")");
	}

	public void toMod(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")%(");
		this.currentBuilder.append(")");
	}

	public void toLeftShift(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<<<(");
		this.currentBuilder.append(")");
	}

	public void toRightShift(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>>>(");
		this.currentBuilder.append(")");
	}

	public void toLogicalLeftShift(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<<<(");
		this.currentBuilder.append(")");
	}

	public void toLogicalRightShift(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>>>(");
		this.currentBuilder.append(")");
	}

	public void toGreaterThan(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>(");
		this.currentBuilder.append(")");
	}

	public void toGreaterThanEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")>=(");
		this.currentBuilder.append(")");
	}

	public void toLessThan(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<(");
		this.currentBuilder.append(")");
	}

	public void toLessThanEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<=(");
		this.currentBuilder.append(")");
	}

	public void toEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")=(");
		this.currentBuilder.append(")");
	}

	public void toNotEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<>(");
		this.currentBuilder.append(")");
	}
	
	public void toStrictEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")=(");
		this.currentBuilder.append(")");
	}

	public void toStrictNotEquals(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")<>(");
		this.currentBuilder.append(")");
	}

	//none
	public void toCompare(CommonTree node) {
		this.currentBuilder.append("(");
		this.generateBinary(node, ")-(");
		this.currentBuilder.append(")");
	}
	
	public void toInstanceOf(CommonTree node) {
		this.currentBuilder.append("(");
		this.visit(node.get(0));
		this.currentBuilder.append(").constructor.name === ");
		this.visit(node.get(1));
		this.currentBuilder.append(".name");
	}
	
	public void toStringInstanceOf(CommonTree node) {
		//this.generateBinary(node, " instanceof ");
	}
	
	public void toHashIn(CommonTree node) {
		//this.generateBinary(node, " in ");
	}

	public void toBitwiseAnd(CommonTree node) {
		this.generateBinary(node, "&&&");
	}

	public void toBitwiseOr(CommonTree node) {
		this.generateBinary(node, "|||");
	}

	public void toBitwiseNot(CommonTree node) {
		this.generateBinary(node, "not");
	}

	public void toBitwiseXor(CommonTree node) {
		this.generateBinary(node, "^");
	}

	public void toLogicalAnd(CommonTree node) {
		this.generateBinary(node, "&&");
	}

	public void toLogicalOr(CommonTree node) {
		this.generateBinary(node, "||");
	}

	public void toLogicalNot(CommonTree node) {
		this.genaratePrefixUnary(node, "not");
	}

	public void toLogicalXor(CommonTree node) {
		this.generateBinary(node, "^^^");
	}

	public void toConditional(CommonTree node) {
		this.generateTrinaryAddHead(node, "if", "then", "else");
	}
	
	protected FSharpScope searchScopeFromList(String name){
		String pathName;
		String prefixName = this.prefixName;
		String[] prefixNameElements = prefixName.split(".");
		if(prefixNameElements.length == 0){
			prefixNameElements = new String[1];
			prefixNameElements[0] = prefixName.substring(0, prefixName.length() - 1);
		}
		for(FSharpScope element : this.scopeList){
			pathName = element.getPathName();
			if(pathName.substring(0, pathName.length()-1).contentEquals(prefixName + name)){
				return element;
			}
		}
		if(prefixNameElements != null){
			for(int i = prefixNameElements.length - 1; i >= 0; i--){
				if(prefixName.length() > 0){
					prefixName = prefixName.substring(0, prefixName.length() - (prefixNameElements[i].length() + 1));
				}
				for(FSharpScope element : this.scopeList){
					pathName = element.getPathName();
					if(pathName.substring(0, pathName.length()-1).contentEquals(prefixName + name)){
						return element;
					}
				}
			}
		}
		return null;
	}
	
	protected void formatRightSide(CommonTree node){
		if(node.is(JSTag.TAG_APPLY)){
			this.formatRightSide(node.get(1));
		} else if(node.is(JSTag.TAG_FIELD)){
			String fieldValue = "(!(";
			FSharpScope fs;
			FSharpVar targetVar = null;
			for(int i = 0; i < node.size()-1; i++){
				fs = this.currentScope.getAvailableScope(node.get(i).getText());
				if(fs != null){
					fieldValue += "(new " + fs.getScopeName() + "()).";
					targetVar = fs.searchVar(node.get(node.size()-1).getText());
				}
				fs = null;
			}
			
			if(targetVar != null){
				fieldValue += targetVar.getTrueName() + ")).Value";
			} else {
				fieldValue += node.get(0).getText() + ")).Value";
			}
			CommonTree nameNode = new CommonTree(Tag.tag("Name"), null, 0, 0, 0, fieldValue);
			node.getParent().set(this.indexOf(node), nameNode);
		} else if(node.is(JSTag.TAG_NAME)){
			String name = node.getText();
//			FSharpScope target = this.searchVarOrFuncFromScopeList(this.currentScope, name);
//			if(target == null){
//				node.setValue("(!(" + name + ")).Value");
//			} else {
//				node.setValue("(!(" + target.getPathName() + name + ")).Value");
//			}
			if(!node.getParent().is(JSTag.TAG_FIELD)){
				FSharpVar targetVar = searchVarFromList(name, false);
				FSharpScope fs = null;
				if(targetVar == null){
					targetVar = this.currentScope.getAvailableVar(name);
				}
				if(targetVar != null){
					name = targetVar.getTrueName();
					String trueName = name;
					for(FSharpScope targetScope : this.scopeList){
						for(FSharpVar fv : targetScope.varList){
							if(fv == targetVar && targetScope != this.currentScope){
								fs = targetScope;
								name = "(new " + fs.getScopeName() + "())." + name;
							}
						}
					}
					if(this.currentScope.searchVar(trueName) != null && !this.currentScope.isArgumentVar(trueName)){
						name = "self." + name;
					}
					node.setValue("(!(" + name + ")).Value");
				} else {
					FSharpScope tScope = this.currentScope.getAvailableScope(name);
					if(tScope != null){
						node.setValue("new " + tScope.getScopeName() + "()");
					}
				}
			}
		} else {
			for(int i = 0; i < node.size(); i++){
				this.formatRightSide(node.get(i));
			}
		}
	}
	
	protected FSharpScope searchVarOrFuncFromScopeList(FSharpScope targetScope, String targetName){
		ArrayList<String> scopePath = new ArrayList<String>();
		ArrayList<String> pathes = new ArrayList<String>();
		FSharpScope result = null;
		for(String element : targetScope.path){
			scopePath.add(element);
		}
		
		while(scopePath.size() > 0){
			pathes.add(scopePath.toString());
			scopePath.remove(scopePath.size() - 1);
		}
		pathes.add("[]");
		
		for(int scope_i = 0; scope_i < this.scopeList.size(); scope_i++){
			if(!pathes.isEmpty()){
				for(int path_i = 0; path_i < pathes.size(); path_i++){
					if(pathes.get(path_i).contentEquals(this.scopeList.get(scope_i).path.toString())){
						if(this.scopeList.get(scope_i).searchFunc(targetName) != null || this.scopeList.get(scope_i).searchVar(targetName) != null || this.scopeList.get(scope_i).name.contentEquals(targetName)){
							result = this.scopeList.get(scope_i);
						}
					}
				}
			} else {
				if(("[]").contentEquals(this.scopeList.get(scope_i).path.toString())){
					if(this.scopeList.get(scope_i).searchFunc(targetName) != null || this.scopeList.get(scope_i).searchVar(targetName) != null || this.scopeList.get(scope_i).name.contentEquals(targetName)){
						result = this.scopeList.get(scope_i);
					}
				}
			}
		}
		return result;
	}
	
	public void toAssign(CommonTree node) {
		this.assignFlag = true;
		this.formatRightSide(node.get(1));
		//this.setVarNameInBinary(node, true);
		String varName = this.getFieldText(node.get(0));
		FSharpVar targetVar = searchVarFromList(varName, node.get(0).is(JSTag.TAG_FIELD));
		if(targetVar == null){
			this.varList.add(new FSharpVar(varName, this.prefixName));
			targetVar = this.varList.get(this.varList.size()-1);
			this.currentBuilder.append("let ");
		}
		//checkAssignVarName(node.get(1), targetVar);
		targetVar.addChild();
		
		this.generateBinary(node, " := Some(");
		this.currentBuilder.append(")");
		this.assignFlag = false;
	}

	public void toMultiAssign(CommonTree node) {
		CommonTree lhs = node.get(0);
		CommonTree rhs = node.get(1);
		if(lhs.size() == 1 && rhs.size() == 1 && !rhs.get(0).is(JSTag.TAG_APPLY) && !rhs.get(0).is(JSTag.TAG_APPLY)){
			this.visit(lhs.get(0));
			this.currentBuilder.append(" = ");
			this.visit(rhs.get(0));
		}else{
			this.currentBuilder.append("multiAssign (");
			generateList(lhs, ", ");
			this.currentBuilder.append(") = (");
			generateList(rhs, ", ");
			this.currentBuilder.appendChar(')');
		}
	}
	
	private void generateAssignCalc(CommonTree node, String tagName){
		CommonTree rexpr = new CommonTree(Tag.tag(tagName), null, 0, 0, 0, null);
		CommonTree lexpr = node.get(0).dup();
		rexpr.add(lexpr);
		rexpr.add(node.get(1));
		node.setTag(Tag.tag("Assign"));
		node.set(1, rexpr);
		this.toAssign(node);
	}
	
	public void toAssignAdd(CommonTree node) {
		this.generateAssignCalc(node, "Add");
	}

	public void toAssignSub(CommonTree node) {
		this.generateAssignCalc(node, "Sub");
	}

	public void toAssignMul(CommonTree node) {
		this.generateAssignCalc(node, "Mul");
	}

	public void toAssignDiv(CommonTree node) {
		this.generateAssignCalc(node, "Div");
	}

	public void toAssignMod(CommonTree node) {
		this.generateAssignCalc(node, "Mod");
	}

	public void toAssignLeftShift(CommonTree node) {
		this.generateBinary(node, "<<=");
	}

	public void toAssignRightShift(CommonTree node) {
		this.generateBinary(node, ">>=");
	}

	public void toAssignLogicalLeftShift(CommonTree node) {
		this.generateBinary(node, "<<<=");
	}

	public void toAssignLogicalRightShift(CommonTree node) {
		this.generateBinary(node, ">>>=");
	}

	public void toAssignBitwiseAnd(CommonTree node) {
		this.generateBinary(node, "&=");
	}

	public void toAssignBitwiseOr(CommonTree node) {
		this.generateBinary(node, "|=");
	}

	public void toAssignBitwiseXor(CommonTree node) {
		this.generateBinary(node, "^=");
	}

	public void toAssignLogicalAnd(CommonTree node) {
		this.generateBinary(node, "&&=");
	}

	public void toAssignLogicalOr(CommonTree node) {
		this.generateBinary(node, "||=");
	}

	public void toAssignLogicalXor(CommonTree node) {
		this.generateBinary(node, "^=");
	}

//	public void toMultipleAssign(CommonTree node) {
//		
//	}

	public void toComma(CommonTree node) {
		if(node.size() > 2){
			this.currentBuilder.appendChar('(');
			this.generateList(node, ", ");
			this.currentBuilder.appendChar(')');	
		}else{
			this.generateBinary(node, ", ");
		}
	}
	
	//none
	public void toConcat(CommonTree node) {
		this.generateBinary(node, " + ");
	}

	public void toField(CommonTree node) {
		CommonTree field;
		FSharpScope fs;
		for(int i = 0; i < node.size()-1; i++){
			field = node.get(i);
			fs = this.searchScopeFromList(field.getText());
			if(fs != null){
				field.setValue(fs.getScopeName());
			}
			fs = null;
		}
		this.generateBinary(node, ".");
	}

	public void toIndex(CommonTree node) {
		generateExpression(node.get(0));
		this.visit(node.get(1), ".[(int (", "))]");
	}

	//none
	public void toMultiIndex(CommonTree node) {
		generateExpression(node.get(0));
		for(CommonTree indexNode : node.get(1)){
			this.visit(indexNode, '[', ']');
		}
	}
	
	private boolean containsVariadicValue(CommonTree list){
		for(CommonTree item : list){
			if(item.is(JSTag.TAG_VARIADIC_PARAMETER)
					|| item.is(JSTag.TAG_APPLY)
					|| item.is(JSTag.TAG_MULTIPLE_RETURN_APPLY)
					|| item.is(JSTag.TAG_METHOD)
					|| item.is(JSTag.TAG_MULTIPLE_RETURN_METHOD)){
				return true;
			}
		}
		return false;
	}
	
	protected void formatApplyFuncName(CommonTree node){
		if(node.is(JSTag.TAG_FIELD)){
			CommonTree field;
			FSharpScope fs;
			String fieldValue = "";
			CommonTree child = node.get(0);
			if(child.is(JSTag.TAG_APPLY)){
				this.formatApplyFuncName(child.get(0));
				this.formatRightSide(child.get(1));
				fieldValue += "(!(" + child.get(0).getText() + "(ref(Some(" + child.get(1).getText() + "))))).";
			} else {
				fs = this.searchScopeFromList(child.getText());
				if(fs != null){
					fieldValue += "(new " + fs.getScopeName() + "()).";
				}
				//field.setValue(fs.getScopeName());
			}
			this.formatRightSide(node.get(1));
			fieldValue += node.get(1).getText();
			CommonTree nameNode = new CommonTree(Tag.tag("Name"), null, 0, 0, 0, fieldValue);
			node.getParent().set(this.indexOf(node), nameNode);
		} else if(node.is(JSTag.TAG_NAME)){
			FSharpScope fs;
			fs = this.searchVarOrFuncFromScopeList(this.currentScope, node.getText());
			fs = this.currentScope.getAvailableScope(node.getText());
			if(fs != null){
				node.setValue("(new " + fs.getScopeName() + "())" + "." + node.getText());
			}
		}
	}
	
	public void toApply(CommonTree node) {
		CommonTree func = node.get(0);
		CommonTree arguments = node.get(1);
		if(this.assignFlag){
			this.currentBuilder.append("!(");
		}
		//if(this.checkApplyFuncName(getFieldText(func))){
		this.formatApplyFuncName(func);
		this.formatRightSide(arguments);
		func = node.get(0);
		boolean asFlag = this.assignFlag;
		this.assignFlag = true;
		this.visit(func);
		this.assignFlag = asFlag;
		if(arguments.size() > 0){
			this.currentBuilder.appendSpace();
			this.currentBuilder.append("(ref(Some(");
			this.generateList(arguments, "))) (ref(Some(");
			this.currentBuilder.append(")))");
		}
		//}
		if(this.assignFlag){
			this.currentBuilder.append(")");
		}
	}

	//none
	public void toMethod(CommonTree node) {
		this.generateBinary(node, ".");
		this.currentBuilder.appendChar('(');
		this.generateList(node.get(2), ", ");
		this.currentBuilder.appendChar(')');
	}
	
	public void toTypeOf(CommonTree node) {
		this.currentBuilder.append("(");
		generateExpression(node.get(0));
		this.currentBuilder.append(")");
		this.currentBuilder.append(".GetType().GetMethod().[0].toString()");
	}

	public void toIf(CommonTree node) {
		String thenBlock;
		this.assignFlag = true;
		this.visit(node.get(0), "if (", ")");
		this.assignFlag = false;
		this.currentBuilder.appendNewLine("then");
		int start = this.currentBuilder.getPosition();
		this.generateBlock(node.get(1));
		if(this.checkReturn(node.get(1), false)){
			if(!isNullOrEmpty(node, 2)){
				CommonTree elseBlock = node.get(2);
				if(!this.checkReturn(elseBlock, false)){
					CommonTree parent = node.getParent();
					CommonTree element;
					long currentPosition = node.getSourcePosition();
					for(int i = 0; i < parent.size(); i++){
						element = parent.get(i);
						if(currentPosition < element.getSourcePosition()){
							elseBlock.add(element);
							parent.remove(i);
						}
					}
				}
			} else {
				CommonTree elseBlock = new CommonTree(Tag.tag("Block"), null, 0, 0, 0, null);
				node.set(2, elseBlock);
				CommonTree parent = node.getParent();
				CommonTree element;
				long currentPosition = node.getSourcePosition();
				for(int i = 0; i < parent.size(); i++){
					element = parent.get(i);
					if(currentPosition < element.getSourcePosition()){
						elseBlock.add(element);
						parent.remove(i);
					}
				}
				if(elseBlock.size() < 1){
					node.remove(2);
				}
			}
		}
		thenBlock = this.currentBuilder.substring(start, this.currentBuilder.getPosition());
		if(!isNullOrEmpty(node, 2)){
			this.currentBuilder.appendNewLine("else");
			this.generateBlock(node.get(2));
		} else {
			this.currentBuilder.appendNewLine("else");
			this.currentBuilder.append(thenBlock);
		}
	}

	public void toWhile(CommonTree node) {
		int begin, end;
		String thenBlock;
		this.visit(node.get(0), "if ", "");
		this.currentBuilder.appendNewLine("then");
		begin = this.currentBuilder.getPosition();
		this.generateBlock(node.get(1));
		end = this.currentBuilder.getPosition();
		this.currentBuilder.appendNewLine("else");
		this.currentBuilder.append(this.currentBuilder.substring(begin, end));
//		this.currentBuilder.indent();
//		this.currentBuilder.append("printfn " + this.currentBuilder.quoteString + "dammy" + this.currentBuilder.quoteString);
//		this.currentBuilder.appendNewLine("done");
//		this.currentBuilder.unIndent();
	}

	public void toFor(CommonTree node) {
		int begin, end;
		String thenBlock;
		this.forFlag = true;
		CommonTree exp1 = node.get(0).get(0);
		this.currentBuilder.append("for ");
		if(exp1.is(JSTag.TAG_VAR_DECL)){
			this.currentBuilder.append(exp1.get(0).getText());
			this.forConunter = exp1.get(0).getText();
			this.formatForCounter(node.get(1));
			this.formatForCounter(node.get(3));
		}
		
		this.forFlag = false;
		this.currentBuilder.append("=0 to 1 do");
		this.currentBuilder.indent();
		this.currentBuilder.appendNewLine();
		this.visit(node.get(1), "if (", ") ");
		this.currentBuilder.appendNewLine("then");
		begin = this.currentBuilder.getPosition();
		this.generateBlock(node.get(3));
		end = this.currentBuilder.getPosition();
		thenBlock = this.currentBuilder.substring(begin, end);
		this.currentBuilder.appendNewLine("else");
		this.currentBuilder.indent();
		this.currentBuilder.append(thenBlock);
		this.currentBuilder.unIndent();
		this.currentBuilder.unIndent();
		this.currentBuilder.appendNewLine("done");	
		this.forConunter = "";
	}
	
	public void toForCounter(CommonTree node){
		this.currentBuilder.append(node.getText());
	}
	
	protected void formatForCounter(CommonTree node){
		if(node.is(JSTag.TAG_NAME)){
			if(node.getText().contentEquals(this.forConunter)){
				node.getParent().set(indexOf(node), new CommonTree(Tag.tag("ForCounter"), null, 0, 0, 0, "double " + this.forConunter));
			}
		} else {
			for(int i = 0; i < node.size(); i++){
				this.formatForCounter(node.get(i));
			}
		}
	}
	
	public void toJSForeach(CommonTree node) {
		CommonTree param1 = node.get(0);
		if(param1.is(JSTag.TAG_LIST)){
			this.currentBuilder.append("for " + param1.get(0).get(0).getText() + " in ");
		} else if(param1.is(JSTag.TAG_NAME)){
			this.currentBuilder.append("for " + param1.getText() + " in ");
		}
		this.visit(node.get(1));
		this.currentBuilder.append(" do");
		this.generateBlock(node.get(2));
		this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "done");
		if(!isNullOrEmpty(node, 3)){
			this.currentBuilder.appendNewLine();
			this.visit(node.get(3));
		}
	}

	public void toDoWhile(CommonTree node) {
		this.currentBuilder.append("do");
		this.generateBlock(node.get(0));
		this.visit(node.get(1), "while (", ")");
	}
	
	protected void generateJump(CommonTree node, String keyword){
		if(!isNullOrEmpty(node, 0)){
			this.currentBuilder.appendSpace();
			CommonTree returnValue = node.get(0);
			if(returnValue.is(JSTag.TAG_LIST)){
				this.currentBuilder.append("multiple m");
				this.currentBuilder.append(keyword);
				this.generateList(returnValue, " (", ", ", ")");
			}else{
				this.currentBuilder.append(keyword);
				this.visit(returnValue);
			}
		}
	}

	public void toReturn(CommonTree node) {
		this.assignFlag = true;
		this.formatRightSide(node.get(0));
		this.visit(node.get(0), "ref(", ")");
		this.assignFlag = false;
	}

	public void toBreak(CommonTree node) {
		//this.generateJump(node, "break");
	}

	public void toYield(CommonTree node) {
		//this.generateJump(node, "yield");
	}

	public void toContinue(CommonTree node) {
		//this.generateJump(node, "continue");
	}

	public void toRedo(CommonTree node) {
		this.generateJump(node, "/*redo*/");
	}

	public void toSwitch(CommonTree node) {
		this.visit(node.get(0), "match ", " with");
		
		this.currentBuilder.indent();
		
		for(CommonTree element : node.get(1)){
			this.currentBuilder.appendNewLine();
			this.visit(element);
		}
		
		this.currentBuilder.unIndent();
	}

	public void toCase(CommonTree node) {
		this.visit(node.get(0), "| ", "->");
		this.currentBuilder.indent();
		this.currentBuilder.appendNewLine();
		if(!isNullOrEmpty(node, 1)){
			this.visit(node.get(1));
		}
		if(this.checkReturn(node, false)){
			this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "ref(new fsLib.fl.Void(0))");
		}
		this.currentBuilder.unIndent();
	}

	public void toDefault(CommonTree node) {
		this.currentBuilder.append("| _ ->");
		this.currentBuilder.indent();
		this.visit(node.get(0));
		this.currentBuilder.unIndent();
	}

	public void toTry(CommonTree node) {
		this.currentBuilder.append("try");
		this.generateBlock(node.get(0));
		
		if(!isNullOrEmpty(node, 1)){
			for(CommonTree element : node.get(1)){
				this.visit(element);
			}
		}
		if(!isNullOrEmpty(node, 2)){
			this.currentBuilder.append("finally");
			this.visit(node.get(2));
		}
	}

	//TODO
	public void toCatch(CommonTree node) {
		this.visit(node.get(0), "with(", ")");
		this.generateBlock(node.get(1));
	}
	
	public void toVarDeclStmt(CommonTree node) {
		boolean objLet = this.objFlag;
		CommonTree listNode = node.get(2);
		CommonTree varDeclNode = listNode.get(0);
		try{
			CommonTree varStmtNode = varDeclNode.get(1);
			if(!varStmtNode.is(JSTag.TAG_FUNC_DECL) && !varStmtNode.is(JSTag.TAG_OBJECT)){
				this.currentBuilder.append("let ");
				this.objFlag = false;
				this.visit(listNode);
				String name = varDeclNode.get(0).getText();
				if(objLet){
					this.addedGetterList.add("member this." + name + " = " + this.searchVarFromList(name, false).getCurrentName());
				}
			} else if(varStmtNode.is(JSTag.TAG_OBJECT)){
				this.visit(varStmtNode);
			} else {	
				varStmtNode.set(2, varDeclNode.get(0));
				this.visit(varStmtNode);
			}
		} catch(ArrayIndexOutOfBoundsException e){
			this.currentBuilder.append("//let " + varDeclNode.getText() + "0");
			this.varList.add(new FSharpVar(varDeclNode.get(0).getText(), this.prefixName));
		}
	}
	
	public void toVarDecl(CommonTree node) {
		this.varList.add(new FSharpVar(node.get(0).getText(), this.prefixName));
		this.visit(node.get(0));
		if(node.size() > 1){
			this.currentBuilder.append(" = ");
			this.visit(node.get(1), "ref(", ")");
		}
	}
	
	private boolean isRecursiveFunc(CommonTree node, String name, boolean result){
		if(result){
			return true;
		}
		boolean res = false;
		if(node.is(JSTag.TAG_APPLY)){
			res = this.getFieldText(node.get(0)).contentEquals(name);
		} else {
			res = false;
		}
		if(node.size() >= 1 && !result){
			for(int i = 0; i < node.size(); i++){
				res = this.isRecursiveFunc(node.get(i), name, result);
			}
		}
		return res;
	}
	
	public void toFuncDecl(CommonTree node) {
		//boolean mustWrap = this.currentBuilder.isStartOfLine();
//		boolean mustWrap = false;
//		boolean notLambda = node.get(2).is(JSTag.TAG_NAME);
//		boolean memberFlag = objFlag;	
//		
//		if(mustWrap){
//			this.currentBuilder.appendChar('(');
//		}
//		if(notLambda && !objFlag){
//			this.currentBuilder.append("let");
//			if(this.isRecursiveFunc(node.get(6), this.prefixName + node.get(2).getText(), false)){
//				this.currentBuilder.append(" rec");
//			}
//		} else if(!notLambda && !objFlag){
//			this.currentBuilder.append("fun");
//		} else if(objFlag){
//			this.currentBuilder.append("member");
//		}
//		String addName = node.get(2).getText() + ".";
//		this.prefixName += addName;
//		if(!isNullOrEmpty(node, 2)){
//			this.currentBuilder.appendSpace();
//			if(this.objFlag){
//				this.currentBuilder.append("this.");
//			}
//			this.visit(node.get(2));
//		}
//		
//		CommonTree parameters = node.get(4);
//		boolean containsVariadicParameter = false;
//		boolean isFirst = true;
//		int sizeOfParametersBeforeValiadic = 0;
//		int sizeOfParametersAfterValiadic = 0;
//		
//		this.currentBuilder.appendChar(' ');
//		//this.prefixName = this.nameList.get(this.nameList.size() - 1);
//		
//		for(CommonTree param : parameters){
//			if(param.is(JSTag.TAG_VARIADIC_PARAMETER)){
//				containsVariadicParameter = true;
//				sizeOfParametersAfterValiadic = 0;
//				continue;
//			}
//			if(containsVariadicParameter){
//				sizeOfParametersAfterValiadic++;
//			}else{
//				sizeOfParametersBeforeValiadic++;
//			}
//			if(!isFirst){
//				this.currentBuilder.append(" ");
//			}
//			this.varList.add(new FSharpVar(param.getText(), this.prefixName));
//			this.visit(param);
//			isFirst = false;
//		}
//		this.currentBuilder.appendChar(' ');
//		
//		if(notLambda){
//			this.currentBuilder.appendChar('=');
//		} else {
//			this.currentBuilder.append("->");
//		}
//		this.currentBuilder.indent();
//		if(containsVariadicParameter){
//			this.currentBuilder.appendNewLine("var __variadicParams = __markAsVariadic([]);");
//			this.currentBuilder.appendNewLine("for (var _i = ");
//			this.currentBuilder.appendNumber(sizeOfParametersBeforeValiadic);
//			this.currentBuilder.append(", _n = arguments.length - ");
//			this.currentBuilder.appendNumber(sizeOfParametersAfterValiadic);
//			this.currentBuilder.append("; _i < _n; ++_i){ __variadicParams.push(arguments[_i]); }");
//		}
//		this.objFlag = false;
//		this.visit(node.get(6));
//		if(!checkReturn(node.get(6), false)){
//			if(!memberFlag){
//				this.currentBuilder.appendNewLine(this.currentBuilder.indentString + "new fsLib.fl.Void(0)");
//			} else {
//				this.currentBuilder.appendNewLine("new fsLib.fl.Void(0)");
//			}
//		}
//		this.currentBuilder.unIndent();
//		if(mustWrap){
//			this.currentBuilder.appendChar(')');
//		}
//		
//		this.prefixName = this.prefixName.substring(0, this.prefixName.length() - addName.length());
	}
	
	public void toDeleteProperty(CommonTree node) {
		this.currentBuilder.append("delete ");
		this.visit(node.get(0));
	}
	
	public void toVoidExpression(CommonTree node) {
		this.currentBuilder.append("void(");
		this.visit(node.get(0));
		this.currentBuilder.append(")");
	}
	
	public void toThrow(CommonTree node) {
		this.currentBuilder.append("throw ");
		this.visit(node.get(0));
	}
	
	public void toNew(CommonTree node) {
		this.currentBuilder.append("new ");
		this.visit(node.get(0));
		this.currentBuilder.appendChar('(');
		if(!isNullOrEmpty(node, 1)){
			this.generateList(node.get(1), ", ");
		}
		this.currentBuilder.appendChar(')');
	}

	public void toEmpty(CommonTree node) {
		this.currentBuilder.appendChar(';');
	}
	
	public void toVariadicParameter(CommonTree node){
		this.currentBuilder.append("__variadicParams");
	}
	
	public void toCount(CommonTree node){
		this.visit(node.get(0));
		this.currentBuilder.append(".length");
	}

}
