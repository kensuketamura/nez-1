package nez.main;

import java.io.IOException;

import nez.anne.AnneGrammarGenerator;
import nez.ast.Source;

public class Canne extends Command {
	@Override
	public void exec() throws IOException {
		checkInputSource();
		AnneGrammarGenerator generator = new AnneGrammarGenerator();
		while (hasInputSource()) {
			Source input = nextInputSource();
			generator.generate(input, grammarFile);
		}
	}
}
