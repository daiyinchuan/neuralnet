/**
 * 
 */
package com.daiyc.mahjong;

import static com.daiyc.mahjong.AICommon.*;
import static com.daiyc.mahjong.Util.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.daiyc.mahjong.AICommon.Matrix2D;
import com.daiyc.mahjong.AICommon.Point;
import com.daiyc.mahjong.AICommon.Timer;
import com.daiyc.mahjong.AICommon.Vector2D;
import com.daiyc.mahjong.Minesweeper.GenAlg.Genome;
/**
 * 神经网络-扫雷机器人
 * @author Administrator
 */
public class Minesweeper extends JPanel {
	private static final long serialVersionUID = 1L;
	static final int WINDOW_WIDTH=400;
	static final int WINDOW_HEIGHT=400;
	static final int SWEEPER_SCALE=5;
	static final int MINE_SCALE=2;
	static final int NUM_INPUTS=1;
	static final int NUM_OUTPUTS=2;
	static final int NUM_HIDDEN=1;
	static final int NEURONS_PER_HIDDENLAYER=10;
	static final double MAX_TURN_RATE=0.3;
	static final int NUM_MINES=40;
	static final int NUM_SWEEPERS=30;
	static final int BIAS=-1;
	static final int ACTIVATION_RESPONSE=1;
	static final double CROSSOVER_RATE=0.7;
	static final double MUTATION_RATE=0.1;
	static final int NUM_TICKS=2000;
	static final double MAX_PERTURBATION=0.3;
	static int NUM_ELITE=4;
	static int NUM_COPIES_ELITE=1;
	static Vector<Genome> cloneGenomeVector(Vector<Genome> src){
		Vector<Genome> dst=new Vector<Genome>();
		for(Genome g:src){
			dst.add((Genome)(g.clone()));
		}
		return dst;
	}
	/**
	 * 神经网络
	 */
	static class NeuralNet{
		/**
		 * 神经细胞
		 */
		static class Neuron{
			int m_numInputs;//输入数目
			Vector<Double> m_vecWeight=new Vector<Double>();
			Neuron(int numInputs){
				m_numInputs=numInputs+1;
				for(int i=0;i<m_numInputs;i++){
					m_vecWeight.add(randClamped());
				}
			}
		}
		/**
		 *  神经细胞层
		 */
		static class NeuronLayer{
			int m_numNeurons;//本层使用的细胞数目
			//本层使用的细胞集合
			Vector<Neuron> m_vecNeurons=new Vector<Neuron>();
			NeuronLayer(int numNeurons,int numInputsPerNeuron){
				m_numNeurons=numNeurons;
				for(int i=0;i<m_numNeurons;i++){
					m_vecNeurons.add(new Neuron(numInputsPerNeuron));
				}
			}
		}
		int m_numInputs;
		int m_numOutputs;
		int m_numHiddenLayers;
		int m_neuronsPerHiddenLyr;
		Vector<NeuronLayer> m_vecLayers=new Vector<NeuronLayer>();
		NeuralNet(){
			m_numInputs=NUM_INPUTS;
			m_numOutputs=NUM_OUTPUTS;
			m_numHiddenLayers=NUM_HIDDEN;
			m_neuronsPerHiddenLyr=NEURONS_PER_HIDDENLAYER;
			createNet();
		}
		void createNet(){
			if(m_numHiddenLayers>0){//有隐藏层时
				//输入层
				m_vecLayers.add(new NeuronLayer(m_neuronsPerHiddenLyr,m_numInputs));
				for(int i=0;i<m_numHiddenLayers-1;i++){
					m_vecLayers.add(new NeuronLayer(m_neuronsPerHiddenLyr,
							m_neuronsPerHiddenLyr));
				}
				//输出层
				m_vecLayers.add(new NeuronLayer(m_numOutputs, m_neuronsPerHiddenLyr));
			}else{//无隐藏层时，只需创建输出层
				m_vecLayers.add(new NeuronLayer(m_numOutputs, m_numInputs));
			}
		}
		Vector<Double> getWeights(){
			Vector<Double> weights=new Vector<Double>();
			for(int i=0;i<m_numHiddenLayers+1;i++){//每一层
				//每一个神经细胞
				for(Neuron n:m_vecLayers.get(i).m_vecNeurons){
					weights.addAll(n.m_vecWeight);
				}
			}
			return weights;
		}
		int getNumberOfWeights(){
			int weights=0;
			for(int i=0;i<m_numHiddenLayers+1;i++){//每一层
				//每一个神经细胞
				for(Neuron n:m_vecLayers.get(i).m_vecNeurons){
					weights+=n.m_numInputs;
				}
			}
			return weights;
		}
		/**
		 * 新的权重替代旧的
		 * @param weights
		 */
		void putWeights(Vector<Double> weights){
			int cWeight=0;
			for(int i=0;i<m_numHiddenLayers+1;i++){//每一层
				//每一个神经细胞
				for(Neuron n:m_vecLayers.get(i).m_vecNeurons){
					for(int k=0;k<n.m_numInputs;k++){//每一个权重
						n.m_vecWeight.set(k, weights.get(cWeight++));
					}
				}
			}
		}
		/**
		 * S形响应曲线
		 * @param netinput
		 * @param response
		 * @return
		 */
		double sigmoid(double netinput,double response){
			return 1/(1+Math.exp(-netinput/response));
		}
		/**
		 * 根据一组输入计算输出，神经网络更新
		 * @param inputs
		 * @return
		 */
		@SuppressWarnings("unchecked")
		Vector<Double> update(Vector<Double> inputs){
			Vector<Double> _inputs=(Vector<Double>)inputs.clone();
			Vector<Double> outputs=new Vector<Double>(m_numOutputs);
			if(_inputs.size()!=m_numInputs){
				return outputs;
			}
			int cWeight=0;
			for(int i=0;i<m_numHiddenLayers+1;i++){
				if(i>0){
					_inputs=(Vector<Double>)outputs.clone();
				}
				outputs.clear();
				cWeight=0;
				//对每个神经细胞，求输入*对应权重乘积的总和，
				//并将总和赋给S形函数以计算输出
				for(Neuron n:m_vecLayers.get(i).m_vecNeurons){
					double netinput=0;
					int numInputs=n.m_numInputs;
					for(int k=0;k<numInputs-1;k++){
						netinput+=(n.m_vecWeight.get(k)*
								_inputs.get(cWeight++));
					}
					//加入偏移值
					netinput+=(n.m_vecWeight.get(numInputs-1)*BIAS);
					outputs.add(sigmoid(netinput, ACTIVATION_RESPONSE));
					cWeight=0;
				}
			}
			return outputs;
		}
		Vector<Integer> calcSplitPoint(){
			Vector<Integer> splitPoints=new Vector<Integer>();
			int weightCounter=0;
			for(int i=0;i<m_numHiddenLayers+1;i++){//每一层
				//每一个神经细胞
				for(Neuron n:m_vecLayers.get(i).m_vecNeurons){
					weightCounter=weightCounter+n.m_numInputs;
					splitPoints.add(weightCounter-1);
				}
			}
			return splitPoints;
		}
	}
	/**
	 * 遗传算法
	 */
	static class GenAlg{
		static class Genome{
			Vector<Double> m_vecWeights=new Vector<Double>();
			double m_dFitness;
			Genome(){}
			Genome(Vector<Double> w,double f){
				m_vecWeights=w;
				m_dFitness=f;
			}
			@SuppressWarnings("unchecked")
			public Object clone(){
				Genome gm=new Genome();
				gm.m_dFitness=m_dFitness;
				gm.m_vecWeights=(Vector<Double>)m_vecWeights.clone();
				return gm;
			}
			public String toString(){
				return "Fitness:"+m_dFitness+",vecWeights:"+m_vecWeights;
			}
			private static class FitnessCmp implements Comparator<Genome>{
				@Override
				public int compare(Genome g1, Genome g2) {
					double d=g1.m_dFitness-g2.m_dFitness;
					int r=0;
					if(d>0){
						r=1;
					}else if(d<0){
						r=-1;
					}
					return r;
				}
			}
			private static FitnessCmp fCmp=new FitnessCmp();
			static void sort(Vector<Genome> vecPop){
				Collections.sort(vecPop, fCmp);
			}
		}
		Vector<Genome> m_vecPop=new Vector<Genome>();
		int m_iPopSize;//种群数目
		int m_iChromoLen;//染色体长度
		double m_dBestFitness;//最好的
		double m_dWorstFitness;//最差的
		double m_dTotalFitness;//总和
		double m_dAverageFitness;//适应分平均分
		int m_iFittestGenome;//适应分最高的成员
		double m_dMutationRate;//突变率
		double m_dCrossoverRate;//杂交率
		int m_iGeneration;//哪一代
		Vector<Integer> m_vecSplitPoints;
		GenAlg(double mutRat,double crossRat,int popSize,
				int numWeights,Vector<Integer> splitPoints){
			m_dMutationRate=mutRat;
			m_dCrossoverRate=crossRat;
			m_iPopSize=popSize;
			m_iChromoLen=numWeights;
			m_dWorstFitness=BIG_NUMBER;
			for(int i=0;i<m_iPopSize;i++){
				m_vecPop.add(new Genome());
				for(int j=0;j<m_iChromoLen;j++){
					m_vecPop.get(i).m_vecWeights.add(randClamped());
				}
			}
			m_vecSplitPoints=splitPoints;
		}
		void reset(){
			m_dBestFitness=0;
			m_dWorstFitness=BIG_NUMBER;
			m_dTotalFitness=0;
			m_dAverageFitness=0;
		}
		/**
		 * 计算适应分
		 */
		void calcBestWorstAvTot(){
			m_dTotalFitness=0;
			double highestSoFar=0;
			double lowestSoFar=BIG_NUMBER;
			for(int i=0;i<m_iPopSize;i++){
				double fitness=m_vecPop.get(i).m_dFitness;
				if(fitness>highestSoFar){
					highestSoFar=fitness;
					m_iFittestGenome=i;
					m_dBestFitness=highestSoFar;
				}
				if(fitness<lowestSoFar){
					lowestSoFar=fitness;
					m_dWorstFitness=lowestSoFar;
				}
				m_dTotalFitness+=fitness;
			}
			m_dAverageFitness=m_dTotalFitness/m_iPopSize;
		}
		/**
		 * 排名变比
		 */
		void fitnessScaleRank(){
			int fitnessMultiplier=1;
			for(int i=0;i<m_iPopSize;i++){
				m_vecPop.get(i).m_dFitness=i*fitnessMultiplier;
			}
			calcBestWorstAvTot();
		}
		//=====================精英选择=====================
		void grabNBest(int nBest,int numCopies,Vector<Genome> vecNewPop){
			while(nBest-->0){
				for(int i=0;i<numCopies;i++){
					vecNewPop.add(m_vecPop.get(m_iPopSize-1-nBest));
				}
			}
		}
		/**
		 * 赌轮选择
		 * @return
		 */
		Genome rouletteWheelSelection(){
			double fSlice=new Random().nextFloat()*m_dTotalFitness;
			double fitnessSoFar=0;
			int selected=0;
			for(int i=0;i<m_iPopSize;i++){
				fitnessSoFar+=m_vecPop.get(i).m_dFitness;
				if(fitnessSoFar>=fSlice){
					selected=i;
					break;
				}
			}
			return m_vecPop.get(selected);
		}
		/**
		 * 杂交
		 * @param mum
		 * @param dad
		 * @param baby1
		 * @param baby2
		 */
		void crossoverAtSplits(Vector<Double> mum,Vector<Double> dad,
				Vector<Double> baby1,Vector<Double> baby2){
			Random rand=new Random();
			if(rand.nextFloat()>m_dCrossoverRate||mum.equals(dad)){
				baby1.addAll(mum);
				baby2.addAll(dad);
				return;
			}
			int index1=randInt(0, m_vecSplitPoints.size()-2);
			int index2=randInt(index1, m_vecSplitPoints.size()-1);
			int cp1=m_vecSplitPoints.get(index1);
			int cp2=m_vecSplitPoints.get(index2);
			for(int i=0;i<mum.size();i++){
				//如果在杂交点外，则保持原来的基因
				if(i<cp1 || i>=cp2){
					baby1.add(mum.get(i));
					baby2.add(dad.get(i));
				}else{
					baby1.add(dad.get(i));
					baby2.add(mum.get(i));
				}
			}
		}
		void mutate(Vector<Double> chromo){
			Random rand=new Random();
			for(int i=0;i<chromo.size();i++){
				if(rand.nextFloat()<m_dMutationRate){
					chromo.set(i, chromo.get(i)+randClamped()*MAX_PERTURBATION);
				}
			}
		}
		Vector<Genome> epoch(Vector<Genome> oldPop){
			m_vecPop=cloneGenomeVector(oldPop);
			reset();
//			fitnessScaleRank();
			Genome.sort(m_vecPop);
			calcBestWorstAvTot();
			Vector<Genome> vecNewPop=new Vector<Genome>();
			if((NUM_COPIES_ELITE*NUM_ELITE)%2==0){
				grabNBest(NUM_ELITE, NUM_COPIES_ELITE, vecNewPop);
			}
			while(vecNewPop.size()!=m_iPopSize){
				Genome mum=rouletteWheelSelection();
				Genome dad=rouletteWheelSelection();
				Genome baby1=new Genome();
				Genome baby2=new Genome();
				crossoverAtSplits(mum.m_vecWeights, dad.m_vecWeights,
						baby1.m_vecWeights, baby2.m_vecWeights);
				mutate(baby1.m_vecWeights);
				mutate(baby2.m_vecWeights);
				vecNewPop.add(baby1);
				vecNewPop.add(baby2);
			}
			m_vecPop=vecNewPop;
			return m_vecPop;
		}
	}
	static class CMinesweeper{
		NeuralNet m_brain;
		Vector2D m_vPosition;//在世界中的位置
		Vector2D m_vLookAt;//朝向
		double m_dRotation;//旋转
		double m_dSpeed;//速度
		double m_lTrack,m_rTrack;//左右轮的速度
		double m_dFitness;//适应分
		double m_dScale;//被画时的大小比例
		int m_iCloestMine;//最邻近的地雷下标位置
		//扫雷机形状顶点
		Vector<Point> m_sweeperVB=new Vector<Point>();
		double[][] sweeper={
			{-1,-1},{-1,1},{-0.5,1},{-0.5,-1},
			{0.5,-1},{1,-1},{1,1},{0.5,1},
			{-0.5,-0.5},{0.5,-0.5},
			{-0.5,0.5},{-0.25,0.5},{-0.25,1.75},{0.25,1.75},{0.25,0.5},{0.5,0.5}
		};
		public CMinesweeper() {
			m_brain=new NeuralNet();
			Random rand=new Random();
			m_dRotation=rand.nextFloat()*TWO_PI;
			m_lTrack=0.16;
			m_rTrack=0.16;
			m_dScale=SWEEPER_SCALE;
			m_vPosition=new Vector2D(
					rand.nextFloat()*WINDOW_WIDTH,
					rand.nextFloat()*WINDOW_HEIGHT);
			m_vLookAt=new Vector2D();
			for(int i=0;i<sweeper.length;i++){
				double[] p=sweeper[i];
				m_sweeperVB.add(new Point(p[0],p[1]));
			}
		}
		public String toString(){
			return "Fitness:"+m_dFitness+",m_brain:"
					+m_brain.getWeights();
		}
		void reset(){
			Random rand=new Random();
			m_vPosition=new Vector2D(
					rand.nextFloat()*WINDOW_WIDTH,
					rand.nextFloat()*WINDOW_HEIGHT);
			m_dFitness=0;
			m_dRotation=rand.nextFloat()*TWO_PI;
			m_vLookAt=new Vector2D();
		}
		/**
		 * 对扫雷机各顶点进行变换
		 * @param sweeper
		 */
		void worldTransform(Vector<Point> sweeper){
			Matrix2D m2=new Matrix2D();
			m2.scale(m_dScale, m_dScale);
			m2.rotate(m_dRotation);
			m2.translate(m_vPosition.x, m_vPosition.y);
			m2.transformPoints(sweeper);
		}
		/**
		 * 利用从扫雷机环境得到的信息来更新人工神经网络
		 * @param mines
		 * @return
		 */
		boolean update(Vector<Vector2D> mines){
			Vector<Double> inputs=new Vector<Double>();
			Vector2D vClosestMine=getClosestMine(mines);
			Vector2D.vec2DNormalize(vClosestMine);
			double dot=Vector2D.vec2DDot(m_vLookAt, vClosestMine);
			int sign=Vector2D.vec2DSign(m_vLookAt, vClosestMine);
			inputs.add(dot*sign);
			Vector<Double> output=m_brain.update(inputs);
			if(output.size()<NUM_OUTPUTS){
				return false;
			}
			m_lTrack=output.get(0);
			m_rTrack=output.get(1);
			double rotForce=m_lTrack-m_rTrack;
			rotForce=clamp(rotForce, -MAX_TURN_RATE, MAX_TURN_RATE);
			m_dRotation+=rotForce;
			m_dSpeed=m_lTrack+m_rTrack;
			m_vLookAt.x=-Math.sin(m_dRotation);
			m_vLookAt.y=Math.cos(m_dRotation);
			m_vPosition.add(Vector2D.multiply(m_vLookAt, m_dSpeed));
			if(m_vPosition.x>WINDOW_WIDTH){
				m_vPosition.x=0;
			}
			if(m_vPosition.x<0){
				m_vPosition.x=WINDOW_WIDTH;
			}
			if(m_vPosition.y>WINDOW_HEIGHT){
				m_vPosition.y=0;
			}
			if(m_vPosition.y<0){
				m_vPosition.y=WINDOW_HEIGHT;
			}
			return true;
		}
		/**
		 * 返回一个向量到最邻近的地雷
		 * @param mines
		 * @return
		 */
		Vector2D getClosestMine(Vector<Vector2D> mines){
			double closestSoFar=BIG_NUMBER;
			Vector2D vClosestMine=new Vector2D();
			for(int i=0;i<mines.size();i++){
				double lenToMine=Vector2D.vec2DLen(
						Vector2D.subtract(mines.get(i), m_vPosition));
				if(lenToMine<closestSoFar){
					closestSoFar=lenToMine;
					vClosestMine=Vector2D.subtract(m_vPosition, mines.get(i));
					m_iCloestMine=i;
				}
			}
			return vClosestMine;
		}
		/**
		 * 检测碰撞
		 * @param mines
		 * @param size
		 * @return
		 */
		int checkForMine(Vector<Vector2D> mines,double size){
			Vector2D distToMine=Vector2D.subtract(m_vPosition, mines.get(m_iCloestMine));
			if(Vector2D.vec2DLen(distToMine)<(size+5)){
				return m_iCloestMine;
			}
			return -1;
		}
		void putWeights(Vector<Double> w){
			m_brain.putWeights(w);
		}
		int getNumOfWeights(){
			return m_brain.getNumberOfWeights();
		}
		Vector<Integer> calcSplitPoints(){
			return m_brain.calcSplitPoint();
		}
		public void render(Graphics g){
			Vector<Point> sweeperVB=clonePointVector(m_sweeperVB);
			worldTransform(sweeperVB);
			int v;
			//draw left track
			for(v=0;v<3;v++){
				g.drawLine((int)sweeperVB.get(v).x,(int)sweeperVB.get(v).y,
						(int)sweeperVB.get(v+1).x,(int)sweeperVB.get(v+1).y);
			}
			g.drawLine((int)sweeperVB.get(v).x, (int)sweeperVB.get(v).y, 
					(int)sweeperVB.get(0).x,(int)sweeperVB.get(0).y);
			//draw right track
			for(v=4;v<7;v++){
				g.drawLine((int)sweeperVB.get(v).x,(int)sweeperVB.get(v).y,
						(int)sweeperVB.get(v+1).x,(int)sweeperVB.get(v+1).y);
			}
			g.drawLine((int)sweeperVB.get(v).x, (int)sweeperVB.get(v).y, 
					(int)sweeperVB.get(4).x,(int)sweeperVB.get(4).y);
			//draw
			g.drawLine((int)sweeperVB.get(8).x, (int)sweeperVB.get(8).y, 
					(int)sweeperVB.get(9).x,(int)sweeperVB.get(9).y);
			//draw
			for(v=10;v<15;v++){
				g.drawLine((int)sweeperVB.get(v).x,(int)sweeperVB.get(v).y,
						(int)sweeperVB.get(v+1).x,(int)sweeperVB.get(v+1).y);
			}
		}
		public void incFitness() {
			m_dFitness++;
		}
	}
	static class Controller implements Runnable{
		Vector<Genome> m_vecPop;
		Vector<CMinesweeper> m_vecSweepers=new Vector<CMinesweeper>();
		Vector<Vector2D> m_vecMines=new Vector<Vector2D>();//地雷
		GenAlg m_pGA;
		int m_numSweepers;
		int m_numMines;
		int m_numWeightsInNN;//神经网络中使用的权重值总数
		Vector<Point> m_mineVB=new Vector<Point>();//地雷形状顶点
		//每一代的平均适应分数
		Vector<Double> m_vecAvFitness=new Vector<Double>();
		//每一代的最高适应分数
		Vector<Double> m_vecBestFitness=new Vector<Double>();
		boolean m_bFastRender;
		boolean m_bShowFittest;
		int m_iTicks;//每一代的帧数
		int m_iGeneration;
		int m_cxClient,m_cyClient;
		boolean m_bDone;
		double[][] mine={
			{-1,-1},{-1,1},{1,1},{1,-1}
		};
		public Controller() {
			m_numSweepers=NUM_SWEEPERS;
			m_numMines=NUM_MINES;
			m_cxClient=WINDOW_WIDTH;
			m_cyClient=WINDOW_HEIGHT;
			for(int i=0;i<m_numSweepers;i++){
				m_vecSweepers.add(new CMinesweeper());
			}
			m_numWeightsInNN=m_vecSweepers.get(0).getNumOfWeights();
			Vector<Integer> splitPoints=m_vecSweepers.get(0).calcSplitPoints();
			m_pGA=new GenAlg(MUTATION_RATE,CROSSOVER_RATE,m_numSweepers,
					m_numWeightsInNN,splitPoints);
			m_vecPop=m_pGA.m_vecPop;
			for(int i=0;i<m_numSweepers;i++){
				m_vecSweepers.get(i).putWeights(m_vecPop.get(i).m_vecWeights);
			}
			Random rand=new Random();
			for(int i=0;i<m_numMines;i++){
				m_vecMines.add(new Vector2D(rand.nextFloat()*m_cxClient,
						rand.nextFloat()*m_cyClient));
			}
			for(int i=0;i<mine.length;i++){
				double[] p=mine[i];
				m_mineVB.add(new Point(p[0],p[1]));
			}
		}
		void worldTransform(Vector<Point> vecBuffer,Vector2D vPos){
			Matrix2D m2=new Matrix2D();
			m2.scale(MINE_SCALE,MINE_SCALE);
			m2.translate(vPos.x, vPos.y);
			m2.transformPoints(vecBuffer);
		}
		boolean update(){
			if(m_iTicks++<NUM_TICKS){
				for(int i=0;i<m_numSweepers;i++){
					if(!m_vecSweepers.get(i).update(m_vecMines)){
						pln("Wrong amount of NN inputs!");
						return false;
					}
					int grabHit=m_vecSweepers.get(i).checkForMine(m_vecMines, MINE_SCALE);
					if(grabHit>=0){
						m_vecSweepers.get(i).incFitness();
						Random rand=new Random();
						m_vecMines.set(grabHit, new Vector2D(rand.nextFloat()*m_cxClient,
								rand.nextFloat()*m_cyClient));
					}
					m_vecPop.get(i).m_dFitness=m_vecSweepers.get(i).m_dFitness;
				}
			}else{
				pln("#m_pGA.m_dBestFitness#"+m_pGA.m_dBestFitness);
				m_vecAvFitness.add(m_pGA.m_dAverageFitness);
				m_vecBestFitness.add(m_pGA.m_dBestFitness);
				m_iGeneration++;
				m_iTicks=0;
				pln("#m_vecPop#"+m_vecPop);
				m_vecPop=m_pGA.epoch(m_vecPop);
				pln("#m_vecPop epoch#"+m_vecPop);
				for(int i=0;i<m_numSweepers;i++){
					m_vecSweepers.get(i).putWeights(m_vecPop.get(i).m_vecWeights);
					m_vecSweepers.get(i).reset();
				}
			}
			return true;
		}
		void render(Graphics g){
			g.drawString("Generation:"+m_iGeneration, 5, 15);
			if(!m_bFastRender){
				g.setColor(Color.RED);
				for(int i=0;i<m_numMines;i++){
					Vector<Point> mineVB=clonePointVector(m_mineVB);
					worldTransform(mineVB, m_vecMines.get(i));
					int v;
					for(v=0;v<mineVB.size()-1;v++){
						g.drawLine((int)mineVB.get(v).x,(int)mineVB.get(v).y,
								(int)mineVB.get(v+1).x,(int)mineVB.get(v+1).y);
					}
					g.drawLine((int)mineVB.get(v).x,(int)mineVB.get(v).y,
							(int)mineVB.get(0).x,(int)mineVB.get(0).y);
				}
				for(int i=0;i<m_numSweepers;i++){
					g.setColor(Color.DARK_GRAY);
					if(m_pGA.m_iFittestGenome==i){
						g.setColor(Color.BLUE);
					}
					m_vecSweepers.get(i).render(g);
				}
			}else{
				plotStats(g);
			}
		}
		void plotStats(Graphics g){
			g.drawString("Best Fitness:"+m_pGA.m_dBestFitness,5,35);
			g.drawString("Average Fitness:"+m_pGA.m_dAverageFitness,5,55);
			float hSlice=(float)(m_cxClient/(m_iGeneration+1));
			float vSlice=(float)(m_cyClient/((m_pGA.m_dBestFitness+1)*2));
			g.setColor(Color.RED);
			drawLineStats(g,hSlice,vSlice,m_vecBestFitness);
			g.setColor(Color.BLUE);
			drawLineStats(g,hSlice,vSlice,m_vecAvFitness);
		}
		private void drawLineStats(Graphics g,float hSlice,float vSlice,
				Vector<Double> vecFitness){
			int sx=0,sy=m_cyClient,ex=0,ey=0;
			for(int i=0;i<vecFitness.size();i++){
				ey=m_cyClient-(int)(vSlice*vecFitness.get(i));
				g.drawLine(sx, sy, ex, ey);
				sx=ex;
				sy=ey;
				ex+=hSlice;
			}
		}
		void fastRenderToggle(){
			m_bFastRender=!m_bFastRender;
		}
		void toggleShowFittest(){
			m_bShowFittest=!m_bShowFittest;
		}
		@Override
		public void run() {
			Timer t=new Timer(FRAMES_PER_SECOND);
			t.start();
			while(!m_bDone){
				if(t.readyForNextFrame()||m_bFastRender){
					if(!update()){
						m_bDone=true;
					}
				}
			}
		}
	}
	Controller c;
	public void paint(Graphics g){
		super.paint(g);
		c.render(g);
	}
	public void toDraw(){
		long s=1000/FRAMES_PER_SECOND;
		while(!c.m_bDone){
			try {
				Thread.sleep(s);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			repaint();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JFrame f=new JFrame();
    	f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    	f.setBounds(500, 150, WINDOW_WIDTH, WINDOW_HEIGHT);
    	f.setVisible(true);
    	final Minesweeper m=new Minesweeper();
    	f.setContentPane(m);
    	m.c=new Controller();
    	f.addKeyListener(new KeyAdapter(){
    		public void keyPressed(final KeyEvent ke){
    			new Thread(new Runnable(){
					@Override
					public void run() {
						if(ke.getKeyCode()==KeyEvent.VK_ENTER){
							m.c.m_bDone=false;
							new Thread(m.c).start();
							m.toDraw();
						}else if(ke.getKeyCode()==KeyEvent.VK_SPACE){
							m.c.m_bDone=true;
						}else if(ke.getKeyCode()==KeyEvent.VK_F){
							m.c.fastRenderToggle();
						}
					}
    			}).start();
    		}
    	});
	}
}