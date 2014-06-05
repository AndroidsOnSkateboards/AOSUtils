package org.aosutils.database;

public class Field {
	private Column column;
	private Object value;
	
	public Field(Column column) {
		this.column = column;
	}
	
	public Column getColumn() {
		return column;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	
}
