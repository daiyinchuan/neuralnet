/**
 * 
 */
package com.daiyc.mahjong;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.Random;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import static com.daiyc.mahjong.AICommon.*;
import com.daiyc.mahjong.AICommon.Matrix2D;
import com.daiyc.mahjong.AICommon.Point;
import com.daiyc.mahjong.AICommon.Vector2D;
import static com.daiyc.mahjong.Util.*;
/**
 * 月球登陆器
 * @author Administrator
 */
public class Lander extends JPanel{
	private static final long serialVersionUID = 1L;
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
    static int NUM_COPIES_ELITE=1;
    static int NUM_ELITE=4;
    static final int MAX_MUTATION_DURATION=MAX_ACTION_DURATION/2;
    static final int WINDOW_WIDTH=400;
    static final int WINDOW_HEIGHT=400;
    static final double SHIPS_MASS=100.0;
    static final double LANDER_SCALE=10;
    static final int SCALING_FACTOR=60;
    static final int NUM_STARS=20;
    static final int KB_LEFT=1;
    static final int KB_RIGHT=2;
    static final int KB_SPACE=4;
    /**
     * 登陆器
     */
    static class CLander{
    	Vector2D m_vPos;//在世界坐标系中的位置
    	double m_dRotation;//在世界坐标系中的旋转
    	double m_dMass;//飞船的质量
    	Vector2D m_vVelocity;//飞船的速度
    	Vector2D m_vPadPos;//为了碰撞检测，需要知道登陆地点
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
    	int keyCode;//操纵飞船的按键
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
    	CLander(int cxClient,int cyClient,double rot,Vector2D pos,Vector2D pad){
    		m_cxClient=cxClient;
    		m_cyClient=cyClient;
    		m_dRotation=rot;
    		m_vPos=pos;
    		m_vPadPos=pad;
    		m_vVelocity=new Vector2D(0,0);
    		m_dMass=SHIPS_MASS;
    		m_dScale=LANDER_SCALE;
    		m_iPadX=cxClient/2;
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
    		m_vPos=new Vector2D(WINDOW_WIDTH/2,m_cyClient-50);
    		m_dRotation=PI;
    		m_vVelocity.x=0;
    		m_vVelocity.y=0;
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
    		if(m_bCheckedIfLanded){
    			Graphics2D g2=(Graphics2D)g;
    			AffineTransform at=g2.getTransform();//保存原有变换
    			//为字符串显示变换回去
    			g2.rotate(Math.toRadians(180));
    			g2.translate(-m_cxClient, -m_cyClient);
    			FontMetrics fm=g.getFontMetrics();
    			String s="撞毁";
    			if(landedOK()){
    				s="不错的登陆";
    			}
    			g.drawString(s, m_cxClient/2-fm.stringWidth(s)/2, 160);
    			g2.setTransform(at);//应用保存的原有变换
    		}
    	}
    	/**
    	 * 根据用户按键更新飞船
    	 * @param timeElapsed
    	 */
    	void updateShip(double timeElapsed){
    		if(m_bCheckedIfLanded){//如果飞船已经坠毁或着陆
    			return;
    		}
    		m_bJetOn=false;
    		if(keyPressed(KB_LEFT)){
    			m_dRotation+=ROTATION_PER_SECOND*timeElapsed;
    			if(m_dRotation>TWO_PI){
    				m_dRotation-=TWO_PI;
    			}
    		}
    		if(keyPressed(KB_RIGHT)){
    			m_dRotation-=ROTATION_PER_SECOND*timeElapsed;
    			if(m_dRotation<-PI){
    				m_dRotation+=TWO_PI;
    			}
    		}
    		if(keyPressed(KB_SPACE)){
    			double shipAcc=(THRUST_PER_SECOND*timeElapsed)/m_dMass;
    			m_vVelocity.x+=shipAcc*Math.sin(m_dRotation);
    			m_vVelocity.y+=shipAcc*Math.cos(m_dRotation);
    			m_bJetOn=true;//打开喷气
    		}
    		m_vVelocity.y+=GRAVITY*timeElapsed;
    		m_vPos.add(Vector2D.multiply(Vector2D.multiply(m_vVelocity, timeElapsed), SCALING_FACTOR));
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
    				if(landedOK()){
    					//play sound landed
    				}else{
    					//play sound explosion
    				}
    				m_bCheckedIfLanded=true;
    			}
    		}
    	}
		boolean keyPressed(int key){
			return (key & keyCode)==key;
		}
		/**
		 * 如果用户已经满足所有的登陆条件，返回真
		 * @return
		 */
		boolean landedOK(){
			double distFromPad=Math.abs(m_vPadPos.x-m_vPos.x);
			double speed=Vector2D.vec2DLen(m_vVelocity);
			if(!m_bCheckedIfLanded){
				pln("#distFromPad#"+distFromPad+"#"+DIST_TOLERANCE);
				pln("#speed#"+speed+"#"+SPEED_TOLERANCE);
				pln("#rot#"+Math.abs(m_dRotation)+"#"+ROTATION_TOLERANCE);
			}
			if(distFromPad<DIST_TOLERANCE && speed<SPEED_TOLERANCE
					&& Math.abs(m_dRotation)<ROTATION_TOLERANCE){
				return true;
			}
			return false;
		}
    }
    static class Controller implements Runnable{
    	CLander m_pUserLander;//用户能控制的登月飞船
    	boolean m_bStarted;//是否成功登陆
    	Vector<Point> m_vecStarVB=new Vector<Point>();//星星顶点
    	Vector<Point> m_vecPadVB=new Vector<Point>();//登陆点形状顶点
    	Vector2D m_vPadPos;//登陆点的位置
    	int m_cxClient,m_cyClient;//窗口尺寸
    	double[][] pad={
    		{-20,0},{20,0},{20,5},{-20,5}
    	};
    	Controller(){
    		this(WINDOW_WIDTH,WINDOW_HEIGHT);
    	}
    	Controller(int cxClient,int cyClient){
    		m_cxClient=cxClient;
    		m_cyClient=cyClient;
    		Random rand=new Random();
    		m_vPadPos=new Vector2D(rand.nextFloat()*m_cxClient,50);
    		Vector2D vStartPos=new Vector2D(WINDOW_WIDTH/2,m_cyClient-50);
    		m_pUserLander=new CLander(m_cxClient,m_cyClient,PI,vStartPos,m_vPadPos);
    		for(double[] p:pad){
    			m_vecPadVB.add(new Point(p[0],p[1]));
    		}
    		for(int i=0;i<NUM_STARS;i++){
    			m_vecStarVB.add(new Point(randInt(0,m_cxClient),randInt(100, m_cyClient)));
    		}
    	}
    	void worldTransform(Vector<Point> pad){
    		Matrix2D m2=new Matrix2D();
    		m2.translate(m_vPadPos.x, m_vPadPos.y);
    		m2.transformPoints(pad);
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
    	boolean update(double timeElapsed){
    		m_pUserLander.updateShip(timeElapsed);
    		return true;
    	}
    	/**
    	 * 新一轮的执行
    	 */
    	void newRun(){
    		Random rand=new Random();
    		m_vPadPos=new Vector2D(50+rand.nextFloat()*(m_cxClient-100),50);
    		m_pUserLander.reset(m_vPadPos);
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
    		m_pUserLander.render(g);
    		renderLandingPad(g);
    		g2.rotate(Math.toRadians(180));
    		g2.translate(-m_cxClient, -m_cyClient);
    		int y=15;
    		g.drawString("回车:开始", 10, y);
    		g.drawString("R键:新的一轮", 75, y);
    		g.drawString("左键:左转", 160, y);
    		g.drawString("右键:右转", 220, y);
    		g.drawString("空格:喷气推进", 280, y);
    	}
		@Override
		public void run() {
			Timer t=new Timer(FRAMES_PER_SECOND);
			t.start();
			while(m_bStarted){
				if(t.readyForNextFrame()){
					update(t.getTimeElapsed());
				}
			}
		}
		void keyPress(int k){
			m_pUserLander.keyCode=m_pUserLander.keyCode | k;
		}
		void keyRelease(int k){
			m_pUserLander.keyCode=m_pUserLander.keyCode & (~k);
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
    	final Lander gl=new Lander();
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
						}else if(ke.getKeyCode()==KeyEvent.VK_LEFT){
							gl.c.keyPress(KB_LEFT);
						}else if(ke.getKeyCode()==KeyEvent.VK_RIGHT){
							gl.c.keyPress(KB_RIGHT);
						}else if(ke.getKeyCode()==KeyEvent.VK_SPACE){
							gl.c.keyPress(KB_SPACE);
						}
					}
    			}).start();
    		}
    		public void keyReleased(final KeyEvent ke){
    			new Thread(new Runnable(){
					@Override
					public void run() {
						if(ke.getKeyCode()==KeyEvent.VK_LEFT){
							gl.c.keyRelease(KB_LEFT);
						}else if(ke.getKeyCode()==KeyEvent.VK_RIGHT){
							gl.c.keyRelease(KB_RIGHT);
						}else if(ke.getKeyCode()==KeyEvent.VK_SPACE){
							gl.c.keyRelease(KB_SPACE);
						}
					}
    			}).start();
    		}
    	});
    }
}