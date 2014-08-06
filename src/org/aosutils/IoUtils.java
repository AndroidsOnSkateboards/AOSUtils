package org.aosutils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class IoUtils {
	public static String readFile(String filename) throws IOException {
		/*
		Path pathObj = FileSystems.getDefault().getPath(filename);
		return new String(Files.readAllBytes(pathObj));
		*/
		
		FileInputStream inputStream = new FileInputStream(filename);
		return getString(inputStream);
	}
	
	public static void writeFile(String filename, String data) throws FileNotFoundException {
		PrintWriter writer = new PrintWriter(filename);
		writer.write(data);
		writer.close();
	}
	
	public static String getString(InputStream inputStream) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		
		StringBuilder stringBuilder = new StringBuilder();
		String line = bufferedReader.readLine();
		while (line != null) {
			stringBuilder.append(line + "\n");
			line = bufferedReader.readLine();
		}
		
		bufferedReader.close();
		
		return stringBuilder.toString();
	}
}
