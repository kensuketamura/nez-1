package nez.infer;

import nez.Grammar;
import nez.ast.Tree;
import nez.lang.Expression;
import nez.lang.expr.ExpressionCommons;
import nez.util.UList;

public class Token implements InferenceTokenSymbol {
	protected String label;
	protected Histogram histogram;

	public Token(String label, int totalNumOfChunks) {
		this.label = label;
		this.histogram = new Histogram(label, totalNumOfChunks);
	}

	public Histogram getHistogram() {
		return this.histogram;
	}

	// return similarity of histogram against a specified token's one
	public double calcHistogramSimilarity(Token target) {
		return Histogram.calcSimilarity(this.histogram, target.getHistogram());
	}

	public Expression getExpression(Grammar g) {
		return ExpressionCommons.newNonTerminal(null, g, this.label);
	}

	@Override
	public String toString() {
		return this.label;
	}
}

class DelimToken extends Token {

	public DelimToken(String label, int totalNumOfChunks) {
		super(label, totalNumOfChunks);
	}

	@Override
	public Expression getExpression(Grammar g) {
		return ExpressionCommons.newString(null, this.label);
	}

	@Override
	public String toString() {
		return String.format("\"%s\"", this.label);
	}
}

class MetaToken extends Token {
	Tree<?> innerNode;

	public MetaToken(String label, int totalNumOfChunks, Tree<?> node) {
		super(label, totalNumOfChunks);
		this.innerNode = node;
	}

	@Override
	public Expression getExpression(Grammar g) {
		return this.getExpression(innerNode, g);
	}

	// FIXME
	// assume that there is no nested MetaToken
	public Expression getExpression(Tree<?> node, Grammar g) {
		UList<Expression> l = new UList<Expression>(new Expression[3]);
		l.add(ExpressionCommons.newString(null, node.getText(_open, "")));
		l.add(ExpressionCommons.newNonTerminal(null, g, node.get(_value).getTag().toString()));
		l.add(ExpressionCommons.newString(null, node.getText(_close, "")));
		return ExpressionCommons.newPsequence(null, l);
	}
}
