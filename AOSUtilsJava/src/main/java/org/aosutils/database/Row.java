package org.aosutils.database;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.serial.SerialBlob;

import org.aosutils.StringUtils;
import org.aosutils.database.Clause.AbstractClause;
import org.aosutils.database.Clause.GreaterThan;
import org.aosutils.database.Clause.GreaterThanOrEqualTo;
import org.aosutils.database.Clause.LessThan;
import org.aosutils.database.Clause.LessThanOrEqualTo;
import org.aosutils.database.Clause.NotEqualTo;

public abstract class Row {
	private LinkedHashMap<Column, Field> fields;
	private String writeProtectMessage;
	private boolean shouldDelete;
	
	public abstract String getTableName();
	protected abstract LinkedHashMap<Column, Field> createFields();
	protected abstract Row mapRow(ResultSet rs) throws SQLException;
	public abstract void processGeneratedKeysOnInsert(ResultSet generatedKeys) throws SQLException;
	
	public enum SortOrder { ASC, DESC }
	private enum Condition { EQUAL, NOT_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL }
	
	public Row() {
		this.fields = new LinkedHashMap<Column, Field>();
		this.fields.putAll(createFields());
		this.shouldDelete = false;
	}
	
	protected Map<Column, Field> getFields() {
		return Collections.unmodifiableMap(fields);
	}
	protected Field getField(Column key) {
		return fields.get(key);
	}
	
	public void setDelete(boolean shouldDelete) {
		this.shouldDelete = shouldDelete;
	}
	
	protected List<? extends Row> select(Map<Column, Object> criteria, Connection dbConnection) throws SQLException {
		return select(null, criteria, null, null, null, dbConnection);
	}
	
