package ru.ifmo.rain.moshnikov.walk;

import java.io.PrintStream;

 class Logger {
	
	private static final PrintStream LOG_STREAM = System.out;
	private static final boolean PRINT_STACK_TRACE = false;
	
	static void error(Exception e, String message) {
		if (PRINT_STACK_TRACE) {
			e.printStackTrace();
		}
		LOG_STREAM.println(message);
	}
	
}