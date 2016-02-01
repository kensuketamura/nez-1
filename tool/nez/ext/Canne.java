package nez.ext;

import java.io.IOException;

import nez.Parser;
import nez.main.Command;
import nez.main.CommandContext;

public class Canne extends Command {

	@Override
	public void exec(CommandContext config) throws IOException {
		Parser parser = config.newParser();
		// while (config.hasInput()) {
		// SourceContext input = config.nextInput();
		// Tree<?> node = parser.parseCommonTree(input);
		// if (node == null) {
		// ConsoleUtils.println(input.getSyntaxErrorMessage());
		// continue;
		// } else if (input.hasUnconsumed()) {
		// ConsoleUtils.println(input.getUnconsumedMessage());
		// } else {
		// Ganne ganne = new Ganne();
		// ganne.parse(node);
		// }
		// }
	}

}
