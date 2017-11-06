/**
 * 
 */
package com.daiyc.mahjong;
import static com.daiyc.mahjong.Util.*;

import com.daiyc.mahjong.GraphAdjList.EdgeNode;
/**
 * 深度优先遍历
 */
public class DFS {

	boolean[] visited;
	
	private void dfsMg(MGraph g,int i){
		int j;
		visited[i]=true;
		pf("%s ",g.vexs[i]);
		for(j=0;j<g.numVertexes;j++){
			if(g.isValidPath(g.arc[i][j])&& !visited[j]){
				dfsMg(g,j);
			}
		}
	}
	/**
	 * 邻接矩阵深度遍历
	 * @param g
	 */
	void dfsMgTraverse(MGraph g){
		visited=new boolean[g.numVertexes];
		for(int i=0;i<g.numVertexes;i++){
			if(!visited[i]){
				dfsMg(g,i);
			}
		}
	}
	
	private void dfsGal(GraphAdjList g,int i){
		visited[i]=true;
		pf("%s ",g.adjList[i].data);
		EdgeNode e=g.adjList[i].firstedge;
		while(e!=null){
			if(!visited[e.adjvex]){
				dfsGal(g,e.adjvex);
			}
			e=e.next;
		}
	}
	
	/**
	 * 邻接表深度遍历
	 * @param g
	 */
	void dfsGalTraverse(GraphAdjList g){
		visited=new boolean[g.numVertexes];
		for(int i=0;i<g.numVertexes;i++){
			if(!visited[i]){
				dfsGal(g, i);
			}
		}
	}
	
	public static void main(String[] args) {
		DFS dfs=new DFS();
		MGraph mg=new MGraph();
		mg.createMGraph(file("0jjz9.txt"));
		pln("#邻接矩阵深度优先遍历#");
		dfs.dfsMgTraverse(mg);
		pln();
		GraphAdjList gal=new GraphAdjList();
		gal.createALGraph(file("0jb9.txt"));
		pln("#邻接表深度优先遍历#");
		dfs.dfsGalTraverse(gal);
	}

}
