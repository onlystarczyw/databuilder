package com.onlystarczy.databuild.utils;

import java.util.Collection;

public class CollectionUtils {
	
	public static boolean isEmpty(Collection<?> c) {
		if(null == c || c.size() == 0) {
			return true;
		}
		return false;
	}
	
	public static boolean isNotEmpty(Collection<?> c) {
		return !isEmpty(c);
	}

}
