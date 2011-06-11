package com.gluegadget.hndroid;

public class Utils {

	/** 
	 * Returns the preferred String if it is not empty ot the alternative String otherwise.
	 */
	public static String coalesce(String... alternatives) {
		
		for(String alt : alternatives) {
			if(alt != null) {
				return alt;
			}
		}
		
		return null;
	}
	
	/**
	 * Checks if a string is empty 
	 * 
	 * @param s the String to check 
	 * @return true if s is empty or null
	 */
	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0;
	}
}
