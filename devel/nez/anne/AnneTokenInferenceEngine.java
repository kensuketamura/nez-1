package nez.anne;

import java.util.HashMap;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.infer.InferenceEngine;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;

public class AnneTokenInferenceEngine {
	private HashMap<String, SemiInferedExpression> exprList;

	private class SemiInferedExpression {
		public StringBuilder data;
		private InferenceEngine inferEngine;
		private final int order;
		private final String name;

		public SemiInferedExpression(String name, int order) {
			this.data = new StringBuilder();
			this.inferEngine = new InferenceEngine();
			this.name = name;
			this.order = order;
		}

		public Production replaceExpr(Grammar grammar) {
			Production prod = grammar.getProduction(name);
			Expression expr = prod.getExpression();
			// FIXME
			expr.set(order + 1, this.infer(grammar));
			return prod;
		}

		public Expression infer(Grammar grammar) {
			String sampleData = data.toString();
			Expression infered = inferEngine.infer(sampleData, grammar);
			return infered;
		}

		public void addData(String sample) {
			if (data.toString().equals("")) {
				data.append(sample);
			} else {
				data.append("\n" + sample);
			}
		}
	}

	public AnneTokenInferenceEngine() {
		this.exprList = new HashMap<>();
	}

	private static Symbol _Token = Symbol.unique("Token");

	public Grammar infer(Grammar base, Tree<?> node) {
		visitToken(node, null, 0);
		for (String key : exprList.keySet()) {
			exprList.get(key).replaceExpr(base);
		}
		return base;
	}

	private void visitToken(Tree<?> node, Symbol parent, int order) {
		String prodName = null;
		if (parent != null) {
			prodName = parent.toString();
		}

		if (node.getTag() == _Token) {
			String index = prodName + ": " + order;
			SemiInferedExpression expr = exprList.get(index);
			if (expr == null) {
				expr = new SemiInferedExpression(prodName, order);
				exprList.put(index, expr);
			}
			expr.addData(node.toText());
		}

		Symbol tag = node.getTag();
		int i = 0;
		for (Tree<?> sub : node) {
			visitToken(sub, tag, i);
			i++;
		}
	}
}
