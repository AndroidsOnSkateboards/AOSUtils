package org.aosutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class HashMapList<T, U> {
	private HashMap<T, ArrayList<U>> values;
	
	public HashMapList() {
		values = new HashMap<T, ArrayList<U>>();
	}
	
	public void add(T key, U value) {
		ArrayList<U> valuesForKey = values.get(key);
		if (valuesForKey == null) {
			valuesForKey = new ArrayList<U>();
			values.put(key, valuesForKey);
		}
		valuesForKey.add(value);
	}
	public void addAll(T key, Collection<U> values) {
		ArrayList<U> valuesForKey = this.values.get(key);
		if (valuesForKey == null) {
			valuesForKey = new ArrayList<U>();
			this.values.put(key, valuesForKey);
		}
		valuesForKey.addAll(values);
	}
	
	public ArrayList<U> get(T key) {
		return values.get(key);
	}
	
	public Set<T> keySet() {
		return values.keySet();
	}
	
	public boolean containsKey(Object key) {
		return values.containsKey(key);
	}
}
