/**
 * 
 */
package com.daiyc.mahjong;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import static com.daiyc.mahjong.Util.*;

/**
 *	图的一种存储结构——邻接矩阵
 */
public class MGraph {
	static final int INFINITY=65535;
	String[] vexs;
	int[][] arc;
	int numVertexes,numEdges;
	boolean isValidPath(int d){
		if(d>0 && d<INFINITY){
			return true;
		}
		return false;
	}
	void createMGraph(File f){
		BufferedReader br=null;
		try{
			br=new BufferedReader(new FileReader(f));
			//输入顶点数和边数
			String numStr=br.readLine();
			String[] nums=numStr.split(" ");
			numVertexes=Integer.parseInt(nums[0]);
			numEdges=Integer.parseInt(nums[1]);
			//输入顶点表（一维数组）
			String vexStr=br.readLine();
			String[] vexs=vexStr.split(" ");
			this.vexs=new String[numVertexes];
			for(int i=0;i<numVertexes;i++){
				this.vexs[i]=vexs[i];
			}
			//输入边矩阵（二维数组）
			arc=new int[numVertexes][numVertexes];
			for(int i=0;i<numVertexes;i++){
				String edgeStr=br.readLine();
				String[] edges=edgeStr.split(" ");
				for(int j=0;j<numVertexes;j++){
					if(edges[j].equals("*")){
						arc[i][j]=INFINITY;
					}else{
						arc[i][j]=Integer.parseInt(edges[j]);
					}
				}
			}
		}catch(IOException e){
			e.printStackTrace();
		}finally{
			if(br!=null){
				try{
					br.close();
				}catch(IOException e){
				}
			}
		}
	}
	
	public static void main(String[] args) {
		MGraph g=new MGraph();
		g.createMGraph(file("0jjz9.txt"));
		pln("#顶点表#");
		for(String v:g.vexs){
			p(v);p(" ");
		}
		pln();pln("#边邻接矩阵#");
		for(int[] r:g.arc){
			for(int c:r){
				p(""+c);
				p(" ");
			}
			pln();
		}
	}
}
