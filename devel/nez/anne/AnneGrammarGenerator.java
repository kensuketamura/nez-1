package nez.anne;

import java.io.IOException;

import nez.ParserGenerator;
import nez.ast.Source;
import nez.ast.Tree;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.ParserStrategy;

public class AnneGrammarGenerator {
	private final static ParserStrategy strategy = ParserStrategy.newSafeStrategy();
	private final static String anneGrammarFilePath = "anne.nez";
	private Parser anneParser;
	public Grammar grammar;

	public AnneGrammarGenerator() {
		this.anneParser = newParser();
	}

	private final Grammar loadAnneGrammar() throws IOException {
		ParserGenerator pg = new ParserGenerator();
		Grammar grammar = pg.loadGrammar(anneGrammarFilePath);
		return grammar;
	}

	private final Parser newParser() {
		try {
			Parser parser = strategy.newParser(loadAnneGrammar());
			return parser;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Grammar generate(Source sc, String predefGrammarFilePath) {
		try {
			Tree<?> node = anneParser.parse(sc);
			loadPredefineGrammar(predefGrammarFilePath);
			AnneExpressionConstructor constructor = new AnneExpressionConstructor(grammar, strategy);
			constructor.load(node);
			constructor.getGrammar().dump();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Grammar getGrammar() {
		return this.grammar;
	}

	private void loadPredefineGrammar(String filePath) throws IOException {
		ParserGenerator pg = new ParserGenerator();
		if (filePath != null) {
			this.grammar = pg.loadGrammar(filePath);
		}
		this.grammar = new Grammar();
	}
}
