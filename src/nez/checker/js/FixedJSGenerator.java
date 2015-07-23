package nez.checker.js;

import java.util.ArrayList;

import nez.checker.ModifiableTree;
import nez.checker.SourceGenerator;

public class FixedJSGenerator extends SourceGenerator {
	private ArrayList<JSFunction> funs;
	private ArrayList<JSObject> objs;
	private ArrayList<JSVariable> vars;

	public FixedJSGenerator(JSRewriter rewriter){
		this.funs = rewriter.getFunctionList();
		this.objs = rewriter.getObjectList();
		this.vars = rewriter.getVariableList();
	}
	
	public void toSource(ModifiableTree node){
		for(JSFunction jf : this.funs){
			jf.printVarDecl(currentBuilder);
		}
	}
	
}
