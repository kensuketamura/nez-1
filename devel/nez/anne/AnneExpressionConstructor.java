package nez.anne;

import nez.ast.Tree;
import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.ast.ExpressionConstructor;
import nez.lang.ast.GrammarVisitorMap;
import nez.parser.ParserStrategy;
import nez.util.UList;

public class AnneExpressionConstructor extends GrammarVisitorMap<AnneExpressionTransducer> implements ExpressionConstructor, AnneSymbols {

	private static final String unnamedProductionName = "_unnamedProduction";
	private int uniqueId = 0;

	public AnneExpressionConstructor(Grammar grammar, ParserStrategy strategy) {
		super(grammar, strategy);
		init(AnneExpressionConstructor.class, new TreeVisitor());
	}

	public void load(Tree<?> node) {
		for (int i = 0; i < node.size(); i++) {
			Tree<?> prev = (i > 0) ? node.get(i - 1) : null;
			Tree<?> sub = node.get(i);
			Tree<?> next = (i < node.size() - 1) ? node.get(i + 1) : null;
			this.find(key(node.get(i))).accept(prev, sub, next);
		}
	}

	public Expression newInstance(Tree<?> prev, Tree<?> node, Tree<?> next) {
		return this.find(key(node)).accept(prev, node, next);
	}

	@Override
	public Expression newInstance(Tree<?> node) {
		return this.find(key(node)).accept(null, node, null);
	}

	public UList<Expression> visit(Tree<?> node) {
		UList<Expression> seqs = new UList<>(new Expression[1]);
		for (int i = 0; i < node.size(); i++) {
			Tree<?> prev = (i > 0) ? node.get(i - 1) : null;
			Tree<?> sub = node.get(i);
			Tree<?> next = (i < node.size() - 1) ? node.get(i + 1) : null;
			seqs.add(newInstance(prev, sub, next));
		}
		return seqs;
	}

	public class TreeVisitor implements AnneExpressionTransducer {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			undefined(node);
			return null;
		}
	}

	public class _Nonterminal extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			String localName = node.getText(_name, null);

			UList<Expression> seqList = visit(node.get(_content));
			Expression inner = Expressions.newSequence(seqList);

			Production target = getGrammar().getProduction(localName);
			if (target != null) {
				inner = Expressions.newChoice(target.getExpression(), inner);
			}

			getGrammar().addProduction(localName, inner);
			return Expressions.newNonTerminal(node, getGrammar(), localName);
		}
	}

	public class _Preamble extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			return Expressions.newNonTerminal(node, getGrammar(), node.getText(_name, null));
		}
	}

	public class _Annotation extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			String localName = node.getText(_name, null);

			// generate an expression such as (!(next delim) .)*
			UList<Expression> repeatedExprList = new UList<>(new Expression[2]);
			assert (next.is(_Delim));
			repeatedExprList.add(Expressions.newNot(Expressions.newExpression(node, next.toText())));
			repeatedExprList.add(Expressions.newAny(node));
			Expression repeatedExpr = Expressions.newSequence(repeatedExprList);
			Expression inner = Expressions.newZeroMore(repeatedExpr);

			Production target = getGrammar().getProduction(localName);
			if (target != null) {
				inner = Expressions.newChoice(target.getExpression(), inner);
			}

			getGrammar().addProduction(localName, inner);
			return Expressions.newNonTerminal(node, getGrammar(), localName);
		}
	}

	public class _Alternative extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			String localName = node.getText(_name, null);

			Expression inner = Expressions.newNonTerminal(node, getGrammar(), node.getText(_alt, null));

			Production target = getGrammar().getProduction(localName);
			if (target != null) {
				inner = Expressions.newChoice(target.getExpression(), inner);
			}

			getGrammar().addProduction(localName, inner);
			return Expressions.newNonTerminal(node, getGrammar(), localName);
		}
	}

	public class _Repetition extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			String localName = node.getText(_name, null);
			Expression inner;
			if (node.has(_alt)) {
				inner = Expressions.newRepeat(Expressions.newNonTerminal(node, getGrammar(), node.getText(_alt, null)));
			} else {
				inner = Expressions.newRepeat(newInstance(node.get(_content).get(0)));
			}
			visit(node.get(_content));
			Production target = getGrammar().getProduction(localName);
			if (target != null) {
				inner = Expressions.newChoice(target.getExpression(), inner);
			}
			return Expressions.newNonTerminal(node, getGrammar(), localName);
		}
	}

	public class _Option extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			String localName = node.getText(_name, null);

			UList<Expression> seqList = visit(node.get(_content));
			Expression inner = Expressions.newSequence(seqList);
			inner = Expressions.newOption(inner);

			Production target = getGrammar().getProduction(localName);
			if (target != null) {
				inner = Expressions.newChoice(target.getExpression(), inner);
			}

			getGrammar().addProduction(localName, inner);
			return Expressions.newNonTerminal(node, getGrammar(), localName);
		}
	}

	public class _Constant extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			String localName = node.getText(_name, null);
			if (localName == null) {
				localName = unnamedProductionName + uniqueId++;
			}

			Expression inner = Expressions.newExpression(node, node.getText(_content, null));

			getGrammar().addProduction(localName, inner);
			return Expressions.newNonTerminal(node, getGrammar(), localName);
		}
	}

	public class _Enumeration extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			// FIXME
			String localName = node.getText(_name, null);
			UList<Expression> seqs = new UList<>(new Expression[2]);
			seqs.add(Expressions.newNot(Expressions.newExpression(node, next.toText())));
			seqs.add(Expressions.newAny(node));

			Expression inner = Expressions.newSequence(seqs);
			getGrammar().addProduction(node, localName, inner);
			return Expressions.newNonTerminal(node, getGrammar(), localName);
		}
	}

	public class _Table extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			// TODO
			return null;
		}
	}

	public class _Assertion extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			// TODO
			return null;
		}
	}

	public class _Token extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			// InferenceEngine inferEngine = new InferenceEngine();
			Expression expr = Expressions.newExpression(node, node.toText());
			return expr;
		}
	}

	public class _Delim extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			Expression expr = Expressions.newExpression(node, node.toText());
			return expr;
		}
	}

}
