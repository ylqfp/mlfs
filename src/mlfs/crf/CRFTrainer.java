/*
 * CRFTrainer.java 
 * 
 * Author : 罗磊，luoleicn@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Last Update:Jul 8, 2011
 * 
 */
package mlfs.crf;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import mlfs.crf.graph.Graph;
import mlfs.crf.model.CRFEvent;
import mlfs.crf.model.CRFModel;
import mlfs.util.Utils;

/**
 * The Class CRFTrainer.
 * CRF训练模型的抽象方法，GIS求参和LBFGS求参均调用这个类
 */
public abstract class CRFTrainer {
	
	/** The logger. */
	private Logger logger = Logger.getLogger(CRFTrainer.class.getName());
	
	/** 特征处理类. */
	protected Features m_featHandler;
	
	/** 特征总数. */
	protected int m_numFeat;
	
	/** 训练语料event总数. */
	protected int m_numEvents;
	
	/** 训练语料中的所有event. */
	protected List<CRFEvent> m_events;
	
//	/** 观测期望. */
//	protected double[][] m_observationExpectation;
	
	/** 模型估测期望. */
	protected double[][] m_modelExpectation;
	
	/** 模型参数. */
	protected double[][] m_parameters;
	
	/** tag和整数对应的map. */
	protected Map<String, Integer> m_tagMap;
	
	/** tag总数. */
	protected int m_numTag;
	
//	protected int START;
//	protected int END;
	
	/**
	 * Instantiates a new cRF trainer.
	 *
	 * @param events the events
	 * @param featHandler the feat handler
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public CRFTrainer(List<CRFEvent> events, Features featHandler) throws IOException
	{
		m_numFeat = featHandler.getFeatNum();
		m_featHandler = featHandler;
		
		m_numEvents = events.size();
		m_events = events;
		
	
		m_tagMap = featHandler.getTagMap();
		m_numTag = m_tagMap.size();
		
		logger.info("There are " + m_numFeat + " features in training file");
		
//		START = m_tagMap.get("START");
//		END = m_tagMap.get("END");
	}
	
	/**
	 * Train.使用默认的迭代求参次数，训练crf模型
	 *
	 * @return the cRF model
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public abstract CRFModel train() throws IOException;
	
	/**
	 * Train.指定迭代次数，训练crf模型
	 *
	 * @param n the n
	 * @return the cRF model
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public abstract CRFModel train(int n) throws IOException;
	
//	/**
//	 * 计算观测期望，不再直接计算，而是通过建立好的Graph，从modelExpectation中减去
//	 */
//	protected void calcObservationExpectation()
//	{
//		for (CRFEvent event : m_events)
//		{
//			for (int i=0; i<=event.inputs.length; i++)//最后一个为end
//			{
//				List<Integer> feats = null;
//				if (i == 0)
//					feats = m_featHandler.getFeatures(event, START, i);
//				else
//					feats = m_featHandler.getFeatures(event, event.labels[i-1],  i);
//				
//				for (int f : feats)
//					if (i == event.inputs.length)
//						m_observationExpectation[f][END]+= 1.0;
//					else
//						m_observationExpectation[f][event.labels[i]] += 1.0;
//			}
//		}
//		
//	}
	
