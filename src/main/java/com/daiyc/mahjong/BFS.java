/**
 * 
 */
package com.daiyc.mahjong;

import java.util.ArrayDeque;
import java.util.Queue;

import com.daiyc.mahjong.GraphAdjList.EdgeNode;

import static com.daiyc.mahjong.Util.*;
/**
 *	广度优先遍历
 */
public class BFS {
	boolean[] visited;
	/**
	 * 邻接矩阵广度优先遍历
	 * @param g
	 */
	void bfsMgTraverse(MGraph g){
		Queue<Integer> q=new ArrayDeque<Integer>();
		visited=new boolean[g.numVertexes];
		for(int i=0;i<g.numVertexes;i++){
			if(!visited[i]){
				visited[i]=true;
				pf("%s ",g.vexs[i]);
				q.offer(i);
				while(!q.isEmpty()){
					i=q.poll();
					for(int j=0;j<g.numVertexes;j++){
						if(g.isValidPath(g.arc[i][j])&&!visited[j]){
							visited[j]=true;
							pf("%s ",g.vexs[j]);
							q.offer(j);
						}
					}
				}
			}
		}
	}
	/**
	 * 邻接表广度优先遍历
	 * @param g
	 */
	void bfsGalTraverse(GraphAdjList g){
		Queue<Integer> q=new ArrayDeque<Integer>();
		visited=new boolean[g.numVertexes];
		EdgeNode e;
		for(int i=0;i<g.numVertexes;i++){
			if(!visited[i]){
				visited[i]=true;
				pf("%s ",g.adjList[i].data);
				q.offer(i);
				while(!q.isEmpty()){
					i=q.poll();
					e=g.adjList[i].firstedge;
					while(e!=null){
						if(!visited[e.adjvex]){
							visited[e.adjvex]=true;
							pf("%s ",g.adjList[e.adjvex].data);
							q.offer(e.adjvex);
						}
						e=e.next;
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		BFS bfs=new BFS();
		MGraph mg=new MGraph();
		mg.createMGraph(file("0jjz9.txt"));
		pln("#邻接矩阵广度优先遍历#");
		bfs.bfsMgTraverse(mg);
		pln();
		GraphAdjList gal=new GraphAdjList();
		gal.createALGraph(file("0jb9.txt"));
		pln("#邻接表广度优先遍历#");
		bfs.bfsGalTraverse(gal);
	}
}
