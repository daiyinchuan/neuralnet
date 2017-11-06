/**
 * 
 */
package com.daiyc.mahjong;

import java.io.File;

/**
 * @author Administrator
 *
 */
public class Util {
	static void pln(String str){
		System.out.println(str);
	}
	static void pf(String str,Object... paramVarArgs){
		System.out.printf(str, paramVarArgs);
	}
	static void pln(){
		System.out.println();
	}
	static void p(String str){
		System.out.print(str);
	}
	static File file(String name){
		return new File(Util.class.getResource(name).getPath());
	}
}
