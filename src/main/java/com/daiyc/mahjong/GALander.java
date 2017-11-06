/**
 * 
 */
package com.daiyc.mahjong;

import static com.daiyc.mahjong.AICommon.BIG_NUMBER;
import static com.daiyc.mahjong.AICommon.FRAMES_PER_SECOND;
import static com.daiyc.mahjong.AICommon.PI;
import static com.daiyc.mahjong.AICommon.TWO_PI;
import static com.daiyc.mahjong.AICommon.clamp;
import static com.daiyc.mahjong.AICommon.clonePointVector;
import static com.daiyc.mahjong.AICommon.randClamped;
import static com.daiyc.mahjong.AICommon.randInt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import com.daiyc.mahjong.GALander.Ga.Gene;
import com.daiyc.mahjong.GALander.Ga.Genome;
/**
 * 遗传算法-月球登陆器
 * @author Administrator
 */
public class GALander extends JPanel{
	private static final long serialVersionUID = 1L;
	final static int ROTATE_LEFT=0;
	final static int ROTATE_RIGHT=1;
	final static int THRUST=2;
	final static int NON=3;
	final static double GRAVITY=-1.63;
	final static double THRUST_PER_SECOND=350.0;
	final static double ROTATION_PER_SECOND=3.0;
	final static double GRAVITY_PER_TICK=GRAVITY/FRAMES_PER_SECOND;
	final static double THRUST_PER_TICK=THRUST_PER_SECOND/FRAMES_PER_SECOND;
	final static double ROTATION_PER_TICK=ROTATION_PER_SECOND/FRAMES_PER_SECOND;
	final static double DIST_TOLERANCE=10.0;
	final static double SPEED_TOLERANCE=0.5;
	final static double ROTATION_TOLERANCE=Math.PI/16;
	final static int MAX_ACTION_DURATION=30;
    static final int WINDOW_WIDTH=400;
    static final int WINDOW_HEIGHT=400;
    static final int MAX_MUTATION_DURATION=MAX_ACTION_DURATION/2;
    static final double SHIPS_MASS=100.0;
    static final double LANDER_SCALE=10;
    static final int SCALING_FACTOR=60;
    static final double MUTATION_RATE=0.01;
    static final double CROSSOVER_RATE=0.7;
    static int NUM_COPIES_ELITE=1;
    static int NUM_ELITE=4;
    static final int POP_SIZE=100;
    static final int CHROMO_LENGTH=30;
    static final int MAX_GENERATIONS_ALLOWED=500;
    static final int NUM_STARS=20;
	static Vector<Genome> cloneGenomeVector(Vector<Genome> src){
		Vector<Genome> dst=new Vector<Genome>();
		for(Genome g:src){
			dst.add((Genome)(g.clone()));
		}
		return dst;
	}
	/**
	 * 遗传算法
	 */
	static class Ga{
		/**
		 * 基因
		 */
		static class Gene{
			int action;//LEFT:0,RIGHT:1,THRUST:2,NON:3
			int duration;
			/**
			 * 生成随机的基因
			 */
			Gene(){
				this(randInt(0,3),randInt(1, MAX_ACTION_DURATION));
			}
			Gene(int action,int duration){
				this.action=action;
				this.duration=duration;
			}
			public boolean equals(Object rhs){
				Gene rhsG=null;
				if(rhs instanceof Gene){
					rhsG=(Gene)rhs;
				}
				return (action==rhsG.action)&&(duration==rhsG.duration);
			}
			public Object clone(){
				return new Gene(action,duration);
			}
		}
		/**
		 * 染色体
		 */
		static class Genome{
			Vector<Gene> vecActions=new Vector<Gene>();
			double dFitness;
			Genome(){}
			Genome(int numActions){
				for(int i=0;i<numActions;i++){
					vecActions.add(new Gene());
				}
			}
			public Object clone(){
				Genome gm=new Genome();
				gm.dFitness=dFitness;
				gm.vecActions=new Vector<Gene>();
				for(Gene ge:vecActions){
					gm.vecActions.add((Gene)ge.clone());
				}
				return gm;
			}
			private static class FitnessCmp implements Comparator<Genome>{
				@Override
				public int compare(Genome g1, Genome g2) {
					double d=g1.dFitness-g2.dFitness;
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
		Vector<Genome> m_vecPop=new Vector<Genome>();//种群
		int m_iPopSize;//种群中染色体的数目
		int m_iChromoLen;//染色体长度
		int m_iFittestGenome;//最新一代中适应分最高的
		double m_dMutationRate;//突变率
		double m_dCrossoverRate;//杂交率
		int m_iGeneration;//哪一代
		double m_dBestFitness;//最好的适应分
		double m_dWorstFitness;//最差的适应分
		double m_dTotalFitness;//种群适应分总分
		double m_dAverageFitness;//种群平均分
		Ga(double mutRate,double crossRat,int popSize,int numActions){
			m_dMutationRate=mutRate;
			m_dCrossoverRate=crossRat;
			m_iPopSize=popSize;
			m_iChromoLen=numActions;
		}
		void createStartPop(){
			m_vecPop.clear();
			for(int i=0;i<m_iPopSize;i++){
				m_vecPop.add(new Genome(m_iChromoLen));
			}
			m_iGeneration=0;
			m_dBestFitness=0;
			m_dWorstFitness=BIG_NUMBER;
			m_dTotalFitness=0;
			m_dAverageFitness=0;
			m_iFittestGenome=0;
		}
		void calcBestWorstAvTot(){
			m_dTotalFitness=0;
			for(Genome g:m_vecPop){
				m_dTotalFitness+=g.dFitness;
			}
			m_dAverageFitness=m_dTotalFitness/m_iPopSize;
			m_iFittestGenome=m_iPopSize-1;
			m_dBestFitness=m_vecPop.get(m_iFittestGenome).dFitness;
			m_dWorstFitness=m_vecPop.get(0).dFitness;
		}
		void grabNBest(int nBest,int numCopies,Vector<Genome> vecPop){
			while(nBest-->0){
				for(int i=0;i<numCopies;i++){
					vecPop.add(m_vecPop.get(m_iPopSize-1-nBest));
				}
			}
		}
		Genome rouletteWheelSelection(){
			double slice=new Random().nextFloat()*m_dTotalFitness;
			double fitnessSoFar=0;
			for(Genome g:m_vecPop){
				fitnessSoFar+=g.dFitness;
				if(fitnessSoFar>slice){
					return g;
				}
			}
			return m_vecPop.get(0);
		}
		void crossoverMulti(Vector<Gene> mum,Vector<Gene> dad,
				Vector<Gene> baby1,Vector<Gene> baby2){
			Random rand=new Random();
			if(rand.nextFloat()>m_dCrossoverRate||mum.equals(dad)){
				baby1.addAll(mum);
				baby2.addAll(dad);
				return;
			}
			float swapRate=rand.nextFloat()*m_iChromoLen;
			for(int g=0;g<mum.size();g++){
				if(rand.nextFloat()<swapRate){
					baby1.add(dad.get(g));
					baby2.add(mum.get(g));
				}else{
					baby1.add(mum.get(g));
					baby2.add(dad.get(g));
				}
			}
		}
		void mutate(Vector<Gene> vecBits){
			Random rand=new Random();
			for(Gene g:vecBits){
				if(rand.nextFloat()<m_dMutationRate){
					g.action=randInt(0, 3);
				}
				if(rand.nextFloat()<m_dMutationRate/2){
					g.duration+=randClamped()*MAX_MUTATION_DURATION;
					g.duration+=clamp(g.duration, 0, MAX_ACTION_DURATION);
				}
			}
		}
		Vector<Genome> epoch(){
			Genome.sort(m_vecPop);
			calcBestWorstAvTot();
			Vector<Genome> vecNewPop=new Vector<Genome>();
			if((NUM_COPIES_ELITE*NUM_ELITE)%2==0){
				grabNBest(NUM_ELITE, NUM_COPIES_ELITE, vecNewPop);
			}
			while(vecNewPop.size()<m_iPopSize){
				Genome mum=rouletteWheelSelection();
				Genome dad=rouletteWheelSelection();
				Genome baby1=new Genome();
				Genome baby2=new Genome();
				crossoverMulti(mum.vecActions, dad.vecActions, 
						baby1.vecActions, baby2.vecActions);
				mutate(baby1.vecActions);
				mutate(baby2.vecActions);
				vecNewPop.add(baby1);
				vecNewPop.add(baby2);
			}
			m_vecPop=vecNewPop;
			m_iGeneration++;
			return m_vecPop;
		}
		void updatePop(Vector<Genome> vOldPop){
			m_vecPop=cloneGenomeVector(vOldPop);
			vOldPop.clear();
			vOldPop.addAll(epoch());
		}
	}
    /**
     * 登陆器
     */
    static class Lander{
    	Vector2D m_vPos;//在世界坐标系中的位置
    	double m_dRotation;//在世界坐标系中的旋转
    	double m_dMass;//飞船的质量
    	Vector2D m_vVelocity;//飞船的速度
    	Vector2D m_vPadPos;//为了碰撞检测，需要知道登陆地点
    	Vector2D m_vStartPos;//起始位置
    	double m_dStartRotation;//其实角度
    	double m_dScale;//为绘制飞船所需的比例因子
    	Vector<Point> m_vecShipVB=new Vector<Point>();//飞船顶点的缓冲区
    	Vector<Point> m_vecShipVBTrans;//飞船坐标变换后的顶点缓冲区
    	Vector<Point> m_vecJetVB=new Vector<Point>();//喷气外形的顶点
    	Vector<Point> m_vecJetVBTrans;
    	boolean m_bJetOn;//是否显示喷气
    	Vector<Integer> m_vecActions=new Vector<Integer>();//从飞船基因组解码的一系列动作
    	int m_cTick;//动作（帧数）计算器，能告诉我们当前的动作
    	int m_cxClient;//窗体尺寸
    	int m_cyClient;
    	double m_dFitness;
    	int m_iPadX;//登陆点水平位置
    	boolean m_bCheckedIfLanded;//是否已经作为成功测试
    	final static double[][] lander={
    		//middle of lander
    		{-1,0},{1,0},{1,-0.5},{-1,-0.5},
    		//top of lander
    		{-0.5,0},{-1,0.3},{-1,0.7},{-0.5,1},{0.5,1},{1,0.7},{1,0.3},{0.5,0},
    		//legs
    		{-1,-0.4},{-1.3,-0.8},{-1.3,-1.2},{-1.5,-1.2},{-1.1,-1.2},{-0.9,-0.5},{-1.3,-0.8},
    		{1,-0.4},{1.3,-0.8},{1.3,-1.2},{1.5,-1.2},{1.1,-1.2},{0.9,-0.5},{1.3,-0.8},
    		//rocket
    		{-0.2,-0.5},{-0.3,-0.8},{0.3,-0.8},{0.2,-0.5}
    	};
    	final static double[][] jet={
    		{-0.1,-0.9},{-0.2,-1.2},{0,-1.6},{0.2,-1.2},{0.1,-0.9}	
    	};
    	Lander(int cxClient,int cyClient,double rot,Vector2D pos,Vector2D pad){
    		m_cxClient=cxClient;
    		m_cyClient=cyClient;
    		m_dRotation=rot;
    		m_vPos=pos;
    		m_vPadPos=pad;
    		m_vVelocity=new Vector2D(0,0);
    		m_dMass=SHIPS_MASS;
    		m_dScale=LANDER_SCALE;
    		m_iPadX=cxClient/2;
    		m_vStartPos=m_vPos.clone();
    		m_dStartRotation=m_dRotation;
    		for(double[] p:lander){
    			m_vecShipVB.add(new Point(p[0],p[1]));
    		}
    		for(double[] p:jet){
    			m_vecJetVB.add(new Point(p[0],p[1]));
    		}
    	}
    	/**
    	 * 检测船的任何顶点是否在登陆平台水平面的下面
    	 * @param ship
    	 * @return
    	 */
    	boolean testForImpact(Vector<Point> ship){
    		for(Point p:ship){
    			if(p.y<55){
    				return true;
    			}
    		}
    		return false;
    	}
    	/**
    	 * 变换飞船顶点到世界坐标以便显示
    	 * @param ship
    	 */
    	void worldTransform(Vector<Point> ship){
    		Matrix2D m2=new Matrix2D();
    		m2.scale(m_dScale, m_dScale);
    		m2.rotate(-m_dRotation);
    		m2.translate(m_vPos.x, m_vPos.y);
    		m2.transformPoints(ship);
    	}
    	/**
    	 * 为开始一个新尝试重置所有相关参数
    	 * @param newPadPos
    	 */
    	void reset(Vector2D newPadPos){
    		m_vPos=m_vStartPos.clone();
    		m_dRotation=m_dStartRotation;
    		m_vVelocity.x=0;
    		m_vVelocity.y=0;
    		m_cTick=0;
    		m_dFitness=0;
    		m_vPadPos=newPadPos;
    		m_bCheckedIfLanded=false;
    	}
    	void render(Graphics g){
    		if(m_vecShipVBTrans==null){
    			return;
    		}
    		int v;
    		//lander base
    		for(v=0;v<3;v++){
    			g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
    					(int)m_vecShipVBTrans.get(v+1).x,(int)m_vecShipVBTrans.get(v+1).y);
    		}
    		g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
					(int)m_vecShipVBTrans.get(0).x,(int)m_vecShipVBTrans.get(0).y);
    		//lander top
    		for(v=4;v<11;v++){
    			g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
    					(int)m_vecShipVBTrans.get(v+1).x,(int)m_vecShipVBTrans.get(v+1).y);
    		}
    		//left leg
    		v=12;
    		g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
					(int)m_vecShipVBTrans.get(v+1).x,(int)m_vecShipVBTrans.get(v+1).y);
    		v=13;
    		g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
					(int)m_vecShipVBTrans.get(v+1).x,(int)m_vecShipVBTrans.get(v+1).y);
    		v=15;
    		g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
					(int)m_vecShipVBTrans.get(v+1).x,(int)m_vecShipVBTrans.get(v+1).y);
    		v=17;
    		g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
					(int)m_vecShipVBTrans.get(v+1).x,(int)m_vecShipVBTrans.get(v+1).y);
    		//right leg
    		v=19;
    		g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
					(int)m_vecShipVBTrans.get(v+1).x,(int)m_vecShipVBTrans.get(v+1).y);
    		v=20;
    		g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
					(int)m_vecShipVBTrans.get(v+1).x,(int)m_vecShipVBTrans.get(v+1).y);
    		v=22;
    		g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
					(int)m_vecShipVBTrans.get(v+1).x,(int)m_vecShipVBTrans.get(v+1).y);
    		v=24;
    		g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
					(int)m_vecShipVBTrans.get(v+1).x,(int)m_vecShipVBTrans.get(v+1).y);
    		//the burner
    		for(v=26;v<29;v++){
    			g.drawLine((int)m_vecShipVBTrans.get(v).x,(int)m_vecShipVBTrans.get(v).y,
    					(int)m_vecShipVBTrans.get(v+1).x,(int)m_vecShipVBTrans.get(v+1).y);
    		}
    		if(m_bJetOn){
    			m_vecJetVBTrans=clonePointVector(m_vecJetVB);
    			worldTransform(m_vecJetVBTrans);
    			for(v=0;v<m_vecJetVBTrans.size()-1;v++){
    				g.drawLine((int)m_vecJetVBTrans.get(v).x,(int)m_vecJetVBTrans.get(v).y,
        					(int)m_vecJetVBTrans.get(v+1).x,(int)m_vecJetVBTrans.get(v+1).y);
    			}
    		}
    	}
    	/**
    	 * 根据解码基因更新飞船
    	 * @param timeElapsed
    	 */
    	boolean updateShip(){
    		if(m_bCheckedIfLanded){//如果飞船已经坠毁或着陆
    			return false;
    		}
    		int action;
    		if(m_cTick>=m_vecActions.size()){
    			action=NON;
    		}else{
    			action=m_vecActions.get(m_cTick++);
    		}
    		m_bJetOn=false;
    		switch(action){
    		case ROTATE_LEFT:
    			m_dRotation-=ROTATION_PER_TICK;
    			if(m_dRotation<-PI){
    				m_dRotation+=TWO_PI;
    			}
    			break;
    		case ROTATE_RIGHT:
    			m_dRotation+=ROTATION_PER_TICK;
    			if(m_dRotation>TWO_PI){
    				m_dRotation-=TWO_PI;
    			}
    			break;
    		case THRUST:
    			double shipAcc=THRUST_PER_TICK/m_dMass;
    			m_vVelocity.x+=shipAcc*Math.sin(m_dRotation);
    			m_vVelocity.y+=shipAcc*Math.cos(m_dRotation);
    			m_bJetOn=true;//打开喷气
    			break;
    		case NON:
    			break;
    		}
    		m_vVelocity.y+=GRAVITY_PER_TICK;
    		m_vPos.add(m_vVelocity);
    		if(m_vPos.x>WINDOW_WIDTH){
    			m_vPos.x=0;
    		}
    		if(m_vPos.x<0){
    			m_vPos.x=WINDOW_WIDTH;
    		}
    		m_vecShipVBTrans=clonePointVector(m_vecShipVB);
    		worldTransform(m_vecShipVBTrans);
    		if(testForImpact(m_vecShipVBTrans)){
    			if(!m_bCheckedIfLanded){
    				if(m_dFitness==0){
    					calcFitness();
    					m_bCheckedIfLanded=true;	
    				}
    				return false;
    			}
    		}
    		return true;
    	}
		void calcFitness(){
			double distFromPad=Math.abs(m_vPadPos.x-m_vPos.x);
			double distFit=m_cxClient-distFromPad;
			double speed=Vector2D.vec2DLen(m_vVelocity);
			double rotFit=1/(Math.abs(m_dRotation)+1);
			double airTimeFit=(double)m_cTick/(speed+1);
			m_dFitness=distFit+400*rotFit+4*airTimeFit;
			if(distFromPad<DIST_TOLERANCE && speed<SPEED_TOLERANCE
					&& Math.abs(m_dRotation)<ROTATION_TOLERANCE){
				m_dFitness=BIG_NUMBER;
			}
		}
		void decode(Vector<Gene> actions){
			m_vecActions.clear();
			for(Gene g:actions){
				for(int j=0;j<g.duration;j++){
					m_vecActions.add(g.action);
				}
			}
		}
    }
    static class Controller implements Runnable{
    	Ga m_pGA;
    	Vector<Lander> m_vecLanders=new Vector<Lander>();//登月飞船种群
    	Vector<Genome> m_vecPop=new Vector<Genome>();//基因种群
    	boolean m_bSuccess;//是否成功登陆
    	Vector<Point> m_vecStarVB=new Vector<Point>();//星星顶点
    	Vector<Point> m_vecPadVB=new Vector<Point>();//登陆点形状顶点
    	Vector2D m_vPadPos;//登陆点的位置
    	int m_cxClient,m_cyClient;//窗口尺寸
    	int m_iGeneration;
    	int m_iFittest;
    	boolean m_bStarted;
    	boolean m_bShowFittest;
    	boolean m_bFastRender;
    	double[][] pad={
    		{-20,0},{20,0},{20,5},{-20,5}
    	};
    	Controller(){
    		this(WINDOW_WIDTH,WINDOW_HEIGHT);
    	}
    	Controller(int cxClient,int cyClient){
    		m_cxClient=cxClient;
    		m_cyClient=cyClient;
    		m_pGA=new Ga(MUTATION_RATE,CROSSOVER_RATE,POP_SIZE,CHROMO_LENGTH);
    		m_pGA.createStartPop();
    		m_vecPop=m_pGA.m_vecPop;
    		Random rand=new Random();
    		m_vPadPos=new Vector2D(50+rand.nextFloat()*(m_cxClient-100),50);
    		Vector2D vStartPos=new Vector2D(rand.nextFloat()*m_cxClient,m_cyClient-50);
    		for(int i=0;i<POP_SIZE;i++){
    			m_vecLanders.add(new Lander(m_cxClient,m_cyClient,PI,vStartPos,m_vPadPos));
    			m_vecLanders.get(i).decode(m_vecPop.get(i).vecActions);
    		}
    		for(double[] p:pad){
    			m_vecPadVB.add(new Point(p[0],p[1]));
    		}
    		for(int i=0;i<NUM_STARS;i++){
    			m_vecStarVB.add(new Point(randInt(0,m_cxClient),randInt(100, m_cyClient)));
    		}
    	}
    	/**
    	 * 新一轮的执行
    	 */
    	void newRun(){
    		m_pGA=new Ga(MUTATION_RATE,CROSSOVER_RATE,POP_SIZE,CHROMO_LENGTH);
    		m_pGA.createStartPop();
    		m_vecPop=m_pGA.m_vecPop;
    		Random rand=new Random();
    		m_vPadPos=new Vector2D(50+rand.nextFloat()*(m_cxClient-100),50);
    		for(int i=0;i<POP_SIZE;i++){
    			m_vecLanders.get(i).reset(m_vPadPos);
    			m_vecLanders.get(i).decode(m_vecPop.get(i).vecActions);
    		}
    		m_iGeneration=0;
    		m_iFittest=0;
    		m_bSuccess=false;
    	}
    	void worldTransform(Vector<Point> pad){
    		Matrix2D m2=new Matrix2D();
    		m2.translate(m_vPadPos.x, m_vPadPos.y);
    		m2.transformPoints(pad);
    	}
    	boolean bAllFinished=false;
    	boolean update(){
    		if(m_iGeneration>MAX_GENERATIONS_ALLOWED){
    			newRun();
    		}
    		if(!bAllFinished){
    			bAllFinished=true;
    			for(Lander l:m_vecLanders){
    				if(l.updateShip()){
    					bAllFinished=false;
    				}
    			}
    		}else{
    			m_iFittest=0;
    			double bestScoreSoFar=0;
    			for(int i=0;i<POP_SIZE;i++){
    				m_vecPop.get(i).dFitness=m_vecLanders.get(i).m_dFitness;
    				if(m_vecPop.get(i).dFitness>bestScoreSoFar){
    					bestScoreSoFar=m_vecPop.get(i).dFitness;
    					m_iFittest=i;
    				}
    				if(m_vecPop.get(i).dFitness>=BIG_NUMBER && !m_bSuccess){
    					m_bSuccess=true;
    					success();
    				}
    				m_vecLanders.get(i).reset(m_vPadPos);
    			}
    			bAllFinished=false;
    			if(!m_bSuccess){
    				m_pGA.updatePop(m_vecPop);
    				for(int i=0;i<POP_SIZE;i++){
    					m_vecLanders.get(i).decode(m_vecPop.get(i).vecActions);
    				}
    				m_iGeneration++;
    			}
    		}
    		return true;
    	}
    	private void success(){
    		if(!m_bShowFittest){
    			m_bShowFittest=true;
    		}
    		if(m_bFastRender){
    			m_bFastRender=false;
    		}
    	}
    	void toggleShowFittest(){
    		m_bShowFittest=!m_bShowFittest;
    	}
    	void toggleFastRender(){
    		m_bFastRender=!m_bFastRender;
    	}
    	void render(Graphics g){
    		g.setColor(Color.BLACK);
    		g.fillRect(0, 0, m_cxClient, m_cyClient);
    		Graphics2D g2=(Graphics2D)g;
    		g2.translate(m_cxClient, m_cyClient);
    		g2.rotate(Math.toRadians(-180));
    		g.setColor(Color.LIGHT_GRAY);
    		Random rand=new Random();
    		for(int i=0;i<m_vecStarVB.size();i++){
    			if(rand.nextFloat()>0.1){
    				g.drawOval((int)m_vecStarVB.get(i).x, (int)m_vecStarVB.get(i).y,
    						1, 1);
    			}
    		}
    		if(!m_bShowFittest){
    			for(Lander l:m_vecLanders){
    				l.render(g);
    			}
    		}else{
    			m_vecLanders.get(m_iFittest).render(g);
    		}
    		renderLandingPad(g);
    		g2.rotate(Math.toRadians(180));
    		g2.translate(-m_cxClient, -m_cyClient);
    		int y=15;
    		g.drawString("回车:开始", 10, y);
    		g.drawString("R键:新的一轮", 75, y);
    		g.drawString("F键:切换快速渲染", 160, y);
    		g.drawString("B键:显示最佳的", 270, y);
    		g.drawString("Generation:"+m_iGeneration, 10, y+20);
    		if(m_bSuccess && rand.nextFloat()>0.1){
    			String s="漂亮的着陆!";
    			g.drawString(s, m_cxClient/2-g.getFontMetrics().stringWidth(s)/2, 160);
    		}
    	}
    	void renderLandingPad(Graphics g){
    		Vector<Point> vecPadVBTrans=clonePointVector(m_vecPadVB);
    		worldTransform(vecPadVBTrans);
    		int v;
    		for(v=0;v<vecPadVBTrans.size()-1;v++){
    			g.drawLine((int)vecPadVBTrans.get(v).x, (int)vecPadVBTrans.get(v).y,
    					(int)vecPadVBTrans.get(v+1).x, (int)vecPadVBTrans.get(v+1).y);
    		}
    		g.drawLine((int)vecPadVBTrans.get(v).x, (int)vecPadVBTrans.get(v).y,
					(int)vecPadVBTrans.get(0).x, (int)vecPadVBTrans.get(0).y);
    	}
		@Override
		public void run() {
			Timer t=new Timer(FRAMES_PER_SECOND);
			t.start();
			while(m_bStarted){
				if(t.readyForNextFrame() || m_bFastRender){
					update();
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
    public static void main(String[] args){
    	JFrame f=new JFrame();
    	f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    	f.setBounds(500, 150, WINDOW_WIDTH, WINDOW_HEIGHT);
    	f.setVisible(true);
    	final GALander gl=new GALander();
    	f.setContentPane(gl);
    	gl.c=new Controller();
    	f.addKeyListener(new KeyAdapter(){
    		public void keyPressed(final KeyEvent ke){
    			new Thread(new Runnable(){
					@Override
					public void run() {
						if(ke.getKeyCode()==KeyEvent.VK_ENTER){
							if(!gl.c.m_bStarted){
								gl.c.m_bStarted=true;
								new Thread(gl.c).start();
								gl.toDraw();
							}
						}else if(ke.getKeyCode()==KeyEvent.VK_R){
							gl.c.newRun();
						}else if(ke.getKeyCode()==KeyEvent.VK_F){
							gl.c.toggleFastRender();
						}else if(ke.getKeyCode()==KeyEvent.VK_B){
							gl.c.toggleShowFittest();
						}
					}
    			}).start();
    		}
    	});
    }
}