/**
 * 
 */
package com.daiyc.mahjong;

import static com.daiyc.mahjong.AICommon.FRAMES_PER_SECOND;
import static com.daiyc.mahjong.AICommon.clamp;
import static com.daiyc.mahjong.AICommon.clonePointVector;
import static com.daiyc.mahjong.AICommon.randClamped;
import static com.daiyc.mahjong.AICommon.randInt;
import static com.daiyc.mahjong.Util.pln;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.daiyc.mahjong.AICommon.Matrix2D;
import com.daiyc.mahjong.AICommon.Point;
import com.daiyc.mahjong.AICommon.Timer;
import com.daiyc.mahjong.AICommon.Vector2D;
import com.google.common.collect.TreeMultiset;

/**
 * 实时演化的智能外星人，躲避你的射击
 */
public class BrainyAliens extends JPanel {
	private static final long serialVersionUID = 1L;
	final static int WINDOW_WIDTH=400;
	final static int WINDOW_HEIGHT=400;
	final static int MAX_BULLETS=3;
	final static double BULLET_SPEED=4;
	final static double BULLET_SCALE=3;
	final static double ALIEN_MASS=100;
	final static double ALIEN_SCALE=3;
	final static int NUM_INPUTS=2+2*MAX_BULLETS;
	final static int NUM_OUTPUTS=3;
	final static int NUM_HIDDEN=1;
	final static int NEURONS_PER_HIDDENLAYER=15;
	final static int BIAS=-1;
	final static double ACTIVATION_RESPONSE=0.2;
	final static double GRAVITY=-1.63;
	final static double GRAVITY_PER_TICK=GRAVITY/FRAMES_PER_SECOND;
	final static double GUN_SCALE=6;
	final static int FIRING_RATE=15;
	final static int KB_LEFT=1;
	final static int KB_RIGHT=2;
	final static int KB_SPACE=4;
	final static double MAX_TRANSLATION_PER_TICK=2.5;
	final static int THRUST_LEFT=0;
	final static int THRUST_RIGHT=1;
	final static int THRUST_UP=2;
	final static int DRIFT=3;
	final static double MAX_THRUST_LATERAL=30;
	final static double MAX_THRUST_VERTICAL=20;
	final static double MAX_VELOCITY=2;
	final static double MUTATION_RATE=0.2;
	final static double MAX_PERTURBATION=1;
	final static int NUM_ON_SCREEN=10;
	final static int NUM_STARS=20;
	final static int PRE_SPAWNS=20;
	final static int POP_SIZE=20;
	final static int NUM_TOURNEY_COMPETITORS=10;
	final static double PERCENT_BEST_TOSELECTFROM=0.2;
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
			for(int i=0;i<m_numHiddenLayers+1;i++){
				if(i>0){
					_inputs=(Vector<Double>)outputs.clone();
				}
				outputs.clear();
				for(Neuron n:m_vecLayers.get(i).m_vecNeurons){
					double netinput=0;
					int numInputs=n.m_numInputs;
					for(int k=0,cWeight=0;k<numInputs-1;k++){
						netinput+=(n.m_vecWeight.get(k)*_inputs.get(cWeight++));
					}
					//加入偏移值
					netinput+=(n.m_vecWeight.get(numInputs-1)*BIAS);
					outputs.add(sigmoid(netinput, ACTIVATION_RESPONSE));
				}
			}
			return outputs;
		}
		/*Vector<Integer> calcSplitPoint(){
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
		}*/
	}
	/**
	 * 子弹
	 */
	static class Bullet{
		Vector2D m_vPos;//位置
		double m_dScale;//缩放
		boolean m_bActive;//是否激活
		Vector<Point> m_vecBulletVB=new Vector<Point>();
		Vector<Point> m_vecBulletVBTrans;
		//界限盒子，用于碰撞检测
		Rectangle2D.Double m_bulletBBox=new Rectangle2D.Double();
		final double[][] bullet={{-1,-1},{0,1},{1,-1}};
		Bullet(){
			m_vPos=new Vector2D(0,30);
			m_dScale=BULLET_SCALE;
			for(double[] b:bullet){
				m_vecBulletVB.add(new Point(b[0],b[1]));
			}
		}
		void worldTransform(){
			m_vecBulletVBTrans=clonePointVector(m_vecBulletVB);
			Matrix2D m2=new Matrix2D();
			m2.scale(m_dScale, m_dScale);
			m2.translate(m_vPos.x, m_vPos.y);
			m2.transformPoints(m_vecBulletVBTrans);
		}
		void render(Graphics g){
			worldTransform();
			int i;
			for(i=1;i<m_vecBulletVBTrans.size();i++){
				g.drawLine((int)m_vecBulletVBTrans.get(i-1).x,
						(int)m_vecBulletVBTrans.get(i-1).y,
						(int)m_vecBulletVBTrans.get(i).x,
						(int)m_vecBulletVBTrans.get(i).y);
			}
			g.drawLine((int)m_vecBulletVBTrans.get(i-1).x,
					(int)m_vecBulletVBTrans.get(i-1).y,
					(int)m_vecBulletVBTrans.get(0).x,
					(int)m_vecBulletVBTrans.get(0).y);
		}
		void update(){
			if(m_bActive){
				m_vPos.y+=BULLET_SPEED;
				if(m_vPos.y>WINDOW_HEIGHT){
					m_bActive=!m_bActive;
				}
			}
			m_bulletBBox.y+=BULLET_SPEED;
		}
		void switchOn(double posX){
			m_vPos.x=posX;
			m_vPos.y=35;
			m_bActive=true;
			m_bulletBBox.x=m_vPos.x-BULLET_SCALE;
			m_bulletBBox.width=BULLET_SCALE*2;
			m_bulletBBox.y=m_vPos.y+BULLET_SCALE;
			m_bulletBBox.height=BULLET_SCALE*2;
		}
	}
	/**
	 * 枪
	 */
	static class Gun{
		Vector2D m_vPos;
		double m_dScale;
		int m_iTicksToNextBullet;
		Vector<Point> m_vecGunVB=new Vector<Point>();
		Vector<Point> m_vecGunVBTrans;
		boolean m_bAutoGun;//用于切换自动开枪
		Vector<Bullet> m_vecBullets=new Vector<Bullet>();
		int keyCode;//按键
		final double[][] gun={
			{2,2},{2,0},{-2,0},{-2,2},{-1,2},{-1,3},{1,3},{1,2}	
		};
		Gun(){
			m_iTicksToNextBullet=FIRING_RATE;
			m_dScale=GUN_SCALE;
			m_bAutoGun=true;
			m_vPos=new Vector2D(WINDOW_WIDTH/2,20);
			for(double[] g:gun){
				m_vecGunVB.add(new Point(g[0],g[1]));
			}
			for(int i=0;i<MAX_BULLETS;i++){
				m_vecBullets.add(new Bullet());
			}
		}
		void worldTransform(){
			m_vecGunVBTrans=clonePointVector(m_vecGunVB);
			Matrix2D m2=new Matrix2D();
			m2.scale(m_dScale, m_dScale);
			m2.translate(m_vPos.x, m_vPos.y);
			m2.transformPoints(m_vecGunVBTrans);
		}
		void render(Graphics g){
			worldTransform();
			Color oldPen=g.getColor();
			g.setColor(new Color(200,200,100));
			int i;
			for(i=1;i<m_vecGunVBTrans.size();i++){
				g.drawLine((int)m_vecGunVBTrans.get(i-1).x,
						(int)m_vecGunVBTrans.get(i-1).y,
						(int)m_vecGunVBTrans.get(i).x,
						(int)m_vecGunVBTrans.get(i).y);
			}
			g.drawLine((int)m_vecGunVBTrans.get(i-1).x,
					(int)m_vecGunVBTrans.get(i-1).y,
					(int)m_vecGunVBTrans.get(0).x,
					(int)m_vecGunVBTrans.get(0).y);
			for(Bullet b:m_vecBullets){
				if(b.m_bActive){
					b.render(g);
				}
			}
			g.setColor(oldPen);
		}
		AutoGun ag=new AutoGun();
		/**
		 * 让枪自动开火并随机移动位置，用于初始阶段加速外星人进化
		 */
		class AutoGun{
			int duration;
			int action;
			void autoGun(){
				if(duration<=0){
					duration=randInt(30, 300);
					action=randInt(0, 2);
				}
				duration--;
				switch (action) {
				case 0:
					if(m_vPos.x>m_dScale){
						m_vPos.x-=MAX_TRANSLATION_PER_TICK;
					}else{
						action=1;
					}
					break;
				case 1:
					if(m_vPos.x<WINDOW_WIDTH-m_dScale){
						m_vPos.x+=MAX_TRANSLATION_PER_TICK;
					}else{
						action=0;
					}
					break;
				}
				for(Bullet b:m_vecBullets){
					if(!b.m_bActive){
						b.switchOn(m_vPos.x);
						duration=-1;
						break;
					}
				}
				for(Bullet b:m_vecBullets){
					b.update();
				}
			}
		}
		/**
		 * 检查用户的按键并且相应的更新枪的参数，
		 * 当枪更新时，任何激活的子弹也更新
		 */
		void update(){
			if(m_bAutoGun){
				ag.autoGun();
				return;
			}
			if(keyPressed(KB_LEFT)&&(!(m_vPos.x<m_dScale))){
				m_vPos.x-=MAX_TRANSLATION_PER_TICK;
			}
			if(keyPressed(KB_RIGHT)&&
					(!(m_vPos.x>WINDOW_WIDTH-m_dScale))){
				m_vPos.x+=MAX_TRANSLATION_PER_TICK;
			}
			if(keyPressed(KB_SPACE)&&
					(m_iTicksToNextBullet<0)){
				for(Bullet b:m_vecBullets){
					if(!b.m_bActive){
						b.switchOn(m_vPos.x);
						m_iTicksToNextBullet=FIRING_RATE;
						break;
					}
				}
			}
			for(Bullet b:m_vecBullets){
				b.update();
			}
			m_iTicksToNextBullet--;
		}
		boolean keyPressed(int key){
			return (key & keyCode)==key;
		}
	}
	/**
	 * 外星人 
	 */
	static class Alien{
		NeuralNet m_itsBrain;
		Vector2D m_vPos;
		Vector2D m_vVelocity;
		double m_dScale;
		double m_dMass;//质量
		int m_iAge;//年龄，也是适应分
		Rectangle2D.Double m_alienBBox;//界限盒子，用于碰撞检测
		Vector<Point> m_vecAlienVB=new Vector<Point>();
		Vector<Point> m_vecAlienVBTrans;
		boolean m_bWarning;//输入大小错误时为true
		final double[][] alien={
			{1,3},{4,1},{4,-1},{2,-4},{1,-1},{0,-2},{-1,-1},
			{-2,-4},{-4,-1},{-4,1},{-1,3},
			
			{-2,1},{-1.5,0.5},{-2,0},{-2.5,1},
			
			{2,1},{1.5,0.5},{2,0},{2.5,1}
		};
		Alien(){
			m_itsBrain=new NeuralNet();
			m_dScale=ALIEN_SCALE;
			m_vVelocity=new Vector2D();
			m_dMass=ALIEN_MASS;
			m_vPos=new Vector2D(randInt(0, WINDOW_WIDTH),WINDOW_HEIGHT);
			for(double[] a:alien){
				m_vecAlienVB.add(new Point(a[0],a[1]));
			}
			m_alienBBox=new Rectangle2D.Double();
			m_alienBBox.x=m_vPos.x-(4*ALIEN_SCALE);
			m_alienBBox.y=m_vPos.y+(3*ALIEN_SCALE);
			m_alienBBox.width=8*ALIEN_SCALE;
			m_alienBBox.height=-7*ALIEN_SCALE;
		}
		public String toString(){
			return "m_iAge:"+m_iAge;
		}
		private static class FitnessCmp implements Comparator<Alien>{
			@Override
			public int compare(Alien a1, Alien a2) {
				double d=a2.m_iAge-a1.m_iAge;
				int r=0;
				if(d>0){
					r=1;
				}else if(d<0){
					r=-1;
				}
				return r;
			}
		}
		static FitnessCmp fCmp=new FitnessCmp();
		private void worldTransform(){
			m_vecAlienVBTrans=clonePointVector(m_vecAlienVB);
			Matrix2D m2=new Matrix2D();
			m2.scale(m_dScale, m_dScale);
			m2.translate(m_vPos.x, m_vPos.y);
			m2.transformPoints(m_vecAlienVBTrans);
		}
		/*
		 * 更新外星人的神经网络并返回它的下一个动作
		 */
		private int getActionFromNetwork(Vector<Bullet> bullets,
				Vector2D gunPos){
			Vector<Double> inputs=new Vector<Double>();
			double xToTurret=gunPos.x-m_vPos.x;
			double yToTurret=gunPos.y-m_vPos.y;
			inputs.add(xToTurret);
			inputs.add(yToTurret);
			for(Bullet bul:bullets){
				if(bul.m_bActive){
					double xToBullet=bul.m_vPos.x-m_vPos.x;
					double yToBullet=bul.m_vPos.y-m_vPos.y;
					inputs.add(xToBullet);
					inputs.add(yToBullet);
				}else{//如果子弹未激活，仅仅输入炮塔的矢量
					inputs.add(xToTurret);
					inputs.add(yToTurret);
				}
			}
			Vector<Double> outputs=m_itsBrain.update(inputs);
			if(outputs.size()!=NUM_OUTPUTS){
				m_bWarning=true;
			}
			double biggestSoFar=0;
			int action=DRIFT;
			for(int i=0;i<outputs.size();i++){
				double op=outputs.get(i);
				if(op>biggestSoFar && op>0.9){
					action=i;
					biggestSoFar=op;
				}
			}
			return action;
		}
		void render(Graphics g){
			worldTransform();
			Color oldPen=g.getColor();
			//draw body
			g.setColor(Color.GREEN);
			int i;
			for(i=1;i<11;i++){
				g.drawLine((int)m_vecAlienVBTrans.get(i-1).x,
						(int)m_vecAlienVBTrans.get(i-1).y,
						(int)m_vecAlienVBTrans.get(i).x,
						(int)m_vecAlienVBTrans.get(i).y);
			}
			g.drawLine((int)m_vecAlienVBTrans.get(i-1).x,
					(int)m_vecAlienVBTrans.get(i-1).y,
					(int)m_vecAlienVBTrans.get(i).x,
					(int)m_vecAlienVBTrans.get(i).y);
			//draw eye
			g.setColor(Color.RED);
			//left eye
			for(i=12;i<15;i++){
				g.drawLine((int)m_vecAlienVBTrans.get(i-1).x,
						(int)m_vecAlienVBTrans.get(i-1).y,
						(int)m_vecAlienVBTrans.get(i).x,
						(int)m_vecAlienVBTrans.get(i).y);
			}
			//right eye
			for(i=16;i<19;i++){
				g.drawLine((int)m_vecAlienVBTrans.get(i-1).x,
						(int)m_vecAlienVBTrans.get(i-1).y,
						(int)m_vecAlienVBTrans.get(i).x,
						(int)m_vecAlienVBTrans.get(i).y);
			}
			g.setColor(oldPen);
			if(m_bWarning){
				g.drawString("Wrong amount of inputs!", 110, 200);
			}
		}
		/**
		 * 询问外星人的大脑并相应的更新它的位置
		 * @param bullets
		 * @param gunPos
		 * @return
		 */
		boolean update(Vector<Bullet> bullets,Vector2D gunPos){
			m_iAge++;
			int action=getActionFromNetwork(bullets, gunPos);
			switch(action){
			case THRUST_LEFT:
				m_vVelocity.x-=MAX_THRUST_LATERAL/m_dMass;
				break;
			case THRUST_RIGHT:
				m_vVelocity.x+=MAX_THRUST_LATERAL/m_dMass;
				break;
			case THRUST_UP:
				m_vVelocity.y+=MAX_THRUST_VERTICAL/m_dMass;
				break;
			default:
					break;
			}
			m_vVelocity.y+=GRAVITY_PER_TICK;
			clamp(m_vVelocity.x, -MAX_VELOCITY, MAX_VELOCITY);
			clamp(m_vVelocity.y, -MAX_VELOCITY, MAX_VELOCITY);
			m_vPos.add(m_vVelocity);
			if(m_vPos.x>WINDOW_WIDTH){
				m_vPos.x=0;
			}
			if(m_vPos.x<0){
				m_vPos.x=WINDOW_WIDTH;
			}
			m_alienBBox.x=m_vPos.x-(4*ALIEN_SCALE);
			m_alienBBox.y=m_vPos.y+(3*ALIEN_SCALE);
			//如果跌落到枪的水平面下或者飞的太高或者被子弹击中，
			//就判定它死掉
			if((m_vPos.y>WINDOW_HEIGHT+5)||(m_vPos.y<15)||
					(checkForCollision(bullets))){
				return false;
			}
			return true;
		}
		/*
		 * 检测与任何激活的子弹的碰撞，如果检测到碰撞返回true
		 */
		private boolean checkForCollision(Vector<Bullet> bullets) {
			for(Bullet bul:bullets){
				if(!bul.m_bActive){
					continue;
				}
				Rectangle2D.Double rect=bul.m_bulletBBox;
				if(!((rect.y+rect.height>m_alienBBox.y)||
					(rect.y<m_alienBBox.y+m_alienBBox.height)||
					(rect.x>m_alienBBox.x+m_alienBBox.width)||
					(rect.x+rect.width<m_alienBBox.x))){
					bul.m_bActive=false;
					return true;
				}
			}
			return false;
		}
		void reset(){
			m_iAge=0;
			m_vVelocity=new Vector2D();
			m_vPos=new Vector2D(randInt(0, WINDOW_WIDTH),WINDOW_HEIGHT);
		}
		/**
		 * this mutates the connection weights in the alien's neural net
		 */
		void mutate(){
			Vector<Double> weights=m_itsBrain.getWeights();
			Random rand=new Random();
			for(int i=0;i<weights.size();i++){
				if(rand.nextFloat()<MUTATION_RATE){
					weights.set(i,weights.get(i)+randClamped()*MAX_PERTURBATION);
				}
			}
			m_itsBrain.putWeights(weights);
		}
	}
	static class Controller{
		Gun m_pGunTurret;//炮塔
		//外星人池
		TreeMultiset<Alien> m_setAliens=TreeMultiset.create(Alien.fCmp);
		//当前活动的外星人
		Vector<Alien> m_vecActiveAliens=new Vector<Alien>();
		int m_iAliensCreatedSoFar;
		int m_iNumSpawnedFromTheMultiset;
		Vector<Point> m_vecStarVB=new Vector<Point>();
		int m_cxClient,m_cyClient;
		boolean m_bFastRender;
		Controller(int cxClient,int cyClient){
			m_cxClient=cxClient;
			m_cyClient=cyClient;
			m_bFastRender=true;
			m_pGunTurret=new Gun();
			for(int a=0;a<NUM_ON_SCREEN;a++){
				m_vecActiveAliens.add(new Alien());
				m_iAliensCreatedSoFar++;
			}
			for(int s=0;s<NUM_STARS;s++){
				m_vecStarVB.add(new Point(randInt(0, cxClient),
						randInt(0, cyClient)));
			}
		}
		Alien advance(Iterator<Alien> it,int n){
			while(n-->0 && it.hasNext()){
				it.next();
			}
			return it.next();
		}
		synchronized Alien tournamentSelection(){
			double bestFitnessSoFar=0;
			Alien chosenOne=null;
			for(int i=0;i<NUM_TOURNEY_COMPETITORS;i++){
				Iterator<Alien> it=m_setAliens.iterator();
				int thisTry=randInt(0, (int)((m_setAliens.size()-1)*
						PERCENT_BEST_TOSELECTFROM));
				Alien aa=advance(it, thisTry);
				if(aa.m_iAge>bestFitnessSoFar){
					chosenOne=aa;
					bestFitnessSoFar=aa.m_iAge;
				}
			}
			return chosenOne;
		}
		synchronized boolean update(){
			if(m_bFastRender && m_iNumSpawnedFromTheMultiset>PRE_SPAWNS){
				m_pGunTurret.m_bAutoGun=false;
				m_bFastRender=false;
			}
			m_pGunTurret.update();
			for(int s=0;s<m_vecStarVB.size();s++){
				Point p=m_vecStarVB.get(s);
				p.y-=0.2f;
				if(p.y<0){
					p.x=randInt(0, m_cxClient);
					p.y=m_cyClient;
				}
			}
			for(int i=0;i<m_vecActiveAliens.size();i++){
				Alien ali=m_vecActiveAliens.get(i);
				if(!ali.update(m_pGunTurret.m_vecBullets, m_pGunTurret.m_vPos)){
					m_setAliens.add(ali);
					if(m_setAliens.size()>=POP_SIZE){
						m_setAliens.pollLastEntry();
					}
					m_iNumSpawnedFromTheMultiset++;
					if(m_iAliensCreatedSoFar<=POP_SIZE){
						m_vecActiveAliens.set(i, new Alien());
						m_iAliensCreatedSoFar++;
					}else{
						m_vecActiveAliens.set(i, tournamentSelection());
						m_vecActiveAliens.get(i).reset();
						if(new Random().nextFloat()<0.8){
							m_vecActiveAliens.get(i).mutate();
						}
					}
				}
			}
			return true;
		}
		Timer stats=new Timer(1);
		void render(Graphics g){
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, m_cxClient, m_cyClient);
			if(!m_bFastRender){
				Graphics2D g2=(Graphics2D)g;
				AffineTransform at=g2.getTransform();
				//垂直翻转
				g2.setTransform(new AffineTransform(1, 0, 0, -1, 0, m_cyClient));
				//render stars
				Random rand=new Random();
				g.setColor(Color.WHITE);
				for(int i=0;i<m_vecStarVB.size();i++){
					if(rand.nextFloat()>0.1){
						g.drawOval((int)m_vecStarVB.get(i).x, 
								(int)m_vecStarVB.get(i).y,
								1, 1);
					}
				}
				//render the aliens
				for(int i=0;i<m_vecActiveAliens.size();i++){
					m_vecActiveAliens.get(i).render(g);
				}
				//render the gun and any bullets
				m_pGunTurret.render(g);
				g2.setTransform(at);
				g.setColor(new Color(0,0,255,255));
				g.drawString("Num Spawned "+m_iNumSpawnedFromTheMultiset,
						5, 15);
				if(stats.m_nextTime==0){
					stats.start();
				}else{
					if(stats.readyForNextFrame()){
						StringBuffer sb=new StringBuffer();
						int i=0;
						synchronized (m_setAliens) {
							for(Iterator<Alien> it=m_setAliens.iterator();
									it.hasNext()&&i<20;i++){
								sb.append(it.next().m_iAge).append(",");
							}
						}
						pln(sb.toString());
					}
				}
			}else{
				g.setColor(new Color(0,0,255,255));
				g.drawString("PreSpawning", 10, m_cyClient-30);
				g.fillRect(10, m_cyClient-20, (m_cxClient/PRE_SPAWNS)*
						m_iNumSpawnedFromTheMultiset-10,10);
			}
		}
		synchronized void reset(){
			m_vecActiveAliens.clear();
			m_setAliens.clear();
			m_iAliensCreatedSoFar=0;
			m_iNumSpawnedFromTheMultiset=0;
			m_bFastRender=true;
			m_pGunTurret.m_bAutoGun=true;
			for(int a=0;a<NUM_ON_SCREEN;a++){
				m_vecActiveAliens.add(new Alien());
				m_iAliensCreatedSoFar++;
			}
		}
		void keyPress(int k){
			m_pGunTurret.keyCode=m_pGunTurret.keyCode|k;
		}
		void keyRelease(int k){
			m_pGunTurret.keyCode=m_pGunTurret.keyCode&(~k);
		}
	}
	Controller c;
	public void paint(Graphics g){
		super.paint(g);
		c.render(g);
	}
	public static void main(String[] args) {
		final BrainyAliens ba=new BrainyAliens();
		ba.c=new Controller(WINDOW_WIDTH, WINDOW_HEIGHT);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame f=new JFrame();
				f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				f.setBounds(500,150,WINDOW_WIDTH+15,WINDOW_HEIGHT+38);
				f.setVisible(true);
				f.setContentPane(ba);
				f.addKeyListener(new KeyAdapter(){
					public void keyPressed(final KeyEvent ke){
						if(ke.getKeyCode()==KeyEvent.VK_R){
							ba.c.reset();
						}else if(ke.getKeyCode()==KeyEvent.VK_LEFT){
							ba.c.keyPress(KB_LEFT);
						}else if(ke.getKeyCode()==KeyEvent.VK_RIGHT){
							ba.c.keyPress(KB_RIGHT);
						}else if(ke.getKeyCode()==KeyEvent.VK_SPACE){
							ba.c.keyPress(KB_SPACE);
						}
					}
					public void keyReleased(final KeyEvent ke){
						if(ke.getKeyCode()==KeyEvent.VK_LEFT){
							ba.c.keyRelease(KB_LEFT);
						}else if(ke.getKeyCode()==KeyEvent.VK_RIGHT){
							ba.c.keyRelease(KB_RIGHT);
						}else if(ke.getKeyCode()==KeyEvent.VK_SPACE){
							ba.c.keyRelease(KB_SPACE);
						}
					}
				});
			}
		});
		ExecutorService es=Executors.newSingleThreadExecutor();
		es.execute(new Runnable() {
			@Override
			public void run() {
				Timer t=new Timer(FRAMES_PER_SECOND);
				t.start();
				boolean bDone=false;
				while(!bDone){
					if(t.readyForNextFrame()||ba.c.m_bFastRender){
						if(!ba.c.update()){
							JOptionPane.showMessageDialog(ba,
									"Contoller Update faulty", "ERROR", 
									JOptionPane.ERROR_MESSAGE);
							bDone=true;
						}
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								ba.repaint();
							}
						});
					}
				}
			}
		});
		es.shutdown();
	}
}