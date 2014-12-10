package org.aosutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
	
	public static void copyFile(String source, String destination) throws IOException {
		FileInputStream inputStream = new FileInputStream(source);
		writeFile(destination, inputStream, null);
	}

	public static void moveFile(String source, String destination) throws IOException {
		new File(source).renameTo(new File(destination));
	}
	
	public static void writeFile(String filename, InputStream inputStream, WriteFileMonitor monitor) throws IOException {
		try {
			if (monitor == null) {
				// To avoid NULL exceptions, but the calling function will not have access to this monitor
				monitor = new WriteFileMonitor();
			}
			
			byte[] buffer = new byte[1024];
			new File(filename).getParentFile().mkdirs();
			OutputStream outputStream = new FileOutputStream(filename);
			
			try {
				int bytesRead;
		        while(!monitor.isCancelled() && (bytesRead = inputStream.read(buffer)) != -1) {
		        	outputStream.write(buffer, 0, bytesRead);
		        	monitor.bytes += bytesRead;
		        }
			}
			finally {
				outputStream.flush();
		        outputStream.close();
			}
		}
		finally {
			inputStream.close();
		}
        
        if (monitor.isCancelled) {
        	new File(filename).delete();
        }
	}
	
	public static class WriteFileMonitor {
		private int bytes = 0;
		private boolean isCancelled = false;
		
		public int getBytes() {
			return this.bytes;
		}
		
		public void cancel() {
			this.isCancelled = true;
		}
		public boolean isCancelled() {
			return this.isCancelled;
		}
	}
	
	public static String getString(InputStream inputStream) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		
		// Remove beginning UTF-8 byte order mark (BOM)
		bufferedReader.mark(1);
		if (bufferedReader.read() != 0xFEFF) {
			bufferedReader.reset();
		}
		
		StringBuilder stringBuilder = new StringBuilder();
		String line = bufferedReader.readLine();
		while (line != null) {
			stringBuilder.append(line + "\n");
			line = bufferedReader.readLine();
		}
		
		bufferedReader.close();
		
		return stringBuilder.toString();
	}
	
	public static void sendToOutputStream(OutputStream outputStream, String output) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, AOSConstants.CHARACTER_ENCODING);
		writer.write(output);
		writer.flush();
	}
}
