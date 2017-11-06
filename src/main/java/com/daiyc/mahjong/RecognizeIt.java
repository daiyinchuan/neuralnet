/**
 * 
 */
package com.daiyc.mahjong;

import static com.daiyc.mahjong.AICommon.*;
import static com.daiyc.mahjong.Util.pln;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.math.BigDecimal;
import java.math.MathContext;
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

import com.daiyc.mahjong.AICommon.Point;
import com.daiyc.mahjong.AICommon.Vector2D;

/**
 * 有监督的前馈式神经网络——鼠标手势的识别
 * 
 * 当已有一系列输入模式需要映射到匹配的输出模式时，有监督的训练方法能起作用。
 * 因此可以将这种技术用到任何地方，包括弹跳类游戏、打杀类游戏、赛车类游戏等。
 * 
 * 编写和调试中遇到三个问题：
 * 1 c++里&对象参数传递时，是否拷贝过，函数内修改操作对原对象内部数据无影响。
 *   最后发现在源头，添加规格化后的矩阵顶点到模式集合中没有克隆，导致数据混乱，
 *   第二次添加的数据影响第一次添加的数据，因为它们指向同一个地址。
 * 2 swing多线程问题，耗时操作放入单独的工作线程中，界面更改和repaint放到UI线程中。
 * 3 弹框不要用Internal的，输入弹框将主窗口的按键事件失效。
 */
