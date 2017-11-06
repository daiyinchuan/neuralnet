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
 * 图的一种存储结构——邻接表
 */
public class GraphAdjList {
	VertexNode[] adjList;
	int numVertexes,numEdges;
	/**
	 * 顶点表结点
	 */
	static class VertexNode{
		String data;
		EdgeNode firstedge;
	}
	/**
	 * 边表结点
	 */
	static class EdgeNode{
		int adjvex;
		int weight;
		EdgeNode next;
	}
	private String readLine(BufferedReader reader){
		String line=null;
		try{
			line=reader.readLine();
			while(line.startsWith("#")){
				line=reader.readLine();
			}
		}catch(IOException e){
		}
		return line;
	}
	void createALGraph(File f){
		BufferedReader br=null;
		try{
			br=new BufferedReader(new FileReader(f));
			//输入顶点数和边数
			String numStr=readLine(br);
			String[] nums=numStr.split(" ");
			numVertexes=Integer.parseInt(nums[0]);
			numEdges=Integer.parseInt(nums[1]);
			//输入顶点表（一维数组）
			String vexStr=readLine(br);
			String[] vexs=vexStr.split(" ");
			adjList=new VertexNode[numVertexes];
			for(int i=0;i<numVertexes;i++){
				VertexNode vn=new VertexNode();
				vn.data=vexs[i];
				adjList[i]=vn;
			}
			//输入边表
			for(int n=0;n<numEdges;n++){
				String edgeStr=readLine(br);
				String[] strs=edgeStr.split(" ");
				int i=Integer.parseInt(strs[0]);
				int j=Integer.parseInt(strs[1]);
				int w=Integer.parseInt(strs[2]);
				EdgeNode e=new EdgeNode();
				e.adjvex=j;
				e.weight=w;
				EdgeNode e1=adjList[i].firstedge;
				if(e1==null){
					adjList[i].firstedge=e;
				}else{
					while(e1.next!=null){
						e1=e1.next;
					}
					e1.next=e;
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
		GraphAdjList g=new GraphAdjList();
		g.createALGraph(file("0jb9.txt"));
		pln("#顶点表#");
		for(VertexNode v:g.adjList){
			p(v.data);
			p(" ");
		}
		pln();
		pln("#边邻接表#");
		for(VertexNode v:g.adjList){
			if(v.firstedge!=null){
				EdgeNode e=v.firstedge;
				do{
					p(v.data);p(" ");
					p(""+e.adjvex);p(" ");
					p(""+e.weight);pln();
					e=e.next;
				}while(e!=null);
			}
		}
	}
}
