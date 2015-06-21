package org.aosutils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * Converts an OutputStream to an InputStream
 * This is accomplished by running OutputStream and InputStream in separate threads.
 * start() returns when all work is completed.  
 */

public class OutputToInputStreamConverter {
	OutputWriterCallable<Object> outputWriter;
	InputReaderCallable<Object> inputReader;
	
	public OutputToInputStreamConverter(OutputWriterCallable<Object> outputWriter, InputReaderCallable<Object> inputReader) {
		this.outputWriter = outputWriter;
		this.inputReader = inputReader;
	}
	
	public void start() throws Throwable {
		ExecutorService service = Executors.newFixedThreadPool(2);
		
		PipedInputStream inputStream = new PipedInputStream();
		inputReader.setInputStream(inputStream);
		PipedOutputStream outputStream = new PipedOutputStream(inputStream);
		outputWriter.setOutputStream(outputStream);
		
		try {
			service.submit(outputWriter); // Do not wait for this; this completes when inputReader completes
			service.submit(inputReader).get(); // .get() waits for this to complete
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		catch (ExecutionException e) {
			throw e.getCause();
		}
	}
	
	public static abstract class OutputWriterCallable<V> implements Callable<V> {
		OutputStream outputStream;
		
		private void setOutputStream(OutputStream outputStream) {
			this.outputStream = outputStream;
		}
		
		@Override
		public V call() throws Exception {
			run(outputStream);
			outputStream.close();
			
			return null;
		}
		
		public abstract void run(OutputStream outputStream) throws Exception;
	}
	
	public static abstract class InputReaderCallable<V> implements Callable<V> {
		InputStream inputStream;
		
		private void setInputStream(InputStream inputStream) {
			this.inputStream = inputStream;
		}
		
		@Override
		public V call() throws Exception {
			run(inputStream);
			inputStream.close();
						
			return null;
		}
		
		public abstract void run(InputStream inputStream) throws Exception;
	}
}
