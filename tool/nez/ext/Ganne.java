package nez.ext;

import java.io.IOException;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.infer.InferenceEngine;
import nez.lang.Expression;
import nez.lang.GrammarFileLoader;
import nez.lang.Production;
import nez.lang.expr.ExpressionCommons;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class Ganne extends GrammarFileLoader {
	public Ganne() {
		init(Ganne.class, new Undefined());
		infer = new InferenceEngine();
	}

	public class Undefined extends DefaultVisitor {
		@Override
		public Expression toExpression(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in ANNE " + node));
			return null;
		}

	}

	static Parser anneParser;
	static InferenceEngine infer;

	@Override
	public Parser getLoaderParser(String start) {
		if (anneParser == null) {
			try {
				Strategy option = Strategy.newSafeStrategy();
				Grammar g = GrammarFileLoader.loadGrammar("anne.nez", option);
				anneParser = g.newParser(option);
				strategy.report();
			} catch (IOException e) {
				ConsoleUtils.exit(1, "unload: " + e.getMessage());
			}
			assert (anneParser != null);
		}
		return anneParser;
	}

	public final static Symbol _Name = Symbol.tag("name");
	public final static Symbol _Content = Symbol.tag("content");
	public final static Symbol _Token = Symbol.tag("token");
	public final static Symbol _List = Symbol.tag("list");
	public final static Symbol _Delim = Symbol.tag("delim");

	@Override
	public void parse(Tree<?> node) {
		this.loadPredefinedProduction();
		for (Tree<?> nonterminal : node) {
			visit(nonterminal);
		}
		getGrammar().dump();
	}

	private final void loadPredefinedProduction() {
		GrammarFileLoader fl = new Gnez();
		try {
			Grammar infergGrammar = GrammarFileLoader.loadGrammarFile("log_pre3.nez", Strategy.newSafeStrategy());
			for (Production production : infergGrammar.getProductionList()) {
				getGrammar().addProduction(production);
			}
			// fl.load(getGrammar(), "log_pre3.nez",
			// Strategy.newSafeStrategy());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final Expression visit(Tree<?> node) {
		return find(node.getTag().toString()).toExpression(node);
	}

	public class _Nonterminal extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			String localName = node.getText(_Name, "");
			UList<Expression> l = new UList<Expression>(new Expression[5]);
			for (Tree<?> content : node.get(_Content)) {
				l.add(visit(content));
			}
			Expression inner = ExpressionCommons.newPsequence(null, l);
			getGrammarFile().addProduction(null, localName, inner);
			return inner;
		}
	}

	public class _Preamble extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			String localName = node.getText(_Name, "");
			UList<Expression> l = new UList<Expression>(new Expression[5]);
			for (Tree<?> content : node.get(_Content)) {
				l.add(visit(content));
			}
			Expression inner = ExpressionCommons.newPsequence(null, l);
			getGrammarFile().addProduction(null, localName, inner);
			return ExpressionCommons.newNonTerminal(null, getGrammar(), localName);
		}
	}

	public class _Annotation extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			String localName = node.getText(_Name, "");
			UList<Expression> l = new UList<Expression>(new Expression[5]);
			for (Tree<?> content : node.get(_Content)) {
				l.add(visit(content));
			}
			Expression inner = ExpressionCommons.newPsequence(null, l);
			getGrammarFile().addProduction(null, localName, inner);
			return ExpressionCommons.newNonTerminal(null, getGrammar(), localName);
		}
	}

	public class _Alternative extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			String localName = node.getText(_Name, "");
			UList<Expression> l = new UList<Expression>(new Expression[5]);
			for (Tree<?> content : node.get(_Content)) {
				l.add(visit(content));
			}
			Expression inner = ExpressionCommons.newPsequence(null, l);
			getGrammarFile().addProduction(null, localName, inner);
			return ExpressionCommons.newNonTerminal(null, getGrammar(), localName);
		}
	}

	public class _Repetition extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			String localName = node.getText(_Name, "");
			UList<Expression> l = new UList<Expression>(new Expression[5]);
			for (Tree<?> content : node.get(_Content)) {
				l.add(visit(content));
			}
			Expression inner = ExpressionCommons.newPsequence(null, l);
			getGrammarFile().addProduction(null, localName, inner);
			return ExpressionCommons.newNonTerminal(null, getGrammar(), localName);
		}
	}

	public class _Option extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			String localName = node.getText(_Name, "");
			UList<Expression> l = new UList<Expression>(new Expression[5]);
			for (Tree<?> content : node.get(_Content)) {
				l.add(visit(content));
			}
			Expression inner = ExpressionCommons.newPsequence(null, l);
			getGrammarFile().addProduction(null, localName, inner);
			return ExpressionCommons.newNonTerminal(null, getGrammar(), localName);
		}
	}

	public class _Constant extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			String localName = node.getText(_Name, "");
			UList<Expression> l = new UList<Expression>(new Expression[5]);
			for (Tree<?> content : node.get(_Content)) {
				l.add(visit(content));
			}
			Expression inner = ExpressionCommons.newPsequence(null, l);
			getGrammarFile().addProduction(null, localName, inner);
			return ExpressionCommons.newNonTerminal(null, getGrammar(), localName);
		}
	}

	public class _Enumeration extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			String localName = node.getText(_Name, "");
			UList<Expression> l = new UList<Expression>(new Expression[5]);
			for (Tree<?> content : node.get(_Content)) {
				l.add(visit(content));
			}
			Expression inner = ExpressionCommons.newPsequence(null, l);
			getGrammarFile().addProduction(null, localName, inner);
			return ExpressionCommons.newNonTerminal(null, getGrammar(), localName);
		}
	}

	public class _Table extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			String localName = node.getText(_Name, "");
			UList<Expression> l = new UList<Expression>(new Expression[5]);
			for (Tree<?> content : node.get(_Content)) {
				l.add(visit(content));
			}
			Expression inner = ExpressionCommons.newPsequence(null, l);
			getGrammarFile().addProduction(null, localName, inner);
			return ExpressionCommons.newNonTerminal(null, getGrammar(), localName);

		}
	}

	public class _Assertion extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			String localName = node.getText(_Name, "");
			UList<Expression> l = new UList<Expression>(new Expression[5]);
			for (Tree<?> content : node.get(_Content)) {
				l.add(visit(content));
			}
			Expression inner = ExpressionCommons.newPsequence(null, l);
			getGrammarFile().addProduction(null, localName, inner);
			return ExpressionCommons.newNonTerminal(null, getGrammar(), localName);
		}
	}

	public class _Name extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			Expression inner = ExpressionCommons.newString(null, node.toText());
			return inner;
		}
	}

	public class _Token extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			try {
				Grammar inferGrammar = infer.inferString(node.toText());
				Grammar preGrammar = GrammarFileLoader.loadGrammarFile("log_pre3.nez", Strategy.newSafeStrategy());
				for (Production production : inferGrammar.getProductionList()) {
					getGrammar().addProduction(production);
				}
				Expression inferExpression = inferGrammar.get(0).getExpression();
				String inferString = inferGrammar.getProductionList().get(0).getLocalName();
				for (int i = 0; i < preGrammar.size(); i++) {
					String preString = preGrammar.getProductionList().get(i).getLocalName();
					if (inferExpression.toString().equals(preString)) {
						inferGrammar = preGrammar;
						break;
					}
				}

				Expression inner = ExpressionCommons.newNonTerminal(node, inferGrammar, inferString);
				return inner;

			} catch (IOException e) {
				// TODO Ž©“®¶¬‚³‚ê‚½ catch ƒuƒƒbƒN
				e.printStackTrace();
			}
			return null;
		}
	}

	public class _Delim extends Undefined {
		@Override
		public Expression toExpression(Tree<?> node) {
			Expression inner = ExpressionCommons.newString(null, node.toText());
			return inner;
		}
	}
}
