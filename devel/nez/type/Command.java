package nez.type;

import java.io.IOException;

import nez.lang.Grammar;

public class Command extends nez.main.Command {
	@Override
	public void exec() throws IOException {
		/* Setting requird options */
		strategy.Optimization = false;
		Grammar grammar = this.newGrammar();
		new TypingVisitor().typing(grammar);
	}

}
