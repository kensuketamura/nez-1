package nez.anne;

import nez.ast.Symbol;
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
	private AnneChunker chunker;

	public AnneExpressionConstructor(Grammar grammar, ParserStrategy strategy) {
		super(grammar, strategy);
		init(AnneExpressionConstructor.class, new TreeVisitor());
		this.chunker = new AnneChunker(getGrammar());
		chunker.appendChunkingGrammar(getGrammar());
	}

	public void load(Tree<?> node) {
		UList<Expression> topProductions = new UList<>(new Expression[node.size()]);
		for (int i = 0; i < node.size(); i++) {
			Tree<?> prev = (i > 0) ? node.get(i - 1) : null;
			Tree<?> sub = node.get(i);
			Tree<?> next = (i < node.size() - 1) ? node.get(i + 1) : null;
			this.find(key(node.get(i))).accept(prev, sub, next);
			if (sub.has(_name)) {
				Expression prod = Expressions.newNonTerminal(getGrammar(), sub.getText(_name, null));
				prod = Expressions.newLinkTree(prod);
				topProductions.add(prod);
			}
		}
		Expression top = Expressions.newChoice(topProductions);
		top = Expressions.newSequence(top, Expressions.newOption(Expressions.newNonTerminal(getGrammar(), "NEWLINE")));
		top = Expressions.newZeroMore(top);
		top = newNode("Top", top);
		getGrammar().addProduction("_TOP", top);
		getGrammar().setStartProduction("_TOP");
	}

	public Expression newInstance(Tree<?> prev, Tree<?> node, Tree<?> next) {
		return this.find(key(node)).accept(prev, node, next);
	}

	@Override
	public Expression newInstance(Tree<?> node) {
		return this.find(key(node)).accept(null, node, null);
	}

	private Expression newNode(String tag, Expression inner) {
		return Expressions.newSequence(Expressions.newBeginTree(), inner, Expressions.newTag(Symbol.unique(tag)), Expressions.newEndTree());
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
			inner = newNode(localName, inner);

			Production target = getGrammar().getProduction(localName);
			if (target != null) {
				inner = Expressions.newChoice(target.getExpression(), inner);
			}

			getGrammar().addProduction(localName, inner);
			return Expressions.newLinkTree(Expressions.newNonTerminal(node, getGrammar(), localName));
		}
	}

	public class _Preamble extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			return Expressions.newLinkTree(Expressions.newNonTerminal(node, getGrammar(), node.getText(_name, null)));
		}
	}

	public class _Annotation extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			String localName = node.getText(_name, null);

			// generate an expression such as (!(next delim) .)*
			UList<Expression> repeatedExprList = new UList<>(new Expression[2]);
			assert (next.is(_Delim));
			repeatedExprList.add(Expressions.newNot(Expressions.newExpression(node, Character.toString(next.toText().charAt(0)))));
			repeatedExprList.add(Expressions.newAny(node));
			Expression repeatedExpr = Expressions.newSequence(repeatedExprList);
			Expression inner = Expressions.newZeroMore(repeatedExpr);

			inner = newNode(localName, inner);

			Production target = getGrammar().getProduction(localName);
			if (target != null) {
				inner = Expressions.newChoice(target.getExpression(), inner);
			}

			getGrammar().addProduction(localName, inner);
			return Expressions.newLinkTree(Expressions.newNonTerminal(node, getGrammar(), localName));
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
			// inferEngine.infer(node.toText());
			Expression expr = chunker.chunk(node.toText());
			return Expressions.newLinkTree(newNode("Token", expr));
		}
	}

	public class _Delim extends TreeVisitor {
		@Override
		public Expression accept(Tree<?> prev, Tree<?> node, Tree<?> next) {
			Expression expr = Expressions.newExpression(node, node.toText());
			return Expressions.newLinkTree(newNode("Delim", expr));
		}
	}

}
