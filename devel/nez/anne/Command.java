package nez.anne;

import java.io.IOException;

import nez.ast.Source;

public class Command extends nez.main.Command {
	@Override
	public void exec() throws IOException {
		checkInputSource();
		AnneGrammarGenerator generator = new AnneGrammarGenerator();
		if (hasInputSource()) {
			Source anne = nextInputSource();
			Source data = null;
			if (hasInputSource()) {
				data = nextInputSource();
			}
			generator.generate(anne, data, grammarFile);
		}
	}
}
