package nez.parser;

import nez.NezOption;
import nez.lang.Production;
import nez.lang.expr.Tlink;
import nez.lang.expr.NonTerminal;
import nez.main.Verbose;

public class PackratCompiler extends OptimizedCompiler {

	public PackratCompiler(NezOption option) {
		super(option);
	}

	protected Instruction encodeMemoizingProduction(ParseFunc cp) {
		// if(cp.memoPoint != null) {
		// Production p = cp.production;
		// //boolean node = option.enabledASTConstruction ?
		// !p.isNoNTreeConstruction() : false;
		// boolean state = p.isContextual();
		// Instruction next = new IMemo(p, cp.memoPoint, state, new IRet(p));
		// Instruction inside = new ICall(cp.production, next);
		// inside = new IAlt(p, new IMemoFail(p, state, cp.memoPoint), inside);
		// return new ILookup(p, cp.memoPoint, state, inside, new IRet(p));
		// }
		return null;
	}

	public final Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production r = p.getProduction();
		ParseFunc pcode = this.getParseFunc(r);
		if (pcode.inlining) {
			this.optimizedInline(r);
			return encode(pcode.e, next, failjump);
		}
		if (pcode.memoPoint != null) {
			if (!option.enabledASTConstruction || r.isNoNTreeConstruction()) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: " + p.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
				}
				Instruction inside = new IMemo(p, pcode.memoPoint, pcode.state, next);
				inside = new ICall(pcode.p, inside);
				inside = new IAlt(p, new IMemoFail(p, pcode.state, pcode.memoPoint), inside);
				return new ILookup(p, pcode.memoPoint, pcode.state, inside, next);
			}
		}
		return new ICall(r, next);
	}

	// AST Construction

	public final Instruction encodeTlink(Tlink p, Instruction next, Instruction failjump) {
		if (option.enabledASTConstruction && p.get(0) instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) p.get(0);
			ParseFunc pcode = this.getParseFunc(n.getProduction());
			if (pcode.memoPoint != null) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: @" + n.getLocalName() + " at " + this.getEncodingProduction().getLocalName());
				}
				Instruction inside = new ITMemo(p, pcode.memoPoint, pcode.state, next);
				inside = new ICommit(p, inside);
				inside = super.encodeNonTerminal(n, inside, failjump);
				inside = new ITStart(p, inside);
				inside = new IAlt(p, new IMemoFail(p, pcode.state, pcode.memoPoint), inside);
				return new ITLookup(p, pcode.memoPoint, pcode.state, inside, next);
			}
		}
		return super.encodeTlink(p, next, failjump);
	}

}