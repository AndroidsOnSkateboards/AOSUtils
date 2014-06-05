package org.aosutils.database;

import java.sql.Types;

public class Column {
	private String tableName;
	private String columnName;
	private String typeStr;
	private int type;
	private Double length;
	private boolean isPrimaryKey;
	private boolean isUnique;
	
	public Column(String tableName, String columnName, String type) {
		this.tableName = tableName;
		this.columnName = columnName;
		this.typeStr = type;
		if (type.contains("(")) {
			length = Double.parseDouble(type.substring(type.indexOf("(")+1, type.indexOf(")")).replace(",", "."));
		}
		this.type = type.startsWith("INT") ? Types.INTEGER :
			type.startsWith("BIGINT") ? Types.BIGINT :
			type.startsWith("SMALLINT") ? Types.SMALLINT :
			type.startsWith("BOOLEAN") ? Types.BOOLEAN :
			type.startsWith("DOUBLE") ? Types.DOUBLE :
			type.startsWith("DATETIME") ? Types.DATE :
			Types.VARCHAR;
			
	}
	
	public Column(String tableName, String columnName, String type, boolean isPrimaryKey) {
		this(tableName, columnName, type);
		this.isPrimaryKey = isPrimaryKey;
	}
	
	public Column(String tableName, String columnName, String type, boolean isPrimaryKey, boolean isUniqueKey) {
		this(tableName, columnName, type, isPrimaryKey);
		this.isUnique = isUniqueKey;
	}
	
	public String fullName() {
		return String.format("`%s`.`%s`", tableName, columnName);
	}
	
	public String getTableName() {
		return this.tableName;
	}
	
	public String getColumnName() {
		return this.columnName;
	}
	public String getTypeStr() {
		return this.typeStr;
	}
	public int getType() {
		return this.type;
	}
	public Double getLength() {
		return this.length;
	}
	public boolean isPrimaryKey() {
		return this.isPrimaryKey;
	}
	public boolean isUnique() {
		return this.isUnique;
	}
}
