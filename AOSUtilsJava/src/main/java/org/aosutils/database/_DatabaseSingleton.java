package org.aosutils.database;

import java.util.ArrayList;
import java.util.List;

public class _DatabaseSingleton {
	private static _DatabaseSingleton _this;
	
	List<Class<?>> initializedTables;
	
	protected static _DatabaseSingleton getInstance() {
		if (_this == null) {
			_this = new _DatabaseSingleton();
			_this.initializedTables = new ArrayList<Class<?>>();
		}
		return _this;
	}
	
	protected boolean isInitialized(Class<?> row) {
		return initializedTables.contains(row);
	}
	protected void setInitialized(Class<?> row) {
		initializedTables.add(row);
	}
}
