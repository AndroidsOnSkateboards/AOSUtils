package org.aosutils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class LocalDataManager {
	private static LocalDataManager _localDataManager;
	
	private Properties properties;
	
	private LocalDataManager() {
		super();
	}
	public LocalDataManager getInstance() {
		if (_localDataManager == null) {
			_localDataManager = new LocalDataManager();
			_localDataManager.reloadData();
		}
		
		return _localDataManager;
	}
	
	public String getProperty(String key) {	
		String value = properties.getProperty(key);
		if (value == null) {
			System.err.println("Property \"" + key + "\" not found!");
		}
		return properties.getProperty(key);
	}
	
	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}
	public void setAndStoreProperty(String key, String value) throws FileNotFoundException, IOException {
		setProperty(key, value);
		store();
	}
	
	private void reloadData() {
		_localDataManager.properties = new Properties();
		try {
			properties.load(new FileInputStream("local-data.properties"));
		} catch (Exception e) {
			
		}
	}
	
	public void store() throws FileNotFoundException, IOException {
		properties.store(new FileOutputStream("local-data.properties"), null);
	}
}
