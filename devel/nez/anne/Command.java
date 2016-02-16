package nez.anne;

import java.io.IOException;

import nez.ast.Source;

public class Command extends nez.main.Command {
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
