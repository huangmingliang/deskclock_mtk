package com.android.deskclock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * add only for holster
 * @author kuan.liang
 *
 */
public class HolsterUtil {

	/**
	 * Add:Read the hall state
	 * 
	 * @return false ( 0 means holster is leaved); true (1 means holster is
	 *         close)
	 */
	public static boolean queryHallState() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(
					"/sys/class/switch/hall/state"));
			String line = br.readLine();
			return (1 == Integer.parseInt(line.trim()));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	
	/**
	 * Add:To determine whether the hall sensor is exists.
	 * 
	 * @return true (support); false(not support)
	 */
	public static boolean isHallExists() {
		File file = new File("/sys/class/switch/hall/state");
		return file.exists();
	}
}