	protected List<? extends Row> select(String optionalTableName, Map<Column, Object> criteria, 
			ArrayList<Column> groupBy, LinkedHashMap<Column, SortOrder> orderBy, 
			Integer limit, Connection dbConnection) throws SQLException {
		
		createTableIfNotExists(dbConnection);
		
		ArrayList<String> criteriaSqlParts = new ArrayList<String>();
		ArrayList<Column> criteriaColumns = new ArrayList<Column>();
		ArrayList<Object> criteriaValues = new ArrayList<Object>();
		
		if (criteria != null) {
			for (Column column : criteria.keySet()) {
				Object value = criteria.get(column);
				
				Condition condition = 
					value instanceof LessThan ? Condition.LESS_THAN :
					value instanceof LessThanOrEqualTo ? Condition.LESS_THAN_OR_EQUAL :
					value instanceof GreaterThan ? Condition.GREATER_THAN :
					value instanceof GreaterThanOrEqualTo ? Condition.GREATER_THAN_OR_EQUAL :
					value instanceof NotEqualTo ? Condition.NOT_EQUAL :
					value instanceof AbstractClause ? null :
						Condition.EQUAL;
				
				if (condition == null) {
					throw new SQLException("Unrecognized Clause: " + value);
				}
				else if (value instanceof AbstractClause) { // Resolved clause
					value = ((AbstractClause) value).getObject();
				}
				
				if (value instanceof AbstractCollection<?>) {
					AbstractCollection<?> collectionValues = (AbstractCollection<?>) value;
					
					if (collectionValues.size() == 0) {
						// Instead of query error (where x in () ), just return empty ResultSet
						return new ArrayList<Row>();
					}
					else {
						ArrayList<String> criteriaCollectionParts = new ArrayList<String>();
						
						for (Object valuePart : collectionValues) {
							criteriaCollectionParts.add("?");
							criteriaColumns.add(column);
							criteriaValues.add(valuePart);
							
							//System.out.println("? " + valuePart);
						}
						
						if (!(condition.equals(Condition.EQUAL) || condition.equals(Condition.NOT_EQUAL))) {
							throw new SQLException("Only EQUAL/NOTEQUAL are valid for a collection of values");
						}
						
						String inNotIn = condition.equals(Condition.EQUAL) ? "IN" : "NOT IN";
						criteriaSqlParts.add(String.format("`%s` %s (%s)", column.getColumnName(), inNotIn, StringUtils.join(criteriaCollectionParts, ", ")));
					}
				}
				else { // Single value
					if (value == null) {
						if (!(condition.equals(Condition.EQUAL) || condition.equals(Condition.NOT_EQUAL))) {
							throw new SQLException("Only EQUAL/NOTEQUAL are valid for a value of NULL");
						}
						
						String isIsNot = condition.equals(Condition.EQUAL) ? "IS" : "IS NOT";
						criteriaSqlParts.add(String.format("`%s` %s NULL", column.getColumnName(), isIsNot));
					}
					else {
						String conditionStr = condition.equals(Condition.EQUAL) ? "=" :
							condition.equals(Condition.NOT_EQUAL) ? "!=" :
							condition.equals(Condition.LESS_THAN) ? "<" :
							condition.equals(Condition.LESS_THAN_OR_EQUAL) ? "<=" :
							condition.equals(Condition.GREATER_THAN) ? ">" :
							condition.equals(Condition.GREATER_THAN_OR_EQUAL) ? ">=" :
							null;
						
						if (conditionStr == null) {
							throw new SQLException("Unknown condition type: " + condition);
						}
						
						if (value instanceof Column) {
							criteriaSqlParts.add(String.format("`%s` %s `%s`", column.getColumnName(), conditionStr, ((Column) value).getColumnName() ));
						}
						else {
							criteriaSqlParts.add(String.format("`%s` %s ?", column.getColumnName(), conditionStr));
							criteriaColumns.add(column);
							criteriaValues.add(value);
						}
					}
				}
			}
		}
		
		String tableName = optionalTableName != null ? optionalTableName : getTableName();
		String sql = String.format("SELECT * FROM `%s`", tableName);
		if (criteriaSqlParts.size() > 0) {
			sql +=  String.format(" WHERE %s", StringUtils.join(criteriaSqlParts, " AND "));
		}
		
		if (groupBy != null && groupBy.size() > 0) {
			ArrayList<String> groupByColumnNames = new ArrayList<String>();
			for (Column column : groupBy) {
				groupByColumnNames.add(String.format("`%s`.`%s`", tableName, column.getColumnName()));
			}
			sql += " GROUP BY " + StringUtils.join(groupByColumnNames, ", ");
		}
		
		if (orderBy != null && orderBy.size() > 0) {
			ArrayList<String> orderParts = new ArrayList<String>();
			for (Column column : orderBy.keySet()) {
				SortOrder sortOrder = orderBy.get(column);
				orderParts.add(String.format("`%s`.`%s` %s", tableName, column.getColumnName(), sortOrder));
			}
			sql += " ORDER BY " + StringUtils.join(orderParts, ", ");
		}
		
		if (limit != null) {
			sql += " LIMIT " + limit;
		}
		
		PreparedStatement preparedStatement = dbConnection.prepareStatement(sql);
		try {
			ResultSet rs = executeQuery(sql, criteriaColumns, criteriaValues, preparedStatement, dbConnection);
			
			ArrayList<Row> rows = new ArrayList<Row>();
			while (rs.next()) {
				Row row = mapRow(rs);
				rows.add(row);
			}
			
			rs.close();
			return rows;
		}
		finally {
			preparedStatement.close();
		}
	}
	
	protected Long getLongOrNull(ResultSet rs, String columnName) throws SQLException {
		Long result = rs.getLong(columnName);
		return rs.wasNull() ? null : result;
	}
	
	protected Double getDoubleOrNull(ResultSet rs, String columnName) throws SQLException {
		Double result = rs.getDouble(columnName);
		return rs.wasNull() ? null : result;
	}
	
