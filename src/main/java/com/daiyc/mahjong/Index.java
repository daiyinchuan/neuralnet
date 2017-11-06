/**
 * 
 */
package com.daiyc.mahjong;
import static com.daiyc.mahjong.Util.*;

/**
 * 模式匹配算法
 */
public class Index {
	/**
	 * 朴素模式匹配算法
	 * @param s
	 * @param t
	 * @param pos
	 * @return
	 */
	static int pIndex(String s,String t,int pos){
		int i=pos;
		int j=0;
		pf("#朴素模式匹配开始在位置i=%d开始匹配#%s",i,s);
		while(i<s.length()-1 && j<=t.length()-1){
			p("^i="+i+" j="+j+"$");
			if(s.charAt(i)==t.charAt(j)){
				i++;
				j++;
			}else{
				i=i-j+1;
				pf("\n#失配，开始在位置i=%d开始匹配#%s",i,s.substring(i));
				j=0;
			}
		}
		if(j>=t.length()){
			pf("\n#匹配中，在位置%d#",i-t.length());
			return i-t.length();
		}else{
			return -1;
		}
	}
	/**
	 * KMP模式匹配算法
	 * @param s
	 * @param t
	 * @param pos
	 * @return
	 */
	static int kmpIndex(String s,String t,int pos){
		int i=pos;
		int j=0;
		int[] next=new int[t.length()];
		getNextVal(t,next);
		pf("#KMP模式匹配开始在位置i=%d开始匹配#%s",i,s);
		while(i<=s.length()-1 && j<=t.length()-1){
			p("^i="+i+" j="+j+"$");
			if(j==-1||s.charAt(i)==t.charAt(j)){
				i++;
				j++;
			}else{
				pf("\n#失配，开始在位置i=%d开始匹配#%s",i,s.substring(i));
				j=next[j];
			}
		}
		if(j>=t.length()){
			pf("\n#匹配中，在位置%d#",i-t.length());
			return i-t.length();
		}else{
			return -1;
		}
	}
	static void getNextVal(String t,int[] nextVal){
		int j=0,k=-1;
		int len=t.length();
		nextVal[0]=-1;
		while(j<len-1){
			if(k==-1||t.charAt(j)==t.charAt(k)){
				j++;
				k++;
				if(t.charAt(j)!=t.charAt(k)){
					nextVal[j]=k;
				}else{
					nextVal[j]=nextVal[k];
				}
			}else{
				k=nextVal[k];
			}
		}
		p("#nextVal#");
		for(int i=0;i<nextVal.length;i++){
			p(""+nextVal[i]);
		}
		pln();
	}
	
	public static void main(String[] args) {
		String s="abcdefgooglhijklmngoogpqrstgoouvgowxgyzgoogleabc";
		pln("#主串#"+s);
		String t="google";
		pln("#子串#"+t);
		int w=pIndex(s,t,0);
		pln("\n#位置在#"+w);
		w=kmpIndex(s, t, 0);
		pln("\n#位置在#"+w);
	}
}
