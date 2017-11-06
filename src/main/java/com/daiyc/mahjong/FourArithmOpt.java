/**
 * 
 */
package com.daiyc.mahjong;
import static com.daiyc.mahjong.Util.p;
import static com.daiyc.mahjong.Util.pln;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 *	四则混合运算
 */
public class FourArithmOpt {
	public static void main(String[] args) throws NumberFormatException, ScriptException {
		pln("#四则混合运算器#");
		BufferedReader br=null;
		String midStr="";
		try{
			br=new BufferedReader(
					new InputStreamReader(System.in));
			midStr=br.readLine();
			if(midStr==null||midStr.equals("")){
				midStr="9+(3*5-10*3+20/5+15)*3+(10/2-5*3)+20/4";
			}
			p("#中缀表达式#");
			pln(midStr);
		}catch(IOException e){
		}finally{
			try{
				br.close();
			}catch(IOException e){
			}
		}
		Map<Character,Byte> optAsciis=new LinkedHashMap<Character,Byte>(6);
		optAsciis.put('+', "+".getBytes()[0]);
		optAsciis.put('-', "-".getBytes()[0]);
		optAsciis.put('*', "*".getBytes()[0]);
		optAsciis.put('/', "/".getBytes()[0]);
		optAsciis.put('(', "(".getBytes()[0]);
		optAsciis.put(')', ")".getBytes()[0]);
		//每个操作数和操作符分隔开到一个队列
		p("#分隔到队列#");
		Queue<String> mEls=new ArrayDeque<String>();
		StringBuilder lnStr=new StringBuilder();
		for(int i=0;i<midStr.length();i++){
			char c=midStr.charAt(i);
			if(Character.isDigit(c)||c==46){//46为小数点
				lnStr.append(c);
			}else if(optAsciis.containsKey(c)){
				if(lnStr.length()>0){
					mEls.offer(lnStr.toString());
					lnStr.delete(0, lnStr.length());
				}
				mEls.offer(""+c);
			}
		}
		if(lnStr.length()>0){
			mEls.offer(lnStr.toString());
		}
		//打印分隔队列
		for(Iterator<String> it=mEls.iterator();it.hasNext();){
			p(it.next());
			if(it.hasNext()){
				p(" ");
			}else{
				pln();
			}
		}
		p("#转换成后缀表达式队列#");
		Queue<String> sEls=new ArrayDeque<String>();//队列
		Deque<Character> optStack=new ArrayDeque<Character>();//堆栈
		//操作符优先级
		Map<Character,Integer> optPris=new LinkedHashMap<Character,Integer>(6);
		optPris.put('+', 0);//低
		optPris.put('-', 0);
		optPris.put('(', 0);
		optPris.put('*', 1);//高
		optPris.put('/', 1);
		while(mEls.size()>0){
			String el=mEls.poll();
			char c=el.charAt(0);
			if(Character.isDigit(c)){
				sEls.offer(el);
			}else if(optAsciis.containsKey(c)){
				if(c==41){//)操作符 	将操作符栈出栈，直到(为止
					char cc=optStack.pop();
					while(cc!=40){
						sEls.offer(""+cc);
						cc=optStack.pop();
					}
				}else{//+ - * / ( 这些操作符
					if(c==40||optStack.size()==0||optPris.get(c)>optPris.get(optStack.peek())){
						optStack.push(c);
					}else{
						char cc=optStack.peek();
						while(optStack.size()>0 && cc!=40 
								&& optPris.get(c)<=optPris.get(cc)){
							sEls.offer(""+optStack.pop());
							if(optStack.size()>0){
								cc=optStack.peek();
							}
						}
						optStack.push(c);
					}
				}
			}
		}
		while(optStack.size()>0){
			sEls.offer(""+optStack.pop());
		}
		//打印后缀表达式队列
		for(Iterator<String> it=sEls.iterator();it.hasNext();){
			p(it.next());
			if(it.hasNext()){
				p(" ");
			}else{
				pln();
			}
		}
		p("#使用后缀表达式计算结果#");
		Deque<Double> numStack=new ArrayDeque<Double>();//堆栈
		ScriptEngine se=new ScriptEngineManager().getEngineByName("js");
		while(sEls.size()>0){
			String el=sEls.poll();
			char c=el.charAt(0);
			if(Character.isDigit(c)){
				numStack.push(Double.parseDouble(el));
			}else{
				Double b=numStack.pop();
				Double a=numStack.pop();
				Double result=Double.parseDouble(se.eval(a+el+b).toString());
				numStack.push(result);
			}
		}
		pln(""+numStack.pop());
	}
}