	/**
	 * 计算模型估计期望.
	 *
	 * @param solutions the solutions
	 * @return the double[]
	 */
	protected double calcModelExpectation(double[] solutions)
	{
		double[] logZx = new double[m_numEvents];
		int k = 0;
		for (int i=0; i<m_numFeat; i++)
			for (int j=0; j<m_numTag; j++)
				m_modelExpectation[i][j] = 0.0;
		
		double negLoglikelihood = 0.0;
		for (CRFEvent event : m_events)
		{
			Graph graph = Graph.buildGraph(event, m_featHandler, solutions);
			graph.forwardBackword();
			negLoglikelihood += graph.gradient(m_modelExpectation);
		}
		return negLoglikelihood;
//			int times = event.inputs.length;
//			double[][][] logM = new double[times+1][m_numTag][m_numTag];
//			double[][] logAlpha = new double[m_numTag][times];
//			double[][] logBeta = new double[m_numTag][times];
//			
//			calcMatrix(logM, event, solutions);
//			forword(logAlpha, times, logM);
//			backword(logBeta, times, logM);
//			
//			//compute Zx[]
//			boolean flag = true;
//			for (int tag = 0; tag<m_numTag; tag++)
//			{
//				if (tag==START || tag==END)
//					continue;
//				logZx[k] =  Utils.logSum(logZx[k], logM[0][START][tag] + logBeta[tag][0], flag);
//				flag = false;
//			}
//			
//			System.out.println(logZx[k]);
//			//compute m_modelExpectation
//			for (int t=0; t<=times; t++)
//			{
//				if (t == 0)
//				{
//					List<Integer> unigramFeats = m_featHandler.getUnigramFeat(event, t);
//					List<Integer> bigramFeats = m_featHandler.getBigramFeat(event, START, t);
//					for (int tag=0; tag<m_numTag; tag++)
//					{
//						if (tag==START || tag==END)
//							continue;
//						for (int f : unigramFeats)
//						{
//							double	tmp =  logM[t][START][tag] + logBeta[tag][t];				
//							m_modelExpectation[f][tag] += Math.exp(tmp - logZx[k]);
//						}
//							
//						for (int f : bigramFeats)
//						{
//							double	tmp =  logM[t][START][tag] + logBeta[tag][t];						
//							m_modelExpectation[f][tag] += Math.exp(tmp - logZx[k]);
//						}
//						
//					}
//				}
//				else if (t == times)//END
//				{
//					for (int preTag=0; preTag<m_numTag; preTag++)
//					{
//						if (preTag==START || preTag==END)
//							continue;
//						List<Integer> unigramFeats = m_featHandler.getUnigramFeat(event, t);
//						List<Integer> bigramFeats = m_featHandler.getBigramFeat(event, preTag, t);
//						for (int f : unigramFeats)
//						{
//							double	tmp =  logAlpha[preTag][t-1] + logM[t][preTag][END] ;
//							m_modelExpectation[f][END] += Math.exp(tmp - logZx[k]);
//						}
//						
//						for (int f : bigramFeats)
//						{
//							double	tmp =  logAlpha[preTag][t-1] + logM[t][preTag][END] ;
//							m_modelExpectation[f][END] += Math.exp(tmp - logZx[k]);
//						}						
//					}
//				}
//				else//t != 0 && t!=times
//				{
//					for (int preTag=0; preTag<m_numTag; preTag++)
//					{
//						if (preTag==START || preTag==END)
//							continue;
//						List<Integer> unigramFeats = m_featHandler.getUnigramFeat(event, t);
//						List<Integer> bigramFeats = m_featHandler.getBigramFeat(event, preTag, t);
//						for (int tag=0; tag<m_numTag; tag++)
//						{
//							if (tag==START || tag==END)
//								continue;
//							for (int f : unigramFeats)
//							{
//								double	tmp =  logAlpha[preTag][t-1] + logM[t][preTag][tag] + logBeta[tag][t];
//								m_modelExpectation[f][tag] += Math.exp(tmp - logZx[k]);
//							}
//								
//							for (int f : bigramFeats)
//							{
//								double	tmp =  logAlpha[preTag][t-1] + logM[t][preTag][tag] + logBeta[tag][t];
//								m_modelExpectation[f][tag] += Math.exp(tmp - logZx[k]);
//							}						
//						}
//					}
//				}
//				
//			}
//			k++;
//		}
//		return logZx;
	}
	
	
//	/**
//	 * 计算CRF论文中的M矩阵
//	 * 矩阵中存储的不是M本身，而是LogM
//	 *
//	 * @param logM the log m
//	 * @param event the event
//	 * @param solutions the solutions
//	 */
//	protected void calcMatrix(double[][][] logM, CRFEvent event, double[] solutions)
//	{
//		int len = event.inputs.length;
//		//i == 0
//		List<Integer> feats = m_featHandler.getFeatures(event, START, 0);
//		for (int tag=0; tag<m_numTag; tag++)
//		{
//			if (tag==START || tag==END)
//				continue;
//			for (int f : feats)
//				logM[0][START][tag] += solutions[f*m_numTag+tag];
//		}
//		// i== 1 to len-1
//		for (int i=1; i<len; i++)
//		{
//			for (int preTag=0; preTag<m_numTag; preTag++)
//			{
//				if (preTag==START||preTag==END)
//					continue;
//				feats = m_featHandler.getFeatures(event, preTag, i);
//				for (int tag=0; tag<m_numTag; tag++)
//				{
//					if (tag==START || tag==END)
//						continue;
//					for (int f : feats)
//						logM[i][preTag][tag] += solutions[f*m_numTag+tag];
//				}
//			}
//		}
//		//i == len
//		for (int preTag=0; preTag<m_numTag; preTag++)
//		{
//			if (preTag==START || preTag==END)
//				continue;
//			feats = m_featHandler.getFeatures(event, preTag, len);
//			for (int f : feats)
//				logM[len][preTag][END] += solutions[f*m_numTag+END];
//		}
//		
//	}
//	
//	/**
//	 * 前向算法，
//	 * 矩阵中保存的不是前向算法的数值本身，而是log值
//	 *
//	 * @param logAlpha the log alpha
//	 * @param times the times
//	 * @param logM the log m
//	 */
//	protected void forword(double[][] logAlpha, int times, double[][][] logM)
//	{
//		int time = 0;
//		for (int y=0; y<m_numTag; y++)
//		{
//			if (y==START || y==END)
//				continue;
////			logAlpha[y][time] = logM[time][START][y];
//			logAlpha[y][time] = 0.0;
//		}
//		
//		boolean flag = true;
//		for (time=1; time<times; time++)
//		{
//			for (int tag=0; tag<m_numTag; tag++)
//			{
//				flag = true;
//				if (tag == START || tag == END)
//					continue;
//				for (int preTag=0; preTag<m_numTag; preTag++)
//				{
//					if (preTag == START || preTag == END)
//						continue;
//					logAlpha[tag][time] = Utils.logSum(logAlpha[tag][time], logAlpha[preTag][time-1]+logM[time][preTag][tag], flag);
//					System.out.println("logAlpha["+preTag+"]["+(time-1)+"] = " + logAlpha[preTag][time-1] +" logM["+time+"]["+preTag+"]["+tag+"] = " + logM[time][preTag][tag]);
//					flag = false;
//				}
//				System.out.println("logAlpha = " + logAlpha[tag][time]);
//			}
//		}
//		//debug only
//			double w = 0.0;
//			flag = true;
//			for (int p=0; p<m_numTag; p++)
//			{
//				if (p == START || p == END)
//					continue;
//				w = Utils.logSum(w, logAlpha[p][times-1]+logM[times][p][END], flag);
//				flag = false;
//			}
//			System.out.println("final logAlpha = " + w);
//	}
//	
//	/**
//	 * 后向算法.
//	 * 矩阵中保存的不是后向算法得出的数值本身，而是log值
//	 *
//	 * @param logBeta the log beta
//	 * @param times the times
//	 * @param logM the log m
//	 */
//	protected void backword(double[][] logBeta, int times, double[][][] logM)
//	{
//		int time = times-1;
//		for (int y=0; y<m_numTag; y++)
//		{
//			if (y==START || y==END)
//				continue;
////			logBeta[y][time] = logM[time+1][y][END];
//			logBeta[y][time] = 0.0;
//		}
//		
//		boolean flag = true;
//		for (time=times-2; time>=0; time--)
//		{
//			for (int p=0; p<m_numTag; p++)
//			{
//				if (p == START || p == END)
//					continue;
//				flag = true;
//				for (int t=0; t<m_numTag; t++)
//				{
//					if (t == START || t == END)
//						continue;
//					logBeta[p][time] = Utils.logSum(logBeta[p][time], logM[time+1][p][t] + logBeta[t][time+1], flag);
//					flag = false;
//					System.out.println("logBeta["+t+"]["+(time+1)+"]" + logBeta[t][time+1] + "logM["+(time+1)+"]["+p+"]["+t+"] = " + logM[(time+1)][p][t]);
//				}
//				System.out.println("logBeta " + logBeta[p][time]);
//			}
//		}
//		
//		//debug only
//			double w = 0.0;
//			flag = true;
//			for (int t=0; t<m_numTag; t++)
//			{
//				if (t == START || t == END)
//					continue;
//				w = Utils.logSum(w, logM[0][START][t] + logBeta[t][0], flag);
//				flag = false;
//			}
//			System.out.println("final logBeta " + w);
//	}
//
//	/**
//	 * 计算似然值
//	 *
//	 * @param logZx the log zx
//	 * @param x the x
//	 * @return the double
//	 */
//	protected double calcloglikelihood(double[] logZx, double[] x)
//	{
//		double f = 0.0;
//		for (int i=0; i<m_numEvents; i++)
//		{
//			CRFEvent e = m_events.get(i);
//			int len = e.inputs.length;
//			for (int idx=0; idx<=len; idx++)
//			{
//				List<Integer> feats = null;
//				if (idx == 0)
//					feats = m_featHandler.getFeatures(e, START,  idx );
//				else
//					feats = m_featHandler.getFeatures(e, e.labels[idx-1], idx );
//					
//				for (int feature : feats)
//					if (idx!=len)
//						f += x[feature*m_numTag + e.labels[idx]];
//					else
//						f += x[feature*m_numTag + END];
//			}
//			
//			f -= logZx[i];
//		}
//		return f;
//	}
//	
	
	
}
