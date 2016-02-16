package nez.anne;

import nez.ast.Symbol;

public interface AnneSymbols {
	public final static Symbol _name = Symbol.unique("name");
	public final static Symbol _content = Symbol.unique("content");
	public final static Symbol _alt = Symbol.unique("alt");

	public final static Symbol _Nonterminal = Symbol.unique("Nonterminal");
	public final static Symbol _Preamble = Symbol.unique("Preamble");
	public final static Symbol _Annotation = Symbol.unique("Annotation");
	public final static Symbol _Name = Symbol.unique("Name");
	public final static Symbol _Token = Symbol.unique("Token");
	public final static Symbol _Alternative = Symbol.unique("Alternative");
	public final static Symbol _Repetition = Symbol.unique("Repetition");
	public final static Symbol _Option = Symbol.unique("Option");
	public final static Symbol _Constant = Symbol.unique("Constant");
	public final static Symbol _Enumeration = Symbol.unique("Enumeration");
	public final static Symbol _Table = Symbol.unique("Table");
	public final static Symbol _Assertion = Symbol.unique("Assertion");
	public final static Symbol _Delim = Symbol.unique("Delim");
}
