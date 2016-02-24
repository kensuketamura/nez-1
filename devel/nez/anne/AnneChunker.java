package nez.anne;

import java.io.IOException;

import nez.ParserGenerator;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.util.UList;

public class AnneChunker {
	private Parser parser;
	private Grammar basedGrammar;
	public static String grammarFilePath = "mytest/inference_log.nez";

	private static Symbol _List = Symbol.unique("List");
	private static Symbol _Chunk = Symbol.unique("Chunk");
	private static Symbol _Sequence = Symbol.unique("Sequence");

	public AnneChunker(Grammar basedGrammar) {
		ParserGenerator pg = new ParserGenerator();
		try {
			this.parser = pg.newParser(grammarFilePath, ParserStrategy.newSafeStrategy());
			this.basedGrammar = basedGrammar;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Expression chunk(String context) {
		Tree<?> node = parser.parse(context);
		return generateExpr(node);
	}

	public void appendChunkingGrammar(Grammar base) {
		ParserGenerator pg = new ParserGenerator();
		try {
			Grammar g = pg.loadGrammar(grammarFilePath);
			for (Production p : g) {
				this.basedGrammar.addProduction(p.getLocalName(), p.getExpression());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Expression generateExpr(Tree<?> node) {
		assert (node.is(_List) && node.has(_Chunk) && node.get(0).has(_Sequence));
		Tree<?> sequence = node.get(0).get(0);
		UList<Expression> list = new UList<>(new Expression[sequence.size()]);
		for (Tree<?> element : sequence) {
			list.add(Expressions.newNonTerminal(basedGrammar, element.getTag().toString()));
		}
		return Expressions.newSequence(list);
	}
}
