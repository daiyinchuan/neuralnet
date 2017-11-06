/**
 * 
 */
package com.daiyc.mahjong;

import java.util.ArrayList;
import java.util.List;
import static com.daiyc.mahjong.Util.*;
/**
 *	最小生成树算法
 */
public class MinSpanTree {
	/**
	 * Prim算法
	 * @param g
	 * @return
	 */
	static Integer[][] prim(MGraph g){
		List<Integer[]> ret=new ArrayList<Integer[]>(); 
		int min,i,j,k;
		int[] adjvex=new int[g.numVertexes];
		int[] lowcost=new int[g.numVertexes];
		for(i=1;i<g.numVertexes;i++){
			lowcost[i]=g.arc[0][i];
		}
		for(i=1;i<g.numVertexes;i++){
			min=MGraph.INFINITY;
			j=1;k=0;
			while(j<g.numVertexes){
				if(lowcost[j]!=0&&lowcost[j]<min){
					min=lowcost[j];
					k=j;
				}
				j++;
			}
			ret.add(new Integer[]{adjvex[k],k});
			lowcost[k]=0;
			for(j=1;j<g.numVertexes;j++){
				if(lowcost[j]!=0&&g.arc[k][j]<lowcost[j]){
					lowcost[j]=g.arc[k][j];
					adjvex[j]=k;
				}
			}
		}
		return ret.toArray(new Integer[ret.size()][2]);
	}
	/**
	 * Kruskal算法，对比两个算法，克鲁斯卡尔算法主要针对边来展开的，
	 * 边数少时效率会非常高，所以对于稀疏图有很大的优势；
	 * 普里姆算法对于稠密图，即边数非常多的情况会更好一些。
	 * @param g
	 * @return
	 */
	static Integer[][] kruskal(MGraph g){
		return null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MGraph mg=new MGraph();
		mg.createMGraph(file("0jjz_m.txt"));
		pln("#最小生成树算法 Prim算法#");
		Integer[][] p=prim(mg);
		int t=0;
		for(Integer[] a:p){
			pf("(%d,%d)",a[0],a[1]);
			t+=mg.arc[a[0]][a[1]];
		}
		p("\n"+t);
	}

}
