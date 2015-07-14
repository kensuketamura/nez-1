package nez.checker.js;

import nez.checker.ModifiableTree;
import nez.checker.ParsingWriter;
import nez.checker.SourceGenerator;

public class FSharpWriter extends ParsingWriter {
	
	static {
		ParsingWriter.registerExtension("fs", FSharpWriter.class);
	}
	
	@Override
	protected void write(ModifiableTree po) {
		SourceGenerator generator = new FSharpGenerator();
		JSRewriter rewriter = new JSRewriter();
		String fixedSource = rewriter.rewrite(po);
		generator.visit(po);
		this.out.println(generator.toString());
	}

}
