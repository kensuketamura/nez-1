package nez.anne;

import nez.ast.Tree;
import nez.lang.Expression;

public interface AnneExpressionTransducer {
	public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next);
}