public class RecognizeIt extends JPanel {
	private static final long serialVersionUID = 1L;
	final static int WINDOW_WIDTH=400;
	final static int WINDOW_HEIGHT=400;
	final static int BIAS=-1;
	final static double ACTIVATION_RESPONSE=0.5;
	final static double ERROR_THRESHOLD=0.003;
	final static int NUM_PATTERNS=11;
	final static int NUM_VECTORS=12;
	final static int NUM_HIDDEN_NEURONS=6;
	final static double LEARNING_RATE=0.5;
	final static double MATCH_TOLERANCE=0.96;
	final static double MOMENTUM=0.9;
	final static double MAX_NOISE_TO_ADD=0.1;
	/**
	 * 神经网络
	 */
	static class NeuralNet{
		/**
		 * 神经细胞
		 */
		static class Neuron{
			int m_iNumInputs;
			//每个输入的权重
			Vector<Double> m_vecWeight=new Vector<Double>();
			double m_dActivation;//神经细胞的激励值
			Vector<Double> m_vecPrevUpdate=new Vector<Double>();
			double m_dError;//误差值
			/**
			 * 随机生成权重
			 * @param numInputs
			 */
			Neuron(int numInputs){
				m_iNumInputs=numInputs+1;
				for(int i=0;i<numInputs+1;i++){
					m_vecWeight.add(randClamped());
					m_vecPrevUpdate.add(0d);
				}
			}
		}
		/**
		 * 神经网络细胞层
		 */
		static class NeuronLayer{
			int m_iNumNeurons;
			Vector<Neuron> m_vecNeurons=new Vector<Neuron>();
			NeuronLayer(int numNeurons,int numInputsPerNeuron){
				m_iNumNeurons=numNeurons;
				for(int i=0;i<numNeurons;i++){
					m_vecNeurons.add(new Neuron(numInputsPerNeuron));
				}
			}
		}
		int m_iNumInputs;
		int m_iNumOutputs;
		int m_iNumHiddenLayers;
		int m_iNeuronsPerHiddenLyr;
		double m_dLearningRate;//规定的学习率
		double m_dErrorSum;//网络的累计误差，所有（输出值-期望值）的总和
		boolean m_bTrained;//如果网络已被训练，为真
		int m_iNumEpochs;//时代计数器
		boolean m_bSoftMax=false;
		Vector<NeuronLayer> m_vecLayers=new Vector<NeuronLayer>();
		NeuralNet(int numInputs,int numOutputs,int neuronsPerHiddenLyr,
				double learningRate,boolean softmax){
			m_iNumInputs=numInputs;
			m_iNumOutputs=numOutputs;
			m_iNumHiddenLayers=1;
			m_iNeuronsPerHiddenLyr=neuronsPerHiddenLyr;
			m_dLearningRate=learningRate;
			m_dErrorSum=9999;
			m_bSoftMax=softmax;
			createNet();
		}
		void createNet(){
			if(m_iNumHiddenLayers>0){//有隐藏层时
				//输入层
				m_vecLayers.add(new NeuronLayer(m_iNeuronsPerHiddenLyr, m_iNumInputs));
				for(int i=0;i<m_iNumHiddenLayers-1;i++){//中间隐藏层
					m_vecLayers.add(new NeuronLayer(m_iNeuronsPerHiddenLyr,
							m_iNeuronsPerHiddenLyr));
				}
				//输出层
				m_vecLayers.add(new NeuronLayer(m_iNumOutputs,
						m_iNeuronsPerHiddenLyr));
			}else{//无隐藏层，只需创建输出层
				m_vecLayers.add(new NeuronLayer(m_iNumOutputs, m_iNumInputs));
			}
		}
		/**
		 * 所有权重设置为小的随机数值
		 */
		void initNetwork(){
			//for each layer
			for(int i=0;i<m_iNumHiddenLayers+1;i++){
				//for each neuron
				for(Neuron neu:m_vecLayers.get(i).m_vecNeurons){
					//for each weight
					for(int k=0;k<neu.m_iNumInputs;k++){
						neu.m_vecWeight.set(k, randClamped());
					}
				}
			}
			m_dErrorSum=9999;
			m_iNumEpochs=0;
		}
		/**
		 * 从一组输入值计算输出值
		 * @param inputs
		 * @return
		 */
		@SuppressWarnings("unchecked")
		Vector<Double> update(Vector<Double> inputs){
			Vector<Double> _inputs=(Vector<Double>)inputs.clone();
			Random rand=new Random();
			for(int k=0;k<_inputs.size();k++){
				_inputs.set(k, _inputs.get(k)+rand.nextFloat()*MAX_NOISE_TO_ADD);
			}
			Vector<Double> outputs=new Vector<Double>();
			if(_inputs.size()!=m_iNumInputs){
				return outputs;
			}
			for(int i=0;i<m_iNumHiddenLayers+1;i++){
				if(i>0){
					_inputs=(Vector<Double>)outputs.clone();
				}
				outputs.clear();
				/*
				 * 对每个神经细胞，求输入*对应权重乘积的综合，
				 * 并将总和赋给S形函数以计算输出
				 */
				for(Neuron neu:m_vecLayers.get(i).m_vecNeurons){
					double netinput=0;
					int numInputs=neu.m_iNumInputs;
					for(int k=0,cWeight=0;k<numInputs-1;k++){
						netinput+=(neu.m_vecWeight.get(k)*_inputs.get(cWeight++));
					}
					//加入偏移值
					netinput+=(neu.m_vecWeight.get(numInputs-1)*BIAS);
					//softmax on output layers
					if(m_bSoftMax&(i==m_iNumHiddenLayers)){
						neu.m_dActivation=Math.exp(netinput);
					}else{
						neu.m_dActivation=sigmoid(netinput,ACTIVATION_RESPONSE);
					}
					outputs.add(neu.m_dActivation);
				}
			}
			if(m_bSoftMax){
				double expTot=0;
				int o;
				for(o=0;o<outputs.size();o++){
					expTot+=outputs.get(o);
				}
				for(o=0;o<outputs.size();o++){
					outputs.set(o, outputs.get(o)/expTot);
					m_vecLayers.get(m_iNumHiddenLayers).m_vecNeurons.get(o)
						.m_dActivation=outputs.get(o);
				}
			}
			return outputs;
		}
		//S形响应曲线
		private double sigmoid(double activation, double response) {
			return 1/(1+Math.exp(-activation/response));
		}
		/**
		 * 对给出的训练集来训练网络，如果数据集有一个错误就返回false
		 * @param data
		 * @param hwnd
		 * @return
		 */
		boolean train(Data data,final Component hwnd){
			m_bTrained=false;
			Vector<Vector<Double>> setIn=data.m_setIn;
			Vector<Vector<Double>> setOut=data.m_setOut;
			if(setIn.size()!=setOut.size()||
					setIn.get(0).size()!=m_iNumInputs||
					setOut.get(0).size()!=m_iNumOutputs){
				pln("Inputs!=Outputs");
				return false;
			}
			initNetwork();
			while(m_dErrorSum>ERROR_THRESHOLD){
				double[] dErrSum=new double[1];
				if(!networkTrainingEpoch(setIn,setOut,dErrSum)){
					return false;
				}else{
					m_dErrorSum=dErrSum[0];
				}
				m_iNumEpochs++;
				pln("#train m_dErrorSum"+m_dErrorSum);
				execIntoUIThread(new Runnable(){
					@Override
					public void run() {
						hwnd.repaint();
					}
				});
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			m_bTrained=true;
			return true;
		}
		double getErrSum(){
			BigDecimal bd=new BigDecimal(m_dErrorSum,new MathContext(6));
			return bd.doubleValue();
		}
		/**
		 * 给定一个训练集，将前馈算法重复执行一次，
		 * 训练集由一系列的输入向量和输出预期值向量组成，
		 * 如果方法执行中出现一个问题，就返回false
		 * @param setIn
		 * @param setOut
		 * @param dErrSum
		 * @return
		 */
		private boolean networkTrainingEpoch(Vector<Vector<Double>> setIn,
				Vector<Vector<Double>> setOut,double[] dErrSum) {
			m_dErrorSum=0;
			double weightUpdate=0;
			//通过神经网络使每一个输入模式得到处理，
			//计算网络输出误差，并更新对应权重
			for(int i=0;i<setIn.size();i++){
				//首先通过神经网络对这一组输入进行计算，并获得输出
				Vector<Double> outputs=update(setIn.get(i));
				if(outputs.size()==0){
					return false;
				}
				//为每个输出神经细胞计算误差并调整相应权重
				for(int j=0;j<m_iNumOutputs;j++){
					double so=setOut.get(i).get(j);//预期
					double op=outputs.get(j);//实际
					double err=(so-op)*op*(1-op);//计算误差
					m_vecLayers.get(1).m_vecNeurons.get(j).m_dError=err;
					Vector<Double> ws=m_vecLayers.get(1).m_vecNeurons.get(j)
							.m_vecWeight;
					Vector<Neuron> nhs=m_vecLayers.get(0).m_vecNeurons;
					int k;
					//更新每一个权重，但不包括偏移
					for(k=0;k<ws.size()-1;k++){
						weightUpdate=err*m_dLearningRate*nhs.get(k).m_dActivation;
						ws.set(k, ws.get(k)+weightUpdate+
							m_vecLayers.get(1).m_vecNeurons.get(j).m_vecPrevUpdate
							.get(k)*MOMENTUM);
						m_vecLayers.get(1).m_vecNeurons.get(j).m_vecPrevUpdate
							.set(k, weightUpdate);
					}
					//计算偏移值
					weightUpdate=err*m_dLearningRate*BIAS;
					ws.set(k, ws.get(k)+weightUpdate+
						m_vecLayers.get(1).m_vecNeurons.get(j)
						.m_vecPrevUpdate.get(k)*MOMENTUM);
					m_vecLayers.get(1).m_vecNeurons.get(j).m_vecPrevUpdate
						.set(k, weightUpdate);
				}
				double error=0;
				if(!m_bSoftMax){
					for(int o=0;o<m_iNumOutputs;o++){
						error+=(setOut.get(i).get(o)-outputs.get(o))*
							 (setOut.get(i).get(o)-outputs.get(o));
					}
				}else{
					for(int o=0;o<m_iNumOutputs;o++){
						error+=setOut.get(i).get(o)*Math.log(outputs.get(o));
					}
					error=-error;
				}
				dErrSum[0]+=error;
				Vector<Neuron> nhs=m_vecLayers.get(0).m_vecNeurons;
				for(int n=0;n<nhs.size();n++){
					double err=0;
					Vector<Neuron> nos=m_vecLayers.get(1).m_vecNeurons;
					for(Iterator<Neuron> it=nos.iterator();it.hasNext();){
						Neuron nrn=it.next();
						err+=nrn.m_dError*nrn.m_vecWeight.get(n);
					}
					//计算错误误差
					double ac=nhs.get(n).m_dActivation;
					err=err*ac*(1-ac);
					Vector<Double> vw=nhs.get(n).m_vecWeight;
					int w;
					for(w=0;w<m_iNumInputs;w++){
						weightUpdate=err*m_dLearningRate*setIn.get(i).get(w);
						//根据BP规则计算新权重
						vw.set(w, vw.get(w)+weightUpdate+nhs.get(n)
							.m_vecPrevUpdate.get(w)*MOMENTUM);
						nhs.get(n).m_vecPrevUpdate.set(w,weightUpdate);
					}
					//和偏移值
					weightUpdate=err*m_dLearningRate*BIAS;
					vw.set(m_iNumInputs,vw.get(m_iNumInputs)+
							weightUpdate+nhs.get(n)
							.m_vecPrevUpdate.get(w)*MOMENTUM);
				}
			}
			return true;
		}
	}
	static class Data{
		final double inputVectors[][]={
			{1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0},//right
			{-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,
				-1,0,-1,0},//left
			{0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,},//down
			{0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,
				0,-1,0,-1},//up
			{1,0,1,0,1,0,0,1,0,1,0,1,-1,0,-1,0,-1,0,0,-1,0,-1,
					0,-1},//clockwise square
			{-1,0,-1,0,-1,0,0,1,0,1,0,1,1,0,1,0,1,0,0,-1,0,-1,
						0,-1},//anticlockwise square
			{1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,-0.45,0,9,
							-0.9,0.45,-0.9,0.45},//right arrow
			{-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,-1,0,
								0.45,0,9,0.9,0.45,0.9,0.45},//left arrow
			{-0.7,0.7,-0.7,0.7,-0.7,0.7,-0.7,0.7,-0.7,0.7,-0.7,0.7,
			 -0.7,0.7,-0.7,0.7,-0.7,0.7,-0.7,0.7,-0.7,0.7,-0.7,0.7},//south west
			{0.7,0.7,0.7,0.7,0.7,0.7,0.7,0.7,0.7,0.7,0.7,0.7,0.7,0.7,
				 0.7,0.7,0.7,0.7,0.7,0.7,0.7,0.7,0.7,0.7},//south east
			{1,0,1,0,1,0,1,0,-0.72,0.69,-0.7,0.72,0.59,0.81,1,0,1,0,1,0,
					 1,0,1,0}//zorro
		};
		final String[] names={
			"Right","Left","Down","Up","Clockwise Square",
			"Anti-Clockwise Square","Right Arrow",
			"Left Arrow","South West","South East","Zorro"
		};
		Vector<Vector<Double>> m_setIn=new Vector<Vector<Double>>();
		Vector<Vector<Double>> m_setOut=new Vector<Vector<Double>>();
		Vector<String> m_vecNames=new Vector<String>();
		Vector<Vector<Double>> m_vecPatterns=new Vector<Vector<Double>>();
		int m_iNumPatterns;//number of patterns loaded into database
		int m_iVectorSize;//size of the pattern vector
		Data(int numStartPatterns,int vectorSize){
			m_iNumPatterns=numStartPatterns;
			m_iVectorSize=vectorSize;
			init();
			createTrainingSetFromData();
		}
		private void init() {
			for(int ptn=0;ptn<m_iNumPatterns;ptn++){
				Vector<Double> temp=new Vector<Double>();
				for(int v=0;v<m_iVectorSize*2;v++){
					temp.add(inputVectors[ptn][v]);
				}
				m_vecPatterns.add(temp);
				m_vecNames.add(names[ptn]);
			}
		}
		private void createTrainingSetFromData() {
			m_setIn.clear();
			m_setOut.clear();
			for(int ptn=0;ptn<m_iNumPatterns;ptn++){
				m_setIn.add(m_vecPatterns.get(ptn));
				Vector<Double> outputs=new Vector<Double>();
				for(int i=0;i<m_iNumPatterns;i++){
					outputs.add(i,0d);
				}
				outputs.set(ptn, 1d);
				m_setOut.add(outputs);
			}
		}
		String patternName(int val){
			if(m_vecNames.size()>0){
				return m_vecNames.get(val);
			}else{
				return "";
			}
		}
		@SuppressWarnings("unchecked")
		boolean addData(Vector<Double> data,String newName){
			Vector<Double> _data=(Vector<Double>)data.clone();
			if(data.size()!=m_iVectorSize*2){
				return false;
			}
			m_vecNames.add(newName);
			m_vecPatterns.add(_data);
			m_iNumPatterns++;
			createTrainingSetFromData();
			return true;
		}
	}
	static class Controller{
		enum Mode{
			LEARNING,//用户正在输入一个自定义手势时
			ACTIVE,//已训练好，准备去识别手势
			UNREADY,//未受训状态
			TRAINING//正处于训练状态
		}
		NeuralNet m_pNet;
		Data m_pData;//保持所有训练的数据
		Vector<Point> m_vecPath=new Vector<Point>();
		Vector<Point> m_vecSmoothPath=new Vector<Point>();
		//将光滑处理后的路径转换为向量
		Vector<Double> m_vecVectors=new Vector<Double>();
		//正在作手势返回true
		boolean m_bDrawing;
		//网络产生的最大输出，是一个最有可能的手势匹配候选者
		double m_dHighestOutput;
		int m_iBestMatch;//根据最大输出确定手势的最好匹配
		//如果网络找到了一个模式，那么这即为匹配者
		int m_iMatch;
		//原始鼠标数据光滑处理时需要达到的点数
		int m_iNumSmoothPoints;
		//数据库中模式的数目
		int m_iNumValidPatterns;
		//程序当前状态
		Mode m_mode;
		Component m_hwnd;
		Controller(Component hwnd){
			m_iNumSmoothPoints=NUM_VECTORS+1;
			m_iBestMatch=-1;
			m_iMatch=-1;
			m_iNumValidPatterns=NUM_PATTERNS;
			m_mode=Mode.UNREADY;
			m_pData=new Data(m_iNumValidPatterns,NUM_VECTORS);
			m_pNet=newNet();
			m_hwnd=hwnd;
		}
		private NeuralNet newNet() {
			return new NeuralNet(NUM_VECTORS*2,m_iNumValidPatterns,
					NUM_HIDDEN_NEURONS,LEARNING_RATE,false);
		}
		//clears the mouse data vectors
		void clear(){
			m_vecPath.clear();
			m_vecSmoothPath.clear();
			m_vecVectors.clear();
		}
		/**
		 * preprocesses the mouse data into a fixed number of points
		 * @return
		 */
		boolean smooth(){
			if(m_vecPath.size()<m_iNumSmoothPoints){
				JOptionPane.showMessageDialog(m_hwnd, "输入的顶点数量不足");
				return false;
			}
			m_vecSmoothPath=clonePointVector(m_vecPath);
			while(m_vecSmoothPath.size()>m_iNumSmoothPoints){
				double shortestSoFar=BIG_NUMBER;
				int pMrk=0;//点记
				//计算最短跨度（即相邻两点间的距离）
				for(int span=2;span<m_vecSmoothPath.size()-1;span++){
					//计算这些点之间的距离
					Point p0=m_vecSmoothPath.get(span-1);
					Point p1=m_vecSmoothPath.get(span);
					double len=Math.sqrt((p0.x-p1.x)*(p0.x-p1.x)+
							(p0.y-p1.y)*(p0.y-p1.y));
					if(len<shortestSoFar){
						shortestSoFar=len;
						pMrk=span;
					}
				}
				Point p0=m_vecSmoothPath.get(pMrk-1);
				Point p1=m_vecSmoothPath.get(pMrk);
				Point np=new Point((p0.x+p1.x)/2,(p0.y+p1.y)/2);
				m_vecSmoothPath.set(pMrk-1, np);
				m_vecSmoothPath.remove(pMrk);
			}
			return true;
		}
		/**
		 * 根据平滑顶点转换成规格化矢量集
		 */
		void createVectors(){
			for(int p=1;p<m_vecSmoothPath.size();p++){
				Point p0=m_vecSmoothPath.get(p-1);
				Point p1=m_vecSmoothPath.get(p);
				Vector2D v2=new Vector2D(p1.x-p0.x,p1.y-p0.y);
				Vector2D.vec2DNormalize(v2);
				m_vecVectors.add(v2.x);
				m_vecVectors.add(v2.y);
			}
		}
		/**
		 * tests for a match with a prelearnt gesture
		 * @return
		 */
		boolean testForMatch(){
			//经过神经网络处理后的输出
			Vector<Double> outputs=m_pNet.update(m_vecVectors);
			if(outputs.size()==0){
				return false;
			}
			m_dHighestOutput=0;
			m_iBestMatch=0;
			m_iMatch=-1;
			for(int i=0;i<outputs.size();i++){
				if(outputs.get(i)>m_dHighestOutput){
					m_dHighestOutput=outputs.get(i);
					m_iBestMatch=i;
					if(m_dHighestOutput>MATCH_TOLERANCE){
						m_iMatch=m_iBestMatch;
					}
				}
			}
			return true;
		}
		void render(Graphics g){
			int h=20;
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
			g.setColor(Color.BLACK);
			if(m_mode==Mode.TRAINING){
				g.drawString("训练代纪："+m_pNet.m_iNumEpochs,
						WINDOW_WIDTH/3, WINDOW_HEIGHT-50);
				g.drawString("学习误差："+m_pNet.getErrSum(),
						WINDOW_WIDTH*3/5, WINDOW_HEIGHT-50);
			}
			if(m_pNet.m_bTrained){
				if(m_mode==Mode.ACTIVE){
					g.drawString("识别循环激活", 5, WINDOW_HEIGHT-50);
				}else if(m_mode==Mode.LEARNING){
					g.drawString("识别循环离线，录入一个新的手势",
							5, WINDOW_HEIGHT-50);
				}
			}else{
				g.drawString("训练中...",5, WINDOW_HEIGHT-50);
			}
			if(!m_bDrawing){
				if(m_dHighestOutput>0){
					if((m_vecSmoothPath.size()>1)&&
							(m_mode!=Mode.LEARNING)){
						if(m_dHighestOutput<MATCH_TOLERANCE){
							g.drawString("我猜这个手势是"+
								m_pData.patternName(m_iBestMatch), 5, h);
						}else{
							g.setColor(Color.BLUE);
							g.drawString(m_pData.patternName(m_iBestMatch),5,h);
						}
					}else if(m_mode!=Mode.LEARNING){
						g.setColor(Color.RED);
						g.drawString("没有足够的顶点，请重试", 5, h);
					}
					m_dHighestOutput=0;
				}
			}
			if(m_vecPath.size()<1){
				return;
			}
			for(int v=1;v<m_vecPath.size();v++){
				g.drawLine((int)m_vecPath.get(v-1).x,
						(int)m_vecPath.get(v-1).y,
						(int)m_vecPath.get(v).x,
						(int)m_vecPath.get(v).y);
			}
			if((!m_bDrawing)&&(m_vecSmoothPath.size()>0)){
				for(Point pt:m_vecSmoothPath){
					g.drawOval((int)(pt.x-2), (int)(pt.y-2), 4, 4);
				}
			}
		}
		boolean drawing(boolean val,final Component root){
			if(val){//按下右键
				clear();
			}else{//放开右键
				if(smooth()){//平滑处理
					createVectors();
					if(m_mode==Mode.ACTIVE){
						if(!testForMatch()){
							return false;
						}
					}else{//处理学习状态
						if(JOptionPane.showConfirmDialog(
								root, "对录入的手势满意吗？", "OK?", 
								JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
							String patterName=JOptionPane.showInputDialog(root, 
									"请录入新手势的名称：");
							if(patterName==null||patterName.equals("")){
								patterName="新名称"+randInt(1, 1000);
							}
							if(m_pData.addData(m_vecVectors, patterName)){
								m_iNumValidPatterns++;
								m_pNet=newNet();
								execIntoThread(new Runnable(){
									@Override
									public void run() {
										trainNetwork();
									}
								});
							}
						}else{
							m_vecPath.clear();
						}
					}
				}
			}
			m_bDrawing=val;
			return true;
		}
		boolean trainNetwork() {
			m_mode=Mode.TRAINING;
			m_hwnd.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			clear();
			if(!m_pNet.train(m_pData, m_hwnd)){
				return false;
			}
			m_hwnd.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			m_mode=Mode.ACTIVE;
			execIntoUIThread(new Runnable(){
				@Override
				public void run() {
					m_hwnd.repaint();
				}
			});
			return true;
		}
		/**
		 * 清除屏幕，改成学习模式，接受用户自定义手势
		 */
		void learningMode(){
			m_mode=Mode.LEARNING;
			clear();
			execIntoUIThread(new Runnable(){
				@Override
				public void run() {
					m_hwnd.repaint();
				}
			});
		}
		void addPoint(Point p){
			m_vecPath.add(p);
		}
	}
	static void execIntoUIThread(Runnable task) {
		SwingUtilities.invokeLater(task);
	}
	static void execIntoThread(Runnable task){
		ExecutorService es=Executors.newSingleThreadExecutor();
		es.execute(task);
		es.shutdown();
	}
	Controller c;
	public void paint(Graphics g){
		super.paint(g);
		c.render(g);
	};
	public static void main(String[] args) {
		final RecognizeIt m=new RecognizeIt();
		m.c=new Controller(m);
		execIntoThread(new Runnable() {
			@Override
			public void run() {
				m.c.trainNetwork();
			}
		});
		execIntoUIThread(new Runnable() {
			@Override
			public void run() {
				JFrame f=new JFrame();
				f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				f.setBounds(500,150, WINDOW_WIDTH, WINDOW_HEIGHT);
				f.setVisible(true);
				f.setContentPane(m);
				f.addKeyListener(new KeyAdapter(){
					public void keyPressed(final KeyEvent ke){
						if(m.getCursor().getType()==Cursor.WAIT_CURSOR){
							return;
						}
						if(ke.getKeyCode()==KeyEvent.VK_T){
							execIntoThread(new Runnable() {
								@Override
								public void run() {
									m.c.trainNetwork();
								}
							});
						}else if(ke.getKeyCode()==KeyEvent.VK_L){
							m.c.learningMode();
						}
					}
				});
				m.addMouseListener(new MouseAdapter() {
					public void mousePressed(MouseEvent me){
						if(m.getCursor().getType()==Cursor.WAIT_CURSOR){
							return;
						}
						if(SwingUtilities.isRightMouseButton(me)){
							m.c.drawing(true, m);
							execIntoUIThread(new Runnable(){
								@Override
								public void run() {
									m.repaint();
								}
							});
						}
					}
					public void mouseReleased(MouseEvent me){
						if(m.getCursor().getType()==Cursor.WAIT_CURSOR){
							return;
						}
						if(SwingUtilities.isRightMouseButton(me)){
							if(!m.c.drawing(false, m)){
								System.exit(1);
							}else{
								execIntoUIThread(new Runnable(){
									@Override
									public void run() {
										m.repaint();
									}
								});
							}
						}
					}
				});
				m.addMouseMotionListener(new MouseMotionAdapter() {
					@Override
					public void mouseDragged(MouseEvent me) {
						if(m.getCursor().getType()==Cursor.WAIT_CURSOR){
							return;
						}
						if(m.c.m_bDrawing){
							java.awt.Point p=me.getPoint();
							m.c.addPoint(new Point(p.x,p.y));
							execIntoUIThread(new Runnable(){
								@Override
								public void run() {
									m.repaint();
								}
							});
						}
					}
				});
			}
		});
	}
}