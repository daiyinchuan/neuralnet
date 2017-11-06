/**
 * 
 */
package com.daiyc.mahjong;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.daiyc.mahjong.AICommon.Matrix2D;
import com.daiyc.mahjong.AICommon.Point;
import com.daiyc.mahjong.AICommon.Timer;
import com.daiyc.mahjong.AICommon.Vector2D;
import com.daiyc.mahjong.SmartMinesweeper.GenAlg.Genome;

import static com.daiyc.mahjong.AICommon.*;
import static com.daiyc.mahjong.Util.pln;
/**
 * 知觉，碰撞检测，记忆
 * @author Administrator
 */
public class SmartMinesweeper extends JPanel {
	private static final long serialVersionUID = 1L;
	static final int WINDOW_WIDTH=400;
	static final int WINDOW_HEIGHT=400;
	static final int SWEEPER_SCALE=5;
	static final int NUM_SENSORS=5;
	static final int NUM_INPUTS=2*NUM_SENSORS+1;
	static final int NUM_OUTPUTS=2;
	static final int NUM_HIDDEN=1;
	static final int NEURONS_PER_HIDDENLAYER=10;
	static final double MAX_TURN_RATE=0.2;
	static final int NUM_SWEEPERS=40;
	static final int BIAS=-1;
	static final double ACTIVATION_RESPONSE=1.0;
	static final double CROSSOVER_RATE=0.7;
	static final double MUTATION_RATE=0.1;
	static final int NUM_TICKS=2000;
	static final double MAX_PERTURBATION=0.3;
	static final double SENSOR_RANGE=25;
	static final double ROTATION_TOLERANCE=0.03;
	static final double COLLISION_DIST=(double)(SWEEPER_SCALE+1)/SENSOR_RANGE;
	static final double MAX_SPEED=2;
	static final int CELL_SIZE=20;
	static final int TOURNAMENT_COMPETITORS=5;
	static int iNumElite=4;
	static int iNumCopiesElite=1;
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
			/**
			 * 进入神经细胞的输入数目
			 */
			int m_numInputs;
			/**
			 * 为每一个输入提供的权重
			 */
			Vector<Double> m_vecWeight=new Vector<Double>();
			Neuron(int numInputs){
				m_numInputs=numInputs+1;
				for(int i=0;i<m_numInputs;i++){
					//初始化为任意-1到1之间的实数
					m_vecWeight.add(randClamped());
				}
			}
		}
		/**
		 * 神经细胞层
		 */
		static class NeuronLayer{
			/**
			 * 本层使用的神经细胞数目
			 */
			int m_numNeurons;
			/**
			 * 神经细胞的层
			 */
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
		/**
		 * 创建网络
		 */
		void createNet(){
			if(m_numHiddenLayers>0){//有隐藏层时
				//输入层
				m_vecLayers.add(new NeuronLayer(m_neuronsPerHiddenLyr,m_numInputs));
				//中间隐藏层
				for(int i=0;i<m_numHiddenLayers-1;i++){
					m_vecLayers.add(new NeuronLayer(m_neuronsPerHiddenLyr,m_neuronsPerHiddenLyr));
				}
				//输出层
				m_vecLayers.add(new NeuronLayer(m_numOutputs,m_neuronsPerHiddenLyr));
			}else{
				m_vecLayers.add(new NeuronLayer(m_numOutputs, m_numInputs));
			}
		}
		Vector<Double> getWeights(){
			Vector<Double> weights=new Vector<Double>();
			for(int i=0;i<m_numHiddenLayers+1;i++){//每一层
				//每一个神经细胞
				for(Neuron n:m_vecLayers.get(i).m_vecNeurons){
					//每一个权重
					for(int k=0;k<n.m_numInputs;k++){
						weights.add(n.m_vecWeight.get(k));
					}
				}
			}
			return weights;
		}
		int getNumberOfWeights(){
			int weights=0;
			for(int i=0;i<m_numHiddenLayers+1;i++){//每一层
				//每一个神经细胞
				for(Neuron n:m_vecLayers.get(i).m_vecNeurons){
					weights=weights+n.m_numInputs;
				}
			}
			return weights;
		}
		/**
		 * 新权重替换旧的
		 * @param weights
		 */
		void putWeights(Vector<Double> weights){
			int cWeights=0;
			for(int i=0;i<m_numHiddenLayers+1;i++){//每一层
				//每一个神经细胞
				for(Neuron n:m_vecLayers.get(i).m_vecNeurons){
					for(int k=0;k<n.m_numInputs;k++){
						n.m_vecWeight.set(k, weights.get(cWeights++));
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
				for(Neuron n:m_vecLayers.get(i).m_vecNeurons){
					double netinput=0;
					int numInputs=n.m_numInputs;
					for(int k=0;k<numInputs-1;k++){
						netinput+=(n.m_vecWeight.get(k)*_inputs.get(cWeight++));
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
		//=====================精英选择=====================
		void grabNBest(int nBest,int numCopies,Vector<Genome> vecNewPop){
			while(nBest-->0){
				for(int i=0;i<numCopies;i++){
					vecNewPop.add(m_vecPop.get(m_iPopSize-1-nBest));
				}
			}
		}
		/**
		 * 锦标赛选择
		 * @return
		 */
		Genome tournamentSelection(int n){
			double bestFitnessSoFar=-BIG_NUMBER;
			int chosenOne=0;
			for(int i=0;i<n;i++){
				int thisTry=randInt(0, m_iPopSize-1);
				double thisTryFitness=m_vecPop.get(thisTry).m_dFitness;
				if(thisTryFitness>bestFitnessSoFar){
					chosenOne=thisTry;
					bestFitnessSoFar=thisTryFitness;
				}
			}
			return m_vecPop.get(chosenOne);
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
			Genome.sort(m_vecPop);
			calcBestWorstAvTot();
			Vector<Genome> vecNewPop=new Vector<Genome>();
			if((iNumCopiesElite*iNumElite)%2==0){
				grabNBest(iNumElite, iNumCopiesElite, vecNewPop);
			}
			while(vecNewPop.size()<m_iPopSize){
				Genome mum=tournamentSelection(TOURNAMENT_COMPETITORS);
				Genome dad=tournamentSelection(TOURNAMENT_COMPETITORS);
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
	static class CSmartMinesweeper{
		/**
		 * 地图记忆
		 */
		static class Mapper{
			/**
			 * 单元记忆
			 */
			static class Cell{
				int iTicksSpentHere;
				Rectangle rect=new Rectangle();
				Cell(int x,int y,int w,int h){
					rect.x=x;
					rect.y=y;
					rect.width=w;
					rect.height=h;
				}
				void update(){
					iTicksSpentHere++;
				}
				void reset(){
					iTicksSpentHere=0;
				}
			}
			Vector<Vector<Cell>> m_2DvecCells=new Vector<Vector<Cell>>();
			int m_numCellsX,m_numCellsY,m_iTotalCells,m_iCellSize;
			/**
			 * 
			 * @param maxRangeX
			 * @param maxRangeY
			 */
			void init(int maxRangeX,int maxRangeY){
				if(m_numCellsX>0){
					return;
				}
				m_iCellSize=CELL_SIZE;
				m_numCellsX=(maxRangeX/m_iCellSize)+1;
				m_numCellsY=(maxRangeY/m_iCellSize)+1;
				for(int x=0;x<m_numCellsX;x++){
					Vector<Cell> temp=new Vector<Cell>();
					for(int y=0;y<m_numCellsY;y++){
						temp.add(new Cell(x*m_iCellSize,y*m_iCellSize,
								m_iCellSize,m_iCellSize));
					}
					m_2DvecCells.add(temp);
				}
				m_iTotalCells=m_numCellsX*m_numCellsY;
			}
			/**
			 * @param xPos
			 * @param yPos
			 */
			void update(double xPos,double yPos){
				if((xPos<0)||(xPos>WINDOW_WIDTH)||
				   (yPos<0)||(yPos>WINDOW_HEIGHT)){
					return;
				}
				int cellX=(int)(xPos/m_iCellSize);
				int cellY=(int)(yPos/m_iCellSize);
				m_2DvecCells.get(cellX).get(cellY).update();
			}
			int ticksLingered(double xPos,double yPos){
				if((xPos<0)||(xPos>WINDOW_WIDTH)||
				   (yPos<0)||(yPos>WINDOW_HEIGHT)){
					return 999;
				}
				int cellX=(int)(xPos/m_iCellSize);
				int cellY=(int)(yPos/m_iCellSize);
				return m_2DvecCells.get(cellX).get(cellY).iTicksSpentHere;
			}
			int numCellsVisited(){
				int total=0;
				for(Vector<Cell> x:m_2DvecCells){
					for(Cell y:x){
						if(y.iTicksSpentHere>0){
							total++;
						}
					}
				}
				return total;
			}
			boolean beenVisited(double xPos,double yPos){
				int cellX=(int)(xPos/m_iCellSize);
				int cellY=(int)(yPos/m_iCellSize);
				if(m_2DvecCells.get(cellX).get(cellY).iTicksSpentHere>0){
					return true;
				}else{
					return false;
				}
			}
			void render(Graphics g){
				for(Vector<Cell> x:m_2DvecCells){
					for(Cell y:x){
						if(y.iTicksSpentHere>0){
							int shading=2*y.iTicksSpentHere;
							if(shading>220){
								shading=220;
							}
							g.setColor(new Color(240,220-shading,220-shading));
							g.fillRect(y.rect.x, y.rect.y, y.rect.width, y.rect.height);
						}
					}
				}
				g.setColor(Color.BLACK);
			}
			void reset(){
				for(Vector<Cell> x:m_2DvecCells){
					for(Cell y:x){
						y.reset();
					}
				}
			}
		}
		NeuralNet m_brain;
		Mapper m_memoryMap;//它的记忆
		Vector2D m_vPosition;//在世界中的位置
		Vector2D m_vLookAt;//朝向
		double m_dRotation;//旋转
		double m_dSpeed;//速度
		double m_lTrack,m_rTrack;//左右轮的速度
		double m_dFitness;//适应分
		double m_dScale;//被画时的大小比例
		//扫雷机形状顶点
		Vector<Point> m_sweeperVB=new Vector<Point>();
		//传感器顶点
		Vector<Point> m_sensorsVB=new Vector<Point>();
		Vector<Point> m_sensorsTrans;
		//保持传感器发生碰撞距离的记录
		Vector<Double> m_vecdSensors=new Vector<Double>();
		Vector<Double> m_vecFeelers=new Vector<Double>();
		private final ReentrantReadWriteLock rwl=new ReentrantReadWriteLock();
		private final Lock m_vecdSensorsReadLock=rwl.readLock();
		private final Lock m_vecdSensorsWriteLock=rwl.writeLock();
		Double getVecdSensors(int i){
			m_vecdSensorsReadLock.lock();
			try{
				return m_vecdSensors.get(i);
			}finally{
				m_vecdSensorsReadLock.unlock();
			}
		}
		int vecdSensorsSize(){
			m_vecdSensorsReadLock.lock();
			try{
				return m_vecdSensors.size();
			}finally{
				m_vecdSensorsReadLock.unlock();
			}
		}
		private final ReentrantReadWriteLock rwl1=new ReentrantReadWriteLock();
		private final Lock m_vecFeelersReadLock=rwl1.readLock();
		private final Lock m_vecFeelersWriteLock=rwl1.writeLock();
		Double getVecFeelers(int i){
			m_vecFeelersReadLock.lock();
			try{
				return m_vecFeelers.get(i);
			}finally{
				m_vecFeelersReadLock.unlock();
			}
		}
		boolean m_bCollided;
		double[][] sweeper={
			{-1,-1},{-1,1},{-0.5,1},{-0.5,-1},
			{0.5,-1},{1,-1},{1,1},{0.5,1},
			{-0.5,-0.5},{0.5,-0.5},
			{-0.5,0.5},{-0.25,0.5},{-0.25,1.75},{0.25,1.75},{0.25,0.5},{0.5,0.5}
		};
		CSmartMinesweeper(){
			m_brain=new NeuralNet();
			Random rand=new Random();
			m_dRotation=rand.nextFloat()*TWO_PI;
			m_dScale=SWEEPER_SCALE;
			m_vPosition=new Vector2D(180,200);
			m_vLookAt=new Vector2D();
			for(int i=0;i<sweeper.length;i++){
				double[] p=sweeper[i];
				m_sweeperVB.add(new Point(p[0],p[1]));
			}
			createSensors(NUM_SENSORS,SENSOR_RANGE);
			m_memoryMap=new Mapper();
			m_memoryMap.init(WINDOW_WIDTH, WINDOW_HEIGHT);
		}
		void createSensors(int numSensors,double range){
			m_sensorsVB.clear();
			double segmentAngle=PI/(numSensors-1);
			for(int i=0;i<numSensors;i++){
				double a=i*segmentAngle-HALF_PI;
				double x=-Math.sin(a)*range;
				double y=Math.cos(a)*range;
				m_sensorsVB.add(new Point(x,y));
			}
		}
		public String toString(){
			return "Fitness:"+m_dFitness+",m_brain:"+m_brain.getWeights();
		}
		void reset(){
			Random rand=new Random();
			m_vPosition=new Vector2D(180,200);
			m_dFitness=0;
			m_dRotation=rand.nextFloat()*TWO_PI;
			m_vLookAt=new Vector2D();
			m_memoryMap.reset();
		}
		/**
		 * 对扫雷机各顶点进行变换
		 * @param sweeper
		 */
		void worldTransform(Vector<Point> vecObjVB,double scale){
			Matrix2D m2=new Matrix2D();
			m2.scale(scale, scale);
			m2.rotate(m_dRotation);
			m2.translate(m_vPosition.x, m_vPosition.y);
			m2.transformPoints(vecObjVB);
		}
		/**
		 * 利用从扫雷机环境得到的信息来更新人工神经网络
		 * @param mines
		 * @return
		 */
		boolean update(Vector<Point> obstacle){
			Vector<Double> inputs=new Vector<Double>();
			testSensors(obstacle);
			m_vecdSensorsReadLock.lock();
			m_vecFeelersReadLock.lock();
			for(int sr=0;sr<m_vecdSensors.size();sr++){
				inputs.add(m_vecdSensors.get(sr));
				inputs.add(m_vecFeelers.get(sr));
			}
			m_vecdSensorsReadLock.unlock();
			m_vecFeelersReadLock.unlock();
			inputs.add((double)(m_bCollided?1:0));
			Vector<Double> output=m_brain.update(inputs);
			if(output.size()<NUM_OUTPUTS){
				return false;
			}
			m_lTrack=output.get(0);
			m_rTrack=output.get(1);
			double rotForce=m_lTrack-m_rTrack;
			rotForce=clamp(rotForce, -MAX_TURN_RATE, MAX_TURN_RATE);
			m_dRotation+=rotForce;
			m_vLookAt.x=-Math.sin(m_dRotation);
			m_vLookAt.y=Math.cos(m_dRotation);
			if(!m_bCollided){
				m_dSpeed=m_lTrack+m_rTrack;
				m_vPosition.add(Vector2D.multiply(m_vLookAt, m_dSpeed));
			}
			m_memoryMap.update(m_vPosition.x, m_vPosition.y);
			return true;
		}
		private void testSensors(Vector<Point> obstacle) {
			m_bCollided=false;
			m_sensorsTrans=clonePointVector(m_sensorsVB);
			worldTransform(m_sensorsTrans, 1);
			m_vecdSensorsWriteLock.lock();
			m_vecFeelersWriteLock.lock();
			m_vecdSensors.clear();
			m_vecFeelers.clear();
			for(int sr=0;sr<m_sensorsTrans.size();sr++){
				boolean bHit=false;
				double[] dist=new double[1];
				for(int seg=0;seg<obstacle.size();seg+=2){
					if(lineIntersect2D(new Point(m_vPosition.x,m_vPosition.y),
						m_sensorsTrans.get(sr),obstacle.get(seg),
						obstacle.get(seg+1),dist)){
						bHit=true;
						break;
					}
				}
				if(bHit){
					m_vecdSensors.add(dist[0]);
					if(dist[0]<COLLISION_DIST){
						m_bCollided=true;
					}
				}else{
					m_vecdSensors.add(-1d);
				}
				int howOften=m_memoryMap.ticksLingered(m_sensorsTrans.get(sr).x,
						m_sensorsTrans.get(sr).y);
				if(howOften==0){
					m_vecFeelers.add(-1d);
					continue;
				}
				if(howOften<10){
					m_vecFeelers.add(0d);
					continue;
				}
				if(howOften<20){
					m_vecFeelers.add(0.2);
					continue;
				}
				if(howOften<30){
					m_vecFeelers.add(0.4);
					continue;
				}
				if(howOften<50){
					m_vecFeelers.add(0.6);
					continue;
				}
				if(howOften<80){
					m_vecFeelers.add(0.8);
					continue;
				}
				m_vecFeelers.add(1d);
			}
			m_vecdSensorsWriteLock.unlock();
			m_vecFeelersWriteLock.unlock();
		}
		private boolean lineIntersect2D(Point A, Point B, 
				Point C, Point D, double[] dist) {
			double rTop=(A.y-C.y)*(D.x-C.x)-(A.x-C.x)*(D.y-C.y);
			double rBot=(B.x-A.x)*(D.y-C.y)-(B.y-A.y)*(D.x-C.x);
			double sTop=(A.y-C.y)*(B.x-A.x)-(A.x-C.x)*(B.y-A.y);
			double sBot=(B.x-A.x)*(D.y-C.y)-(B.y-A.y)*(D.x-C.x);
			if((rBot==0)||(sBot==0)){//线段平行
				return false;
			}
			double r=rTop/rBot;
			double s=sTop/sBot;
			if((r>0)&&(r<1.0f)&&(s>0)&&(s<1.0f)){
				dist[0]=r;
				return true;
			}else{
				return false;
			}
		}
		void endOfRunCalc(){
			m_dFitness+=m_memoryMap.numCellsVisited();
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
			worldTransform(sweeperVB, m_dScale);
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
		public void renderMemoryMap(Graphics g){
			m_memoryMap.render(g);
			String s="Num Cells Visited:"+m_memoryMap.numCellsVisited();
			g.drawString(s, 230, 12);
		}
	}
	static class Controller implements Runnable{
		Vector<Genome> m_vecPop;
		Vector<CSmartMinesweeper> m_vecSweepers=new Vector<CSmartMinesweeper>();
		GenAlg m_pGA;
		int m_numSweepers;
		int m_numWeightsInNN;//神经网络中使用的权重值总数
		Vector<Point> m_obstacleVB=new Vector<Point>();//障碍物形状顶点
		//每一代的平均适应分数
		Vector<Double> m_vecAvFitness=new Vector<Double>();
		//每一代的最高适应分数
		Vector<Double> m_vecBestFitness=new Vector<Double>();
		boolean m_bFastRender;
		int m_iTicks;//每一代的帧数
		int m_iGeneration;
		int m_cxClient,m_cyClient;
		boolean m_bStarted;
		static final double[][] obstacle={
			{80,60},{200,60},{200,60},{200,100},{200,100},{160,100},
			{160,100},{160,200},{160,200},{80,200},{80,200},{80,60},
			
			{250,100},{300,40},{300,40},{350,100},{350,100},{250,100},
			
			{220,180},{320,180},{320,180},{320,300},{320,300},
			{220,300},{220,300},{220,180},
			
			{12,15},{380,15},{380,15},{380,360},{380,360},
			{12,360},{12,360},{12,340},{12,340},{100,290},
			{100,290},{12,240},{12,240},{12,15}
		};
		public Controller() {
			m_numSweepers=NUM_SWEEPERS;
			m_cxClient=WINDOW_WIDTH;
			m_cyClient=WINDOW_HEIGHT;
			for(int i=0;i<m_numSweepers;i++){
				m_vecSweepers.add(new CSmartMinesweeper());
			}
			m_numWeightsInNN=m_vecSweepers.get(0).getNumOfWeights();
			Vector<Integer> splitPoints=m_vecSweepers.get(0).calcSplitPoints();
			m_pGA=new GenAlg(MUTATION_RATE,CROSSOVER_RATE,m_numSweepers,
					m_numWeightsInNN,splitPoints);
			m_vecPop=m_pGA.m_vecPop;
			for(int i=0;i<m_numSweepers;i++){
				m_vecSweepers.get(i).putWeights(m_vecPop.get(i).m_vecWeights);
			}
			for(double[] p:obstacle){
				m_obstacleVB.add(new Point(p[0],p[1]));
			}
		}
		boolean update(){
			if(m_iTicks++<NUM_TICKS){
				for(int i=0;i<m_numSweepers;i++){
					if(!m_vecSweepers.get(i).update(m_obstacleVB)){
						pln("Wrong amount of NN inputs!");
						return false;
					}
				}
			}else{
				int bestCellCoverage=0;
				for(int swp=0;swp<m_vecSweepers.size();swp++){
					m_vecSweepers.get(swp).endOfRunCalc();
					m_vecPop.get(swp).m_dFitness=m_vecSweepers.get(swp).m_dFitness;
					int numCellsVisited=m_vecSweepers.get(swp).m_memoryMap.numCellsVisited();
					if(numCellsVisited>bestCellCoverage){
						bestCellCoverage=numCellsVisited;
					}
				}
				m_vecAvFitness.add(m_pGA.m_dAverageFitness);
				m_vecBestFitness.add(m_pGA.m_dBestFitness);
				m_iGeneration++;
				m_iTicks=0;
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
			g.drawString("Generation:"+m_iGeneration, 5, 12);
			if(!m_bFastRender){
				m_vecSweepers.get(m_numSweepers-1).renderMemoryMap(g);
				for(int v=0;v<m_obstacleVB.size();v+=2){
					g.drawLine((int)m_obstacleVB.get(v).x, (int)m_obstacleVB.get(v).y,
							(int)m_obstacleVB.get(v+1).x, (int)m_obstacleVB.get(v+1).y);
				}
				for(int i=0;i<m_numSweepers;i++){
					if(i>m_numSweepers-iNumElite-1){
						g.setColor(Color.BLUE);
					}else{
						g.setColor(new Color(220,220,220));
						setDottedStroke(g);
					}
					if(m_vecSweepers.get(i).m_bCollided&&i>m_numSweepers-iNumElite-1){
						g.setColor(Color.RED);
					}
					if(m_vecSweepers.get(i).m_bCollided&&i<m_numSweepers-iNumElite-1){
						g.setColor(Color.RED);
						setDottedStroke(g);
					}
					m_vecSweepers.get(i).render(g);
				}
				for(int i=0;i<iNumElite;i++){//render the sensors
					int ii=m_numSweepers-iNumElite+i;
					Vector<Point> sensorsTrans=m_vecSweepers.get(ii).m_sensorsTrans;
					int stSize=m_vecSweepers.get(ii).vecdSensorsSize();
					for(int sr=0;sr<stSize;sr++){
						if(m_vecSweepers.get(ii).getVecdSensors(sr)>0){
							g.setColor(Color.RED);
						}else{
							g.setColor(new Color(220,220,220));
							setDottedStroke(g);
						}
						Vector2D pos=m_vecSweepers.get(ii).m_vPosition;
						if(!(Math.abs(pos.x-sensorsTrans.get(sr).x)>(SENSOR_RANGE+1))
								||(Math.abs(pos.y-sensorsTrans.get(sr).y)>(SENSOR_RANGE+1))){
							g.drawLine((int)pos.x, (int)pos.y,
									(int)sensorsTrans.get(sr).x, (int)sensorsTrans.get(sr).y);
							Rectangle rect=new Rectangle();
							rect.x=(int)sensorsTrans.get(sr).x-2;
							rect.width=4;
							rect.y=(int)sensorsTrans.get(sr).y-2;
							rect.height=4;
							if(m_vecSweepers.get(ii).getVecFeelers(sr)<0){
								g.setColor(Color.BLUE);
							}else{
								g.setColor(Color.RED);
							}
							Graphics2D g2=(Graphics2D)g;
							g2.fill(rect);
						}
					}
				}
			}else{
				plotStats(g);
			}
		}
		private void setDottedStroke(Graphics g) {
			Graphics2D g2=(Graphics2D)g;
			g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
					BasicStroke.JOIN_MITER,10.0f,new float[]{5,5},0.0f));
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
		@Override
		public void run() {
			Timer t=new Timer(FRAMES_PER_SECOND);
			t.start();
			while(m_bStarted){
				if(t.readyForNextFrame()||m_bFastRender){
					m_bStarted=update();
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
		while(c.m_bStarted){
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
    	final SmartMinesweeper m=new SmartMinesweeper();
    	f.setContentPane(m);
    	m.c=new Controller();
    	f.addKeyListener(new KeyAdapter(){
    		public void keyPressed(final KeyEvent ke){
    			new Thread(new Runnable(){
					@Override
					public void run() {
						if(ke.getKeyCode()==KeyEvent.VK_ENTER){
							if(!m.c.m_bStarted){
								m.c.m_bStarted=true;
								new Thread(m.c).start();
								m.toDraw();
							}
						}else if(ke.getKeyCode()==KeyEvent.VK_SPACE){
							m.c.m_bStarted=false;
						}else if(ke.getKeyCode()==KeyEvent.VK_F){
							m.c.fastRenderToggle();
						}
					}
    			}).start();
    		}
    	});
	}
}