	protected boolean nullSafeEquals(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}
		else if (a == null && b != null) {
			return false;
		}
		else if (a != null && b == null) {
			return false;
		}
		else {
			return a.equals(b);
		}
	}
	
	public void writeProtect(String reason) {
		this.writeProtectMessage = reason;
	}
	
	public void store(Connection dbConnection) throws SQLException {
		if (this.writeProtectMessage != null) {
			throw new SQLException("Object is write-protected. Reason: " + this.writeProtectMessage);
		}
		
		createTableIfNotExists(dbConnection);
		
		if (shouldDelete) {
			deleteRow(dbConnection);
		}
		else {
			insertOrUpdate(dbConnection);
		}
	}
	
	private void deleteRow(Connection dbConnection) throws SQLException {
		List<Column> primaryKeys = new ArrayList<Column>();
		for (Column column : this.getFields().keySet()) {
			if (column.isPrimaryKey()) {
				primaryKeys.add(column);
			}
		}
		
		if (primaryKeys.size() == 0) {
			throw new SQLException(String.format("Cannot drop a row from table \"%s\" because it has no primary keys!", getTableName()));
		}
		
		ArrayList<String> conditions = new ArrayList<String>();
		ArrayList<Column> criteriaColumns = new ArrayList<Column>();
		ArrayList<Object> criteriaValues = new ArrayList<Object>();
				
		for (Column primaryKey : primaryKeys) {
			conditions.add(String.format("'%s' = ?", primaryKey.getColumnName()));
			criteriaValues.add(getField(primaryKey).getValue());
		}
		
		String sql = String.format("DELETE FROM `%s` WHERE %s", getTableName(), StringUtils.join(conditions, " AND "));
		
		PreparedStatement preparedStatement = dbConnection.prepareStatement(sql);
		try {
			executeQuery(sql, criteriaColumns, criteriaValues, preparedStatement, dbConnection);
		}
		finally {
			preparedStatement.close();
		}
	}
	
	private void insertOrUpdate(Connection dbConnection) throws SQLException {
		ArrayList<Column> insertColumns = new ArrayList<Column>();
		ArrayList<String> insertColumnNames = new ArrayList<String>();
		
		ArrayList<Object> insertValues = new ArrayList<Object>();
		ArrayList<String> insertValuePlaceholders = new ArrayList<String>();
		ArrayList<String> updateParts = new ArrayList<String>();
		
		for (Field field : getFields().values()) {
			Column column = field.getColumn();
			
			if (!column.isPrimaryKey()) {
				updateParts.add(String.format("`%s`=VALUES(`%s`)", column.getColumnName(), column.getColumnName()));
			}
			
			if (!column.isPrimaryKey() || field.getValue() != null) {
				insertColumns.add(field.getColumn());
				insertColumnNames.add(column.getColumnName());
				insertValues.add(field.getValue());
				insertValuePlaceholders.add("?");
			}
		}
		
		String sql = String.format("INSERT INTO `%s` (%s) VALUES(%s) ON DUPLICATE KEY UPDATE %s",
				getTableName(),
				StringUtils.join(insertColumnNames, ", ", "`"),
				StringUtils.join(insertValuePlaceholders, ", "),
				StringUtils.join(updateParts, ", ")
		);
		
		PreparedStatement preparedStatement = dbConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		try {
			executeQuery(sql, insertColumns, insertValues, preparedStatement, dbConnection);
			processGeneratedKeysOnInsert(preparedStatement.getGeneratedKeys());
		}
		finally {
			preparedStatement.close();
		}
	}
	
	public void createTableIfNotExists(Connection dbConnection) throws SQLException {
		if (!_DatabaseSingleton.getInstance().isInitialized(this.getClass())) {
			// Check if table exists -- This is done even though "IF NOT EXISTS" clause is used, because the CREATE query will commit any transactions
			String query = String.format("SHOW TABLES LIKE '%s'", getTableName());
			Statement statement = dbConnection.createStatement();
			
			ResultSet rs = dbConnection.createStatement().executeQuery(query);
			boolean tableExists = rs.next();
			statement.close();
			
			if (!tableExists) {
				if (dbConnection.getAutoCommit() == false) {
					throw new SQLException(String.format("Table `%s` is being created in middle of a TRANSACTION. Please make sure your code explicitly creates all necessary tables for the transaction, first.", getTableName()));
				}
				
				ArrayList<String> createTableParts = new ArrayList<String>();
				ArrayList<String> primaryKeys = new ArrayList<String>();
				ArrayList<String> uniqueColumns = new ArrayList<String>();
				
				for (Field field : getFields().values()) {
					Column column = field.getColumn();
					
					createTableParts.add(String.format("`%s` %s", column.getColumnName(), column.getTypeStr()));
					
					if (column.isPrimaryKey()) {
						primaryKeys.add(column.getColumnName());
					}
					
					if (column.isUnique()) {
						uniqueColumns.add(column.getColumnName());
					}
				}
				
				if (primaryKeys.size() > 0) {
					createTableParts.add(String.format("PRIMARY KEY (%s)", StringUtils.join(primaryKeys, ", ", "`")));
				}
				
				if (uniqueColumns.size() > 0) {
					createTableParts.add(String.format("UNIQUE INDEX `unique` (%s)", StringUtils.join(uniqueColumns, ", ", "`")));
				}
				
				String sql = String.format("CREATE TABLE IF NOT EXISTS `%s` (%s)", getTableName(), StringUtils.join(createTableParts, ", "));
				
				//System.out.println("SQL: " + sql);
				
				PreparedStatement preparedStatement = (PreparedStatement) dbConnection.prepareStatement(sql);
				try {
					preparedStatement.executeUpdate();
				}
				catch (SQLException e) {
					System.err.println("SQL: " + sql);
					throw e;
				}
				finally {
					preparedStatement.close();
				}
			}
			
			_DatabaseSingleton.getInstance().setInitialized(this.getClass());
		}
	}
	
	private ResultSet executeQuery(String sql, ArrayList<Column> columns, ArrayList<Object> values, PreparedStatement preparedStatement, Connection dbConnection) throws SQLException {
		if (columns.size() != values.size()) {
			throw new SQLException("executeQuery() must be called with an equal number of columns and values");
		}
		
		for (int i=0; i<values.size(); i++) {
			int parameterIndex = i+1;
			Object value = values.get(i);
			int columnType = columns.get(i).getType();
			
			if (value == null) {
				preparedStatement.setNull(parameterIndex, columnType);
			}
			else if (value instanceof String) {
				preparedStatement.setString(parameterIndex, (String) value);
			}
			else if (value instanceof Integer) {
				preparedStatement.setInt(parameterIndex, (Integer) value);
			}
			else if (value instanceof Long) {
				preparedStatement.setLong(parameterIndex, (Long) value);
			}
			else if (value instanceof Double) {
				preparedStatement.setDouble(parameterIndex, (Double) value);
			}
			else if (value instanceof Float) {
				preparedStatement.setFloat(parameterIndex, (Float) value);
			}
			else if (value instanceof Boolean) {
				preparedStatement.setBoolean(parameterIndex, (Boolean) value);
			}
			else if (value instanceof Date) {
				long time = ((Date) value).getTime();
				preparedStatement.setTimestamp(parameterIndex, new Timestamp(time));
			}
			else if (value instanceof Enum) {
				preparedStatement.setString(parameterIndex, ""+value);
			}
			else if (value instanceof byte[] || value instanceof Blob) {
				Blob blob = value instanceof Blob ? (Blob) value : new SerialBlob((byte[]) value);
				preparedStatement.setBlob(parameterIndex, blob);
			}
			else {
				throw new SQLException("Unknown DB field type for: " + value.getClass().getName() + ", please map it correctly in " + Row.class.getName());
			}
		}
		
		//System.out.println("SQL: " + sql);
		
		try {
			preparedStatement.execute();
		}
		catch (SQLException e) {
			System.err.println("SQL: " + sql);
			throw e;
		}
		
		ResultSet results = preparedStatement.getResultSet();
		return results;
	}
}
