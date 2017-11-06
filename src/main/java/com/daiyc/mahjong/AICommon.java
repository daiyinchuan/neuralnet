/**
 * 
 */
package com.daiyc.mahjong;

import static com.daiyc.mahjong.Util.pln;
import java.util.Random;
import java.util.Vector;
import com.sun.jna.Library;
import com.sun.jna.Native;
/**
 * AI Common
 * @author Administrator
 */
public class AICommon {
	final static double PI=Math.PI;
	final static double HALF_PI=PI/2;
	final static double TWO_PI=PI*2;
	final static int FRAMES_PER_SECOND=60;
	final static double BIG_NUMBER=9999999;
	/**
	 * >=beg <=end
	 * @param beg
	 * @param end
	 * @return
	 */
	static int randInt(int beg,int end){
		Random rand=new Random();
		if(beg==0){
			return rand.nextInt(end+1);
		}else{
			int r;
			do{
				r=rand.nextInt(end+1);
			}while(r<beg);
			return r;
		}
	}
	/**
	 * -1到1之间的随机浮点值
	 * @return
	 */
	static double randClamped(){
		Random rand=new Random();
		return rand.nextFloat()-rand.nextFloat();
	}
	/**
	 * 返回范围之内的值
	 * @param arg
	 * @param min
	 * @param max
	 * @return
	 */
	static int clamp(int arg,int min,int max){
		if(arg<min){
			return min;
		}
		if(arg>max){
			return max;
		}
		return arg;
	}
	/**
	 * 返回范围之内的值
	 * @param arg
	 * @param min
	 * @param max
	 * @return
	 */
	static double clamp(double arg,double min,double max){
		if(arg<min){
			return min;
		}
		if(arg>max){
			return max;
		}
		return arg;
	}
	/**
	 * 克隆顶点矢量
	 * @param src
	 * @return
	 */
	static Vector<Point> clonePointVector(Vector<Point> src){
		Vector<Point> dst=new Vector<Point>();
		for(int i=0;i<src.size();i++){
			dst.add((Point)(src.get(i).clone()));
		}
		return dst;
	}
	/**
	 * 顶点，用于描绘对象外观
	 */
	static class Point{
		double x,y;
		Point(){}
		Point(double a,double b){	
			x=a;
			y=b;
		}
		public Object clone(){
			return new Point(x,y);
		}
		public String toString(){
			return "x:"+x+",y:"+y;
		}
	}
	/**
	 * 向量、矢量，用于速度和对象位置
	 */
	static class Vector2D{
		double x,y;
		Vector2D(){}
		Vector2D(double a,double b){
			x=a;
			y=b;
		}
		public Vector2D clone(){
			return new Vector2D(this.x,this.y);
		}
		public String toString(){
			return "x:"+x+",y:"+y;
		}
		/**
		 * +=操作
		 * @param rhs
		 * @return
		 */
		Vector2D add(Vector2D rhs){
			x+=rhs.x;
			y+=rhs.y;
			return this;
		}
		/**
		 * -=操作
		 * @param rhs
		 * @return
		 */
		Vector2D subtract(Vector2D rhs){
			x-=rhs.x;
			y-=rhs.y;
			return this;
		}
		/**
		 * *=操作
		 * @param rhs
		 * @return
		 */
		Vector2D multiply(double rhs){
			x*=rhs;
			y*=rhs;
			return this;
		}
		/**
		 * /=操作
		 * @param rhs
		 * @return
		 */
		Vector2D divide(double rhs){
			x/=rhs;
			y/=rhs;
			return this;
		}
		/**
		 * 相乘
		 * @param lhs
		 * @param rhs
		 * @return
		 */
		static Vector2D multiply(Vector2D lhs,double rhs){
			Vector2D result=lhs.clone();
			return result.multiply(rhs);
		}
		/**
		 * 相乘
		 * @param lhs
		 * @param rhs
		 * @return
		 */
		static Vector2D multiply(double lhs,Vector2D rhs){
			Vector2D result=rhs.clone();
			return result.multiply(lhs);
		}
		/**
		 * 相减
		 * @param lhs
		 * @param rhs
		 * @return
		 */
		static Vector2D subtract(Vector2D lhs,Vector2D rhs){
			Vector2D result=lhs.clone();
			return result.subtract(rhs);
		}
		/**
		 * 2D矢量的长度
		 * @param v
		 * @return
		 */
		static double vec2DLen(Vector2D v){
			return Math.sqrt(v.x*v.x+v.y*v.y);
		}
		/**
		 * 2D矢量的规格化
		 * @param v
		 */
		static void vec2DNormalize(Vector2D v){
			double len=vec2DLen(v);
			v.x=v.x/len;
			v.y=v.y/len;
		}
		/**
		 * 两个矢量的点积，很有用，规格化后值为两个矢量的夹角
		 * @param v1
		 * @param v2
		 * @return
		 */
		static double vec2DDot(Vector2D v1,Vector2D v2){
			return v1.x*v2.x+v1.y*v2.y;
		}
		/**
		 * 当v1按顺时针方向转到v2时返回1，若逆时针则返回-1。这在实际中很有用
		 * @param v1
		 * @param v2
		 * @return
		 */
		static int vec2DSign(Vector2D v1,Vector2D v2){
			if(v1.y*v2.x>v1.x*v2.y){
				return 1;
			}else{
				return -1;
			}
		}
	}
	/**
	 * 矩阵，用于顶点的变比、旋转、平移操作
	 */
	static class Matrix2D{
		double _11,_12,_13,
			   _21,_22,_23,
			   _31,_32,_33;
		Matrix2D(){
			identity();
		}
		public String toString(){
			StringBuilder sb=new StringBuilder();
			sb.append("\n").append(_11).append(" ").append(_12).append(" ").append(_13)
			  .append("\n").append(_21).append(" ").append(_22).append(" ").append(_23)
			  .append("\n").append(_31).append(" ").append(_32).append(" ").append(_33);
			return sb.toString();
		}
		/**
		 * 
		 * @param mIn
		 */
		void multiply(Matrix2D mIn){
			Matrix2D m2=new Matrix2D();
			//first row
			m2._11=_11*mIn._11+_12*mIn._21+_13*mIn._31;
			m2._12=_11*mIn._12+_12*mIn._22+_13*mIn._32;
			m2._13=_11*mIn._13+_12*mIn._23+_13*mIn._33;
			//second
			m2._21=_21*mIn._11+_22*mIn._21+_23*mIn._31;
			m2._22=_21*mIn._12+_22*mIn._22+_23*mIn._32;
			m2._23=_21*mIn._13+_22*mIn._23+_23*mIn._33;
			//third
			m2._31=_31*mIn._11+_32*mIn._21+_33*mIn._31;
			m2._32=_31*mIn._12+_32*mIn._22+_33*mIn._32;
			m2._33=_31*mIn._13+_32*mIn._23+_33*mIn._33;
			_11=m2._11;_12=m2._12;_13=m2._13;
			_21=m2._21;_22=m2._22;_23=m2._23;
			_31=m2._31;_32=m2._32;_33=m2._33;
		}
		/**
		 *  创建一个单位矩阵
		 */
		private void identity(){
			_11=1.0f;_12=0.0f;_13=0.0f;
			_21=0.0f;_22=1.0f;_23=0.0f;
			_31=0.0f;_32=0.0f;_33=1.0f;
		}
		/**
		 * 创建一个变比矩阵
		 * @param xSca
		 * @param ySca
		 */
		void scale(double xSca,double ySca){
			Matrix2D m2=new Matrix2D();
			m2._11=xSca;m2._12=0.0f;m2._13=0.0f;
			m2._21=0.0f;m2._22=ySca;m2._23=0.0f;
			m2._31=0.0f;m2._32=0.0f;m2._33=1.0f;
			multiply(m2);
		}
		/**
		 * 创建一个旋转矩阵
		 * @param rot
		 */
		void rotate(double rot){
			Matrix2D m2=new Matrix2D();
			double sin=Math.sin(rot);
			double cos=Math.cos(rot);
			m2._11=cos; m2._12=sin; m2._13=0.0f;
			m2._21=-sin;m2._22=cos; m2._23=0.0f;
			m2._31=0.0f;m2._32=0.0f;m2._33=1.0f;
			multiply(m2);
		}
		/**
		 * 创建一个平移矩阵
		 * @param xTra
		 * @param yTra
		 */
		void translate(double xTra,double yTra){
			Matrix2D m2=new Matrix2D();
			m2._11=1.0f;m2._12=0.0f;m2._13=0.0f;
			m2._21=0.0f;m2._22=1.0f;m2._23=0.0f;
			m2._31=xTra;m2._32=yTra;m2._33=1.0f;
			multiply(m2);
		}
		/**
		 * 应用变换矩阵到顶点向量
		 * @param vPoints
		 */
		void transformPoints(Vector<Point> vPoints){
			for(int i=0;i<vPoints.size();i++){
				double tmpX=(_11*vPoints.get(i).x)+(_21*vPoints.get(i).y)+_31;
				double tmpY=(_12*vPoints.get(i).x)+(_22*vPoints.get(i).y)+_32;
				vPoints.get(i).x=tmpX;
				vPoints.get(i).y=tmpY;
			}
		}
	}
	/**
	 * 计时器，用于动画帧，调用自Kernel32的API 
	 */
	static class Timer{
		long m_nextTime,
			 m_frameTime;
		long[] m_perfCountFreq=new long[1],
			   m_currentTime=new long[1],
			   m_lastTime=new long[1];
		double m_timeElapsed,
			   m_timeScale;
		double m_FPS;
		Timer(){
			if(CLib.Kernel32.QueryPerformanceFrequency(m_perfCountFreq)){
				m_timeScale=1.0f/m_perfCountFreq[0];
			}else{
				pln("New Computer needed for Timer problem!");
			}
		}
		Timer(float fps){
			this.m_FPS=fps;
			CLib.Kernel32.QueryPerformanceFrequency(m_perfCountFreq);
			m_timeScale=1.0f/m_perfCountFreq[0];
			m_frameTime=(long)(m_perfCountFreq[0]/m_FPS);
		}
		//开始游戏循环
		void start(){
			CLib.Kernel32.QueryPerformanceCounter(m_lastTime);
			m_nextTime=m_lastTime[0]+m_frameTime;
		}
		boolean readyForNextFrame(){
			if(m_FPS==0){
				pln("No FPS set in timer!");
				return false;
			}
			CLib.Kernel32.QueryPerformanceCounter(m_currentTime);
			if(m_currentTime[0]>m_nextTime){
				m_timeElapsed=(m_currentTime[0]-m_lastTime[0])*m_timeScale;
				m_lastTime[0]=m_currentTime[0];
				m_nextTime=m_currentTime[0]+m_frameTime;
				return true;
			}
			return false;
		}
		double getTimeElapsed(){
			return m_timeElapsed;
		}
		double timeElapsed(){
			CLib.Kernel32.QueryPerformanceCounter(m_currentTime);
			m_timeElapsed=(m_currentTime[0]-m_lastTime[0])*m_timeScale;
			m_lastTime[0]=m_currentTime[0];
			return m_timeElapsed;
		}
		interface CLib extends Library{
			CLib Kernel32=(CLib)Native.loadLibrary("Kernel32", CLib.class);
			boolean QueryPerformanceFrequency(long[] lpFrequency);
			boolean QueryPerformanceCounter(long[] lpCount);
		}
	}
}
