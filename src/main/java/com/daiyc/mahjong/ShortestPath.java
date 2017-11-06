/**
 * 
 */
package com.daiyc.mahjong;
import static com.daiyc.mahjong.Util.*;

import java.util.ArrayDeque;
import java.util.Deque;
/**
 *	最短路径算法
 */
public class ShortestPath {
	/**
	 * Dijkstra算法
	 * @param g 邻接矩阵图
	 * @param v0 起始顶点索引
	 * @param pathmatrix 路径矩阵
	 * @param shortPathTable 最短路径权值表
	 */
	static void dijkstra(MGraph g,int v0,
			int[] pathmatrix,int[] shortPathTable){
		//_final[w]=1表示求得顶点v0到vw的最短路径
		int[] _final=new int[g.numVertexes];
		int k=0,min=0;
		for(int v=0;v<g.numVertexes;v++){
			//将与v0点有连线的顶点加上权值
			shortPathTable[v]=g.arc[v0][v];
		}
		_final[v0]=1;//v0到v0不需要求路径
		for(int v=1;v<g.numVertexes;v++){
			min=MGraph.INFINITY;
			//寻找离v0最近的顶点
			for(int w=0;w<g.numVertexes;w++){
				if(_final[w]==0&&shortPathTable[w]<min){
					k=w;
					min=shortPathTable[w];//w顶点离v0顶点更近
				}
			}
			_final[k]=1;//将目前找到的最近的顶点置为1
			//修正当前最短路径及距离
			for(int w=0;w<g.numVertexes;w++){
				if(_final[w]==0&&(min+g.arc[k][w]<shortPathTable[w])){
					shortPathTable[w]=min+g.arc[k][w];
					pathmatrix[w]=k;
				}
			}
		}
	}
	/**
	 * Floyd算法
	 */
	static void floyd(){
		
	}
	
	public static void main(String[] args) {
		MGraph mg=new MGraph();
		mg.createMGraph(file("0jjz_s.txt"));
		
		pln("#最短路径算法_Dijkstra算法#");
		int[] pathmatrix=new int[mg.numVertexes];
		int[] shortPathTable=new int[mg.numVertexes];
		dijkstra(mg, 0, pathmatrix, shortPathTable);
		
		int d=8;
		pf("#起始为v0目的为v%d的最短路径为#",d);
		int p=pathmatrix[d];
		Deque<Integer> pStack=new ArrayDeque<Integer>();
		while(p>0){
			pStack.push(p);
			p=pathmatrix[p];
		}
		p("v0,");
		while(pStack.size()>0){
			p("v"+pStack.pop()+",");
		}
		p("v8");
		pln("\n#最短路径权值#"+shortPathTable[8]);
	}
}
