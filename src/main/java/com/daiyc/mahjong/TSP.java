/**
 * 
 */
package com.daiyc.mahjong;

import static com.daiyc.mahjong.Util.pf;
import static com.daiyc.mahjong.Util.pln;

import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.daiyc.mahjong.TSP.GaTSP.CrossoverType;
import com.daiyc.mahjong.TSP.GaTSP.MutationType;
import com.daiyc.mahjong.TSP.GaTSP.ScaleType;
import com.daiyc.mahjong.TSP.GaTSP.SelectionType;

/**
 * 人工智能-遗传算法-巡回销售员问题
 * @author Administrator
 *
 */
public class TSP extends JPanel{
	private static final long serialVersionUID = 1L;
	static class MapTSP{
		Vector<CoOrd> vecCityCoOrds;
		int numCities;//地图中城市数目
		int mapWidth;
		int mapHeight;
		double dBestPossibleRoute;//如果解可以计算，即保存解的长度
		final static double PI=3.1415926535897;
		final static double EPSILON=0.000001;
		void createCitiesCircular(){
			int margin=50;
			double radius;
			if(mapHeight<mapWidth){
				radius=(mapHeight/2)-margin;
			}else{
				radius=(mapWidth/2)-margin;
			}
			CoOrd orgin=new CoOrd(mapWidth/2,mapHeight/2);
			double segmentSize=2*PI/numCities;
			double angle=0;
			while(angle<2*PI){
				CoOrd thisCity=new CoOrd();
				thisCity.x=radius*Math.sin(angle)+orgin.x;
				thisCity.y=radius*Math.cos(angle)+orgin.y;
				vecCityCoOrds.add(thisCity);
				angle+=segmentSize;
			}
		}
		double calcA_to_B(CoOrd city1,CoOrd city2){
			double xDist=city1.x-city2.x;
			double yDist=city1.y-city2.y;
			return Math.sqrt(xDist*xDist+yDist*yDist);
		}
		//为环形计算最佳周游路径
		void calcBestPossibleRoute(){
			dBestPossibleRoute=0;
			for(int city=0;city<vecCityCoOrds.size()-1;city++){
				dBestPossibleRoute+=calcA_to_B(vecCityCoOrds.get(city),
						vecCityCoOrds.get(city+1));
				dBestPossibleRoute+=EPSILON;//加上一个很小的数覆盖可能的误差
			}
			//加上从最后到第一个的距离
			dBestPossibleRoute+=calcA_to_B(vecCityCoOrds.get(vecCityCoOrds.size()-1), 
					vecCityCoOrds.get(0));
		}
		MapTSP(int w,int h,int nc){
			mapWidth=w;
			mapHeight=h;
			numCities=nc;
			vecCityCoOrds=new Vector<CoOrd>();
			createCitiesCircular();
			calcBestPossibleRoute();
		}
		//改变窗口尺寸
		void resize(int newWidth,int newHeight){
			mapWidth=newWidth;
			mapHeight=newHeight;
			vecCityCoOrds.clear();
			createCitiesCircular();
			calcBestPossibleRoute();
		}
		double getTourLen(Vector<Integer> route){
			double totalDistance=0;
			for(int i=0;i<route.size()-1;i++){
				int city1=route.get(i);
				int city2=route.get(i+1);
				totalDistance+=calcA_to_B(vecCityCoOrds.get(city1),
						vecCityCoOrds.get(city2));
			}
			//因为是闭环，所以要从最后一个访问回第一个
			int last=route.get(route.size()-1);
			int first=route.get(0);
			totalDistance+=calcA_to_B(vecCityCoOrds.get(last),
					vecCityCoOrds.get(first));
			return totalDistance;
		}
	}
	//坐标
	static class CoOrd{
		double x,y;
		CoOrd(){}
		CoOrd(double a,double b){
			x=a;y=b;
		}
	}
	//染色体（基因组）
	static class Genome{
		Vector<Integer> vecCities;//城市周游路径（基因组）
		double dFitness;//适应分
		Genome(){
			vecCities=new Vector<Integer>();
		}
		Genome(int nc){
			vecCities=grabPermutation(nc);
		}
		//创建一个随机的城市周游路径
		Vector<Integer> grabPermutation(int nc){
			Vector<Integer> vecPerm=new Vector<Integer>();
			Random rand=new Random();
			for(int i=0;i<nc;i++){
				int nextPossibleNum=rand.nextInt(nc);
				while(testNumber(vecPerm,nextPossibleNum)){
					nextPossibleNum=rand.nextInt(nc);
				}
				vecPerm.add(nextPossibleNum);
			}
			return vecPerm;
		}
		boolean testNumber(Vector<Integer> vec,int num){
			for(int i=0;i<vec.size();i++){
				if(vec.get(i)==num){
					return true;
				}
			}
			return false;
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
	static class GaTSP implements Runnable{
		Vector<Genome> vecPop;//种群
		MapTSP pMap;//地图
		int iPopSize;//种群中基因组的数目
		int iChromoLen;//染色体长度
		int iFittestGenome;//最新一代中适应分最高的成员
		double dShortestRoute;//在此之前找到的最短周游路径
		double dLongestRoute;//在此之前找到的最长周游路径
		double dBestFitness;//最好的适应分
		double dWorstFitness;//最差的适应分
		double dTotalFitness;//整个种群的适应分总和
		double dAverageFitness;//整个种群的适应分平均分
		double dSigma;//当值为0时，种群内是相同的成员
		double dBoltzmannTemp;//用于波兹曼变比
		double dMutationRate;//突变率
		double dCrossoverRate;//杂交率
		int iGeneration;//表明已经到了哪一代
		boolean bStarted;//是否已进入绘图阶段
		boolean bElitism;//精英选择开关
		//boolean bSorted;//种群是否已按适应分排序
		ScaleType scaleType;//变比算子类型
		static enum ScaleType{
			NONE,SIGMA,BOLTZMANN,RANK
		}
		SelectionType selectType;//选择算子类型
		static enum SelectionType{
			//SUS,
			ROULETTE,TOURNAMENT,ALT_TOURNAMENT
		}
		CrossoverType crossType;//杂交算子类型
		static enum CrossoverType{
			PBX,OBX,PMX
		}
		MutationType mutType;//突变算子类型
		static enum MutationType{
			EM,DM,SM,IM
		}
		static int NUM_CITIES=120;
		final static int CITY_SIZE=5;
		final static double CROSS_RAT=0.75;//杂交率
		final static double MUT_RAT=0.2;//突变率
		final static int POP_SIZE=200;//种群大小
		final static int WIN_WIDTH=500;
		final static int WIN_HEIGHT=500;
		final static int SLEEP_MS=20;
		final static int NUM_TO_COMPARE=5;
		final static double CTOURNAMENT=0.75;//用于可选锦标赛选择常量
		final static int BOLTZMANN_MIN_TEMP=1;//用于波兹曼变比
		final static double BOLTZMANN_DT=0.05;//用于波兹曼变比
		GaTSP(double mutRat,double crossRat,int popSize,int numCities,int mapWidth,int mapHeight){
			dMutationRate=mutRat;
			dCrossoverRate=crossRat;
			iPopSize=popSize;
			iChromoLen=numCities;
			pMap=new MapTSP(mapWidth,mapHeight,numCities);
			vecPop=new Vector<Genome>();
			dSigma=1;
		}
		void setScaleType(ScaleType scaleType){
			this.scaleType=scaleType;
		}
		void setElitism(boolean elitism){
			this.bElitism=elitism;
		}
		void setSelectionType(SelectionType selectType){
			this.selectType=selectType;
		}
		void setMutationType(MutationType mutType){
			this.mutType=mutType;
		}
		void setCrossoverType(CrossoverType crossType){
			this.crossType=crossType;
		}
		void createStartPopulation(){
			vecPop.clear();
			for(int i=0;i<iPopSize;i++){
				vecPop.add(new Genome(iChromoLen));
			}
			iGeneration=0;
			dShortestRoute=9999999;
			dLongestRoute=0;
			dBestFitness=0;
			dWorstFitness=9999999;
			dTotalFitness=0;
			dAverageFitness=0;
			iFittestGenome=0;
			bStarted=false;
		}
		void reset(){
			dShortestRoute=9999999;
			dLongestRoute=0;
			dBestFitness=0;			
			dWorstFitness=9999999;
			dTotalFitness=0;
			dAverageFitness=0;
//			bSorted=false;
			dSigma=1;
		}
		//==================计算适应分===========================================================
		void calcPopulationFitness(){
			for(int i=0;i<iPopSize;i++){
				double tourLen=pMap.getTourLen(vecPop.get(i).vecCities);
				vecPop.get(i).dFitness=tourLen;
				if(tourLen<dShortestRoute){
					dShortestRoute=tourLen;//在每一代中保存最短（即最优）的路程长度
				}
				if(tourLen>dLongestRoute){
					dLongestRoute=tourLen;//在每一代中保存最长（即最差）的路程长度
				}
			}
			//计算完所有周游路线的路程长度，下一步计算它们的适应性分数
			for(int i=0;i<iPopSize;i++){
				vecPop.get(i).dFitness=dLongestRoute-vecPop.get(i).dFitness;
			}
			calcBestWorstAvTot();
		}
		private void calcBestWorstAvTot(){
			dTotalFitness=0;
			double highestSoFar=-9999999;
			double lowestSoFar=9999999;
			for(int i=0;i<iPopSize;i++){
				if(vecPop.get(i).dFitness>highestSoFar){
					highestSoFar=vecPop.get(i).dFitness;
					iFittestGenome=i;
					dBestFitness=highestSoFar;
				}
				if(vecPop.get(i).dFitness<lowestSoFar){
					lowestSoFar=vecPop.get(i).dFitness;
					dWorstFitness=lowestSoFar;
				}
				dTotalFitness+=vecPop.get(i).dFitness;
			}
			dAverageFitness=dTotalFitness/iPopSize;
			if(dAverageFitness==0){
				dSigma=0;
			}
		}
		//=============适应分变比===========================================
		void fitnessScaleSwitch(){
			switch(scaleType){
			case NONE:
				break;
			case RANK:
				fitnessScaleRank();break;
			case SIGMA:
				fitnessScaleSigma();break;
			case BOLTZMANN:
				fitnessScaleBoltzmann();break;
			}
		}
		//排名变比
		private void fitnessScaleRank() {
			for(int i=0;i<iPopSize;i++){
				vecPop.get(i).dFitness=i;
			}
			calcBestWorstAvTot();
		}
		//西格玛变比
		private void fitnessScaleSigma() {
			double runningTotal=0;
			for(int i=0;i<iPopSize;i++){
				double dgf=vecPop.get(i).dFitness;
				runningTotal+=(dgf-dAverageFitness)*(dgf-dAverageFitness);
			}
			dSigma=Math.sqrt(runningTotal/iPopSize);
			for(int i=0;i<iPopSize;i++){
				double oldFitness=vecPop.get(i).dFitness;
				vecPop.get(i).dFitness=(oldFitness-dAverageFitness)/(2*dSigma);
			}
			calcBestWorstAvTot();
		}
		//波兹曼变比
		private void fitnessScaleBoltzmann() {
			dBoltzmannTemp-=BOLTZMANN_DT;
			if(dBoltzmannTemp<BOLTZMANN_MIN_TEMP){//确保不会下降到最小值
				dBoltzmannTemp=BOLTZMANN_MIN_TEMP;
			}
			double divider=dAverageFitness/dBoltzmannTemp;
			for(int i=0;i<iPopSize;i++){
				double oldFitness=vecPop.get(i).dFitness;
				vecPop.get(i).dFitness=(oldFitness/dBoltzmannTemp)/divider;
			}
			calcBestWorstAvTot();
		}
		//==============精英选择===========================================
		//从种群中最佳的nBest个个体中选择相应的copyToAdd个复制到下一代
		//实际中总结得出，保留占种群总体的2%~5%数量的个体，效果最为理想。
		void grabNBest(int nBest,int copyToAdd,Vector<Genome> vecNewPop){
			while(nBest-->0){
				for(int i=0;i<copyToAdd;i++){
					vecNewPop.add(vecPop.get(iPopSize-1-nBest));
				}
			}
		}
		//==============选择===============================================
		//赌轮选择
		Genome rouletteWheelSelection(){
			double slice=new Random().nextFloat()*dTotalFitness;
			double fitnessSoFar=0;
			int selected=0;
			for(int i=0;i<iPopSize;i++){
				fitnessSoFar+=vecPop.get(i).dFitness;
				if(fitnessSoFar>slice){
					selected=i;
					break;
				}
			}
			return vecPop.get(selected);
		}
		//锦标赛选择
		Genome tournamentSelection(int num){
			double bestFitnessSoFar=0;
			int chosenOne=0;
			for(int i=0;i<num;i++){
				int thisTry=randInt(0,iPopSize-1);
				if(vecPop.get(thisTry).dFitness>bestFitnessSoFar){
					chosenOne=thisTry;
					bestFitnessSoFar=vecPop.get(thisTry).dFitness;
				}
			}
			return vecPop.get(chosenOne);//返回冠军
		}
		//可选的锦标赛选择
		Genome alternativeTournamentSelection(){
			int g1=randInt(0,iPopSize-1);
			int g2=randInt(0,iPopSize-1);
			while(g1==g2){//确保g1和g2不同
				g2=randInt(0,iPopSize-1);
			}
			if(new Random().nextFloat()<CTOURNAMENT){
				if(vecPop.get(g1).dFitness>vecPop.get(g2).dFitness){
					return vecPop.get(g1);
				}else{
					return vecPop.get(g2);
				}
			}else{
				if(vecPop.get(g1).dFitness<vecPop.get(g2).dFitness){
					return vecPop.get(g1);
				}else{
					return vecPop.get(g2);
				}
			}
		}
		//随机遍及取样选择
		void SUSSelection(){
		}
		//=============杂交========================================================
		void crossover(Vector<Integer> mum,Vector<Integer> dad,
				Vector<Integer> baby1,Vector<Integer> baby2){
			switch(crossType){
			case PBX:
				crossoverPBX(mum,dad,baby1,baby2);break;
			case OBX:
				crossoverOBX(mum,dad,baby1,baby2);break;
			default:
				crossoverPMX(mum,dad,baby1,baby2);
			}
		}
		//部分匹配杂交
		private void crossoverPMX(Vector<Integer> mum, Vector<Integer> dad, Vector<Integer> baby1,
				Vector<Integer> baby2) {
			baby1.addAll(mum);
			baby2.addAll(dad);
			Random rand=new Random();
			//是否立即返回取决于杂交率，或者要看两个父辈染色体是否相同
			if(rand.nextFloat()>dCrossoverRate||mum.equals(dad)){
				return;
			}
			//首先选择染色体的一节的开始点beg
			int beg=rand.nextInt(mum.size()-1);
			int end=beg;
			while(end<=beg){//再选择一个结束点，不能小于等于开始点
				end=rand.nextInt(mum.size());
			}
			//从beg到end，依次寻找匹配的基因对，病交换两个子染色体中的位置
			for(int p=beg;p<end+1;p++){
				int g1=mum.get(p);
				int g2=dad.get(p);
				if(g1!=g2){
					swap(baby1,g1,g2);
					swap(baby2,g1,g2);
				}
			}
		}
		//基于顺序的杂交
		private void crossoverOBX(Vector<Integer> mum, Vector<Integer> dad, Vector<Integer> baby1,
				Vector<Integer> baby2) {
			baby1.addAll(mum);
			baby2.addAll(dad);
			Random rand=new Random();
			//是否立即返回取决于杂交率，或者要看两个父辈染色体是否相同
			if(rand.nextFloat()>dCrossoverRate||mum.equals(dad)){
				return;
			}
			Vector<Integer> tmpCities=new Vector<Integer>();//保存选择的城市
			Vector<Integer> tmpPos=new Vector<Integer>();//保存所选城市的位置
			int pos=randInt(0,mum.size()-2);
			while(pos<mum.size()){
				tmpPos.add(pos);
				tmpCities.add(mum.get(pos));
				pos+=randInt(1,mum.size()-pos);//下一个城市
			}
			int cPos=0;
			//从mum中取得n个城市放到向量，讲它们的次序强加到dad中
			for(int i=0;i<baby2.size();i++){
				for(int j=0;j<tmpCities.size();j++){
					if(baby2.get(i)==tmpCities.get(j)){
						baby2.set(i, tmpCities.get(cPos++));
						break;
					}
				}
			}
			//反过来，从dad中选择同样位置的城市，讲它们的顺序强加到mum上
			tmpCities.clear();
			cPos=0;
			for(int i=0;i<tmpPos.size();i++){
				tmpCities.add(dad.get(tmpPos.get(i)));
			}
			for(int i=0;i<baby1.size();i++){
				for(int j=0;j<tmpCities.size();j++){
					if(baby1.get(i)==tmpCities.get(j)){
						baby1.set(i, tmpCities.get(cPos++));
						break;
					}
				}
			}
		}
		// >=beg <=end
		private int randInt(int beg,int end){
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
		//基于位置的杂交
		private void crossoverPBX(Vector<Integer> mum, Vector<Integer> dad, Vector<Integer> baby1,
				Vector<Integer> baby2) {
			Random rand=new Random();
			//是否立即返回取决于杂交率，或者要看两个父辈染色体是否相同
			if(rand.nextFloat()>dCrossoverRate||mum.equals(dad)){
				baby1.addAll(mum);
				baby2.addAll(dad);
				return;
			}
			//将babies初始化为-1，使后面的算法能够知道哪一个位置已经被填入
			for(int i=0;i<mum.size();i++){
				baby1.add(-1);
				baby2.add(-1);
			}
			Vector<Integer> tmpPos=new Vector<Integer>();//保存所选城市的位置
			int pos=randInt(0, mum.size()-2);
			while(pos<mum.size()){//不断的随机加入城市，直到走到无法再记录位置为止
				tmpPos.add(pos);
				pos+=randInt(1, mum.size()-pos);
			}
			//将所选的城市复制到子代相同的位置
			for(int p=0;p<tmpPos.size();p++){
				baby1.set(tmpPos.get(p), mum.get(tmpPos.get(p)));//baby1从mum那接受这些城市
				baby2.set(tmpPos.get(p), dad.get(tmpPos.get(p)));//baby2从dad那接受这些城市
			}
			int c1=0,c2=0;
			for(int p=0;p<mum.size();p++){
				while((c2<mum.size())&&(baby2.get(c2)>-1)){
					c2++;
				}
				if(!testNumber(baby2,mum.get(p))){
					baby2.set(c2, mum.get(p));
				}
				while((c1<mum.size())&&(baby1.get(c1)>-1)){
					c1++;
				}
				if(!testNumber(baby1,dad.get(p))){
					baby1.set(c1, dad.get(p));
				}
			}
		}
		private boolean testNumber(Vector<Integer> vec,int num){
			for(int i=0;i<vec.size();i++){
				if(vec.get(i)==num){
					return true;
				}
			}
			return false;
		}
		private void swap(Vector<Integer> vec,int g1,int g2){
			int posGene1=vec.indexOf(g1);
			int posGene2=vec.indexOf(g2);
			vec.set(posGene1, g2);
			vec.set(posGene2, g1);
		}
		private void swap2(Vector<Integer> vec,int g1Pos,int g2Pos){
			int g1=vec.get(g1Pos);
			int g2=vec.get(g2Pos);
			vec.set(g1Pos, g2);
			vec.set(g2Pos, g1);
		}
		//============突变===========================================
		void mutate(Vector<Integer> chromo){
			switch(mutType){
			case SM:
				mutateSM(chromo);break;
			case DM:
				mutateDM(chromo);break;
			case EM:
				mutateEM(chromo);break;
			default:
				mutateIM(chromo);
			}
		}
		//交换突变
		private void mutateSM(Vector<Integer> chromo) {
			Random rand=new Random();
			if(rand.nextFloat()>dMutationRate)return;
			//选择第一个基因
			int pos1=rand.nextInt(chromo.size());
			//选择第二个基因
			int pos2=pos1;
			while(pos1==pos2){
				pos2=rand.nextInt(chromo.size());
			}
			//交换它们的位置
			swap(chromo,chromo.get(pos1),chromo.get(pos2));
		}
		//散播突变
		private void mutateDM(Vector<Integer> chromo) {
			Random rand=new Random();
			if(rand.nextFloat()>dMutationRate)return;
			//声明一个最小的span尺寸
			int minSpan=3;
			int[] beg=new int[1],end=new int[1];
			//在染色体上随机取一个段
			chooseSection(beg,end,chromo.size(),minSpan);
			int span=end[0]-beg[0];
			int numOfSwapsRqd=span;
			while(--numOfSwapsRqd>0){
				int g1Pos=beg[0]+rand.nextInt(span+1);
				int g2Pos=beg[0]+rand.nextInt(span+1);
				swap2(chromo,g1Pos,g2Pos);
			}
		}
		//移位突变
		@SuppressWarnings("unchecked")
		private void mutateEM(Vector<Integer> chromo) {
			Random rand=new Random();
			if(rand.nextFloat()>dMutationRate)return;
			//声明一个最小的span尺寸
			int minSpan=3;
			int[] beg=new int[1],end=new int[1];
			//在染色体上随机取一个段
			chooseSection(beg,end,chromo.size(),minSpan);
			Vector<Integer> chromo1=(Vector<Integer>)chromo.clone();
			List<Integer> theSection=chromo1.subList(beg[0], end[0]);
			chromo.removeAll(theSection);
			int randPos=rand.nextInt(chromo.size());
			chromo.addAll(randPos,theSection);
		}
		//给定一个最大范围和最小范围，在这个范围内计算一个随机的起点和终点，
		//主要用于突变和杂交操作
		private void chooseSection(int[] beg, int[] end, int maxSpan, int minSpan) {
			Random rand=new Random();
			beg[0]=rand.nextInt(maxSpan-minSpan);
			end[0]=beg[0];
			while(end[0]<=beg[0]){
				end[0]=rand.nextInt(maxSpan);
			}
		}
		//插入突变
		private void mutateIM(Vector<Integer> chromo) {
			Random rand=new Random();
			if(rand.nextFloat()>dMutationRate)return;
			int randPos=rand.nextInt(chromo.size());
			int cityNum=chromo.remove(randPos);
			randPos=rand.nextInt(chromo.size());
			chromo.add(randPos,cityNum);
		}
		//倒置突变
		//倒置移位突变
		
		//时代方法
		void epoch(){
			reset();
			calcPopulationFitness();
			pf("第%d代[最短路径=%f，目标最短路径=%f，最佳适应分=%f]\n",iGeneration,dShortestRoute,pMap.dBestPossibleRoute,dBestFitness);
			if(dShortestRoute<=pMap.dBestPossibleRoute){
				pln("#end by dShortestRoute<=pMap.dBestPossibleRoute#"+
						dShortestRoute+","+pMap.dBestPossibleRoute);
				stop();
				return;
			}
			fitnessScaleSwitch();
			if(dSigma==0){
				pln("#end by dSigma==0#"+dSigma);
				stop();
				return;
			}
			Genome.sort(vecPop);
			Vector<Genome> vecNewPop=new Vector<Genome>();
			//精英选择
			if(bElitism){
				int copyToAdd=2,nBest=4;
				if((copyToAdd*nBest)%2==0){
					grabNBest(nBest, copyToAdd, vecNewPop);
				}
			}
			while(vecNewPop.size()!=iPopSize){
				Genome mum=null,dad=null;
				switch(selectType){
				case ROULETTE:
					mum=rouletteWheelSelection();
					dad=rouletteWheelSelection();
					break;
				case TOURNAMENT:
					mum=tournamentSelection(NUM_TO_COMPARE);
					dad=tournamentSelection(NUM_TO_COMPARE);
					break;
				case ALT_TOURNAMENT:
					mum=alternativeTournamentSelection();
					dad=alternativeTournamentSelection();
					break;
				}
				Genome baby1=new Genome();
				Genome baby2=new Genome();
				crossover(mum.vecCities, dad.vecCities, baby1.vecCities, baby2.vecCities);
				mutate(baby1.vecCities);
				mutate(baby2.vecCities);
				vecNewPop.add(baby1);
				vecNewPop.add(baby2);
			}
			vecPop=vecNewPop;
			iGeneration++;
		}
		void render(Graphics g){
			for(int cityNum=0;cityNum<pMap.vecCityCoOrds.size();cityNum++){
				int x=(int)pMap.vecCityCoOrds.get(cityNum).x;
				int y=(int)pMap.vecCityCoOrds.get(cityNum).y;
				g.drawRect(x, y, CITY_SIZE, CITY_SIZE);
			}
			if(iGeneration>0){
				Vector<Integer> route=vecPop.get(iFittestGenome).vecCities;
				CoOrd start=pMap.vecCityCoOrds.get(route.get(0));
				int begX=(int)start.x;
				int begY=(int)start.y;
				for(int i=1;i<route.size();i++){
					int cityX=(int)pMap.vecCityCoOrds.get(route.get(i)).x;
					int cityY=(int)pMap.vecCityCoOrds.get(route.get(i)).y;
					g.drawLine(begX, begY, cityX, cityY);
					begX=cityX;
					begY=cityY;
					if(NUM_CITIES<100){
						g.drawString(""+i,cityX,cityY);
					}
				}
				int endX=(int)start.x;
				int endY=(int)start.y;
				g.drawLine(begX, begY, endX, endY);
				if(NUM_CITIES<100){
					g.drawString(""+route.size(), endX, endY);
				}
			}
			g.drawString("Generation: "+iGeneration, 25,505);
			if(!bStarted){
				g.drawString("Press Enter to start a new run", 315, 505);
				if(dSigma==0){
					g.drawString("Premature Convergence!", 165, 505);
				}
			}else{
				g.drawString("Press Space to stop", 315, 505);
			}
			g.drawString("适应分变比算子="+this.scaleType, 25, 525);
			g.drawString("精英选择="+this.bElitism, 195, 525);
			g.drawString("选择算子="+this.selectType, 305, 525);
			g.drawString("杂交算子="+this.crossType, 25, 545);
			g.drawString("突变算子="+this.mutType, 195, 545);
		}
		void start(){
			createStartPopulation();
			bStarted=true;
		}
		@Override
		public void run() {
			while(started()){
				try {
					Thread.sleep(SLEEP_MS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				epoch();
			}
		}
		void stop(){
			bStarted=false;
		}
		private boolean started() {
			return bStarted;
		}
	}
	GaTSP gt;
	public void paint(Graphics g){
		super.paint(g);
		gt.render(g);
	}
	public void toDraw(){
		while(gt.started()){
			this.repaint();
			try {
				Thread.sleep(GaTSP.SLEEP_MS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.repaint();//最后的结果补画一次
	}
	public static void main(String[] args) {
		JFrame f=new JFrame();
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		f.setBounds(500, 150, GaTSP.WIN_WIDTH+20, GaTSP.WIN_HEIGHT+100);
		f.setVisible(true);
		
		final TSP tsp=new TSP();
		f.setContentPane(tsp);
		final GaTSP gt=new GaTSP(GaTSP.CROSS_RAT,GaTSP.MUT_RAT,GaTSP.POP_SIZE,
				GaTSP.NUM_CITIES,GaTSP.WIN_WIDTH,GaTSP.WIN_HEIGHT);
		gt.setScaleType(ScaleType.BOLTZMANN);
		gt.setElitism(true);
		gt.setSelectionType(SelectionType.TOURNAMENT);
		gt.setCrossoverType(CrossoverType.PBX);
		gt.setMutationType(MutationType.IM);
		tsp.gt=gt;
		f.addKeyListener(new KeyAdapter(){
			public void keyPressed(final KeyEvent ke){
				new Thread(new Runnable(){
					@Override
					public void run() {
						if(ke.getKeyCode()==KeyEvent.VK_ENTER){
							if(!gt.started()){
								gt.start();
								new Thread(gt).start();
								tsp.toDraw();
							}
						}else if(ke.getKeyCode()==KeyEvent.VK_SPACE){
							gt.stop();
						}
					}
				}).start();
			}
		});
		f.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(ComponentEvent e) {
				gt.pMap.resize(e.getComponent().getWidth(), e.getComponent().getHeight());
			}
		});
	}
}