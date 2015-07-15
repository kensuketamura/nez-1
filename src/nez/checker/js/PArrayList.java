package nez.checker.js;

import java.util.ArrayList;

public class PArrayList<E> extends ArrayList<E> {
	public String toString(){
		StringBuilder builder = new StringBuilder();
		//builder.append("[");
		for(int i = 0; i < this.size(); i++){
			builder.append(((JSData) this.get(i)).toStringForArray());
			if(i != this.size() - 1){
				builder.append(".");
			}
		}
		//builder.append("]");
		return builder.toString();
	}
}
