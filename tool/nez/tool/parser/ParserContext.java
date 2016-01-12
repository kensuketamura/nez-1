package nez.tool.parser;

import nez.ast.CommonTree;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.io.StringSource;

public class ParserContext {
	public int pos = 0;
	public Tree<?> left;

	public ParserContext(String s) {
		source = new StringSource(s);
		inputs = source.inputs;
		length = inputs.length - 1;
		this.pos = 0;
		this.left = new CommonTree();
	}

	private StringSource source;
	private byte[] inputs;
	private int length;

	public final boolean eof() {
		return !(pos < length);
	}

	public final int read() {
		return inputs[pos++] & 0xff;
	}

	public final int prefetch() {
		return inputs[pos] & 0xff;
	}

	public final void move(int shift) {
		pos += shift;
	}

	public final boolean match(byte[] text, int len) {
		if (pos + len > this.length) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if (text[i] != this.inputs[pos + i]) {
				return false;
			}
		}
		return true;
	}

	// @Override
	// public final byte[] subByte(long startIndex, long endIndex) {
	// byte[] b = new byte[(int) (endIndex - startIndex)];
	// System.arraycopy(this.inputs, (int) (startIndex), b, 0, b.length);
	// return b;
	// }
	//
	// @Override
	// public final String subString(long startIndex, long endIndex) {
	// try {
	// return new String(this.inputs, (int) (startIndex), (int) (endIndex -
	// startIndex), StringUtils.DefaultEncoding);
	// } catch (UnsupportedEncodingException e) {
	// }
	// return null;
	// }

	// AST

	private enum Operation {
		Link, Tag, Replace, New;
	}

	static class AstLog {
		Operation op;
		// int debugId;
		int pos;
		Symbol label;
		Object value;
		AstLog prev;
		AstLog next;
	}

	private AstLog last = new AstLog();
	private AstLog unused = null;

	private void log(Operation op, int pos, Symbol label, Object value) {
		AstLog l;
		if (this.unused == null) {
			l = new AstLog();
		} else {
			l = this.unused;
			this.unused = l.next;
		}
		// l.debugId = last.debugId + 1;
		l.op = op;
		l.pos = pos;
		l.label = label;
		l.value = value;
		l.prev = last;
		l.next = null;
		last.next = l;
		last = l;
	}

	public final void beginTree(int shift) {
		log(Operation.New, pos + shift, null, null);
	}

	public final void linkTree(Tree<?> parent, Symbol label) {
		log(Operation.Link, 0, label, left);
	}

	public final void tagTree(Symbol tag) {
		log(Operation.Tag, 0, null, tag);
	}

	public final void valueTree(String value) {
		log(Operation.Replace, 0, null, value);
	}

	public final void foldTree(int shift, Symbol label) {
		log(Operation.New, pos + shift, null, null);
		log(Operation.Link, 0, label, left);
	}

	public final void endTree(Symbol tag, String value, int shift) {
		int objectSize = 0;
		AstLog start;
		for (start = last; start.op != Operation.New; start = start.prev) {
			switch (start.op) {
			case Link:
				objectSize++;
				break;
			case Tag:
				if (tag == null) {
					tag = (Symbol) start.value;
				}
				break;
			case Replace:
				if (value == null) {
					value = (String) start.value;
				}
				break;
			case New:
				break;
			}
		}

		left = left.newInstance(tag, source, start.pos, (pos + shift - start.pos), objectSize, value);
		if (objectSize > 0) {
			int n = 0;
			for (AstLog cur = start; cur != null; cur = cur.next) {
				if (cur.op == Operation.Link) {
					left.link(n++, cur.label, cur.value);
					cur.value = null;
				}
			}
		}
		this.backLog(start.prev);
	}

	public final Object saveLog() {
		return last;
	}

	public final void backLog(Object ref) {
		AstLog save = (AstLog) ref;
		if (save != last) {
			last.next = this.unused;
			this.unused = save.next;
			save.next = null;
			this.last = save;
		}
	}

	// Symbol Table ---------------------------------------------------------

	private final static byte[] NullSymbol = { 0, 0, 0, 0 }; // to distinguish
	// others
	private SymbolTableEntry[] tables;
	private int tableSize = 0;
	private int maxTableSize = 0;

	private int stateValue = 0;
	private int stateCount = 0;

	static final class SymbolTableEntry {
		int stateValue;
		Symbol table;
		long code;
		byte[] symbol; // if uft8 is null, hidden

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			sb.append(stateValue);
			sb.append(", ");
			sb.append(table);
			sb.append(", ");
			sb.append((symbol == null) ? "<masked>" : new String(symbol));
			sb.append("]");
			return sb.toString();
		}
	}

	final static long hash(byte[] utf8) {
		long hashCode = 1;
		for (int i = 0; i < utf8.length; i++) {
			hashCode = hashCode * 31 + (utf8[i] & 0xff);
		}
		return hashCode;
	}

	public static final boolean equalsBytes(byte[] utf8, byte[] b) {
		if (utf8.length == b.length) {
			for (int i = 0; i < utf8.length; i++) {
				if (utf8[i] != b[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private void initEntry(int s, int e) {
		for (int i = s; i < e; i++) {
			this.tables[i] = new SymbolTableEntry();
		}
	}

	private void push(Symbol table, long code, byte[] utf8) {
		if (!(tableSize < maxTableSize)) {
			if (maxTableSize == 0) {
				maxTableSize = 128;
				this.tables = new SymbolTableEntry[128];
				initEntry(0, maxTableSize);
			} else {
				maxTableSize *= 2;
				SymbolTableEntry[] newtable = new SymbolTableEntry[maxTableSize];
				System.arraycopy(this.tables, 0, newtable, 0, tables.length);
				this.tables = newtable;
				initEntry(tables.length / 2, maxTableSize);
			}
		}
		SymbolTableEntry entry = tables[tableSize];
		tableSize++;
		if (entry.table == table && equalsBytes(entry.symbol, utf8)) {
			// reuse state value
			entry.code = code;
			this.stateValue = entry.stateValue;
		} else {
			entry.table = table;
			entry.code = code;
			entry.symbol = utf8;
			this.stateCount += 1;
			this.stateValue = stateCount;
			entry.stateValue = stateCount;
		}
	}

	public final int saveSymbolPoint() {
		return this.tableSize;
	}

	public final void backSymbolPoint(int savePoint) {
		if (this.tableSize != savePoint) {
			this.tableSize = savePoint;
			if (this.tableSize == 0) {
				this.stateValue = 0;
			} else {
				this.stateValue = tables[savePoint - 1].stateValue;
			}
		}
	}

	public final void addSymbol(Symbol table, byte[] utf8) {
		push(table, hash(utf8), utf8);
	}

	public final void addSymbolMask(Symbol table) {
		push(table, 0, NullSymbol);
	}

	public final boolean exists(Symbol table) {
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry entry = tables[i];
			if (entry.table == table) {
				return entry.symbol != NullSymbol;
			}
		}
		return false;
	}

	public final boolean exists(Symbol table, byte[] symbol) {
		long code = hash(symbol);
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry entry = tables[i];
			if (entry.table == table) {
				if (entry.symbol == NullSymbol)
					return false; // masked
				if (entry.code == code && equalsBytes(entry.symbol, symbol)) {
					return true;
				}
			}
		}
		return false;
	}

	public final byte[] getSymbol(Symbol table) {
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry entry = tables[i];
			if (entry.table == table) {
				return entry.symbol;
			}
		}
		return null;
	}

	public final boolean contains(Symbol table, byte[] symbol) {
		long code = hash(symbol);
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry entry = tables[i];
			if (entry.table == table) {
				if (entry.symbol == NullSymbol) {
					return false; // masked
				}
				if (entry.code == code && equalsBytes(entry.symbol, symbol)) {
					return true;
				}
			}
		}
		return false;
	}

	// Memotable ------------------------------------------------------------

	private static class MemoEntry {
		long key = -1;
		public int consumed;
		public Tree<?> result;
		public int stateValue = 0;
	}

	private MemoEntry[] memoArray = null;
	private int shift = 0;

	public void initMemoTable(int w, int n) {
		this.memoArray = new MemoEntry[w * n + 1];
		for (int i = 0; i < this.memoArray.length; i++) {
			this.memoArray[i] = new MemoEntry();
			this.memoArray[i].key = -1;
		}
		this.shift = (int) (Math.log(n) / Math.log(2.0)) + 1;
		// this.initStat();
	}

	final long longkey(long pos, int memoPoint, int shift) {
		return ((pos << shift) | memoPoint) & Long.MAX_VALUE;
	}

	public final int lookupMemo(int memoPoint) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry m = this.memoArray[hash];
		if (m.key == key) {
			this.left = m.result;
			return m.consumed;
		}
		return -1; // unfound
	}

	public void memoSucc(int memoPoint, int consumed) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry m = this.memoArray[hash];
		m.key = key;
		m.result = left;
		m.consumed = consumed;
		m.stateValue = -1;
		// this.CountStored += 1;
	}

	public void memoFail(int memoPoint, int consumed) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry m = this.memoArray[hash];
		m.key = key;
		m.result = null;
		m.consumed = 0;
		m.stateValue = -1;
	}

	public final int lookupStateMemo(int memoPoint) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry m = this.memoArray[hash];
		if (m.key == key) {
			if (m.stateValue == stateValue) {
				this.left = m.result;
				return m.consumed;
			}
		}
		return -1; // unfound
	}

	public void memoStateSucc(int memoPoint, int consumed) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry m = this.memoArray[hash];
		m.key = key;
		m.result = left;
		m.consumed = consumed;
		m.stateValue = stateValue;
		// this.CountStored += 1;
	}

	public void memoStateFail(int memoPoint, int consumed) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry m = this.memoArray[hash];
		m.key = key;
		m.result = null;
		m.consumed = 0;
		m.stateValue = stateValue;
	}

}
