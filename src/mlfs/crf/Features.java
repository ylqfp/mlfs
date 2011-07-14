/*
 * Features.java 
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
 * Last Update:Jul 4, 2011
 * 
 */
package mlfs.crf;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import mlfs.crf.cache.FeatureCacher;
import mlfs.crf.model.CRFEvent;

/**
 * The Class Features.
 */
public class Features {

	/** The logger. */
	private static Logger logger = Logger.getLogger(Features.class.getName());
	
	/** 模板操作类. */
	private TemplateHandler m_template;
	
	/** 特征和数字的对应map. */
	private Map<String, Integer> m_featIdMap;
	
	/** 特征总数. */
	private int m_featCounter;
	
	/** tag和id对应的map. */
	private Map<String, Integer> m_tagMap;
	
	/** 连接字符. */
	public static char FEATURE_JOIN = '#';
	
	/**
	 * Instantiates a new features.
	 *
	 * @param templateHandler the template handler
	 * @param tagMap the tag map
	 * @throws IOException 
	 */
	public Features(TemplateHandler templateHandler, Map<String, Integer> tagMap, List<CRFEvent> events, String modelPath) throws IOException
	{
		m_template = templateHandler;
		m_featCounter = 0;
		m_featIdMap = new HashMap<String, Integer>();
		m_tagMap = tagMap;
		
		statisticFeat(events, modelPath);
		logger.info("Feature Number : " + m_featCounter*m_tagMap.size());
	}
	
	/**
	 * 统计训练语料中出现过的所有特征集合
	 *
	 * @param events the events
	 * @throws IOException 
	 */
	public void statisticFeat(List<CRFEvent> events, String modelPath) throws IOException
	{
		logger.info("statistic featues in training file...");
		PrintWriter writer = new PrintWriter(new File(modelPath));
			
		Set<String> dict = new HashSet<String>();
		int numTag = m_tagMap.size();
		
		FeatureCacher cacher = FeatureCacher.getInstance();
		for (CRFEvent event : events)
		{
			event.FEATURE_CACHE_POS = cacher.size();
			
			int len = event.inputs.length;
			List<Integer> feats = null;
			for (int i=0; i<len; i++)//unigram
			{
				feats = new ArrayList<Integer>();
				List<String> unigramPred = m_template.getUnigramPred(event, i);
				for (String predicate : unigramPred)
				{
					String unigramFeat = predicate;
					
					if (!m_featIdMap.containsKey(unigramFeat))
					{
						m_featIdMap.put(unigramFeat, m_featCounter);
						m_featCounter++;
					}
					feats.add(m_featIdMap.get(unigramFeat));
				}
				cacher.add(feats);
			}
			for (int i=1; i<len; i++)//bigram
			{
				List<String> bigramPred = m_template.getBigramPred(event, i);
				for (int preTag=0; preTag<numTag; preTag++)
				{
					feats = new ArrayList<Integer>();
					for (String predicate : bigramPred)
					{
						String bigramFeat = predicate + FEATURE_JOIN +preTag;
						
						if (!m_featIdMap.containsKey(bigramFeat))
						{
							m_featIdMap.put(bigramFeat, m_featCounter);
							m_featCounter++;
						}
						feats.add(m_featIdMap.get(bigramFeat));
					}
					cacher.add(feats);
				}
			}
			//持久化
			for (List<String> chars : event.charFeat)
			{
				if (!dict.contains(chars.get(0)))
				{
					dict.add(chars.get(0));
					StringBuilder sb = new StringBuilder();
					for (String s : chars)
						sb.append(s).append(' ');
					writer.println(sb.toString());
				}
			}
			event.charFeat = null;
		}
		writer.println();//隔开charFeat和fidmap
		for (Entry<String, Integer> fid : m_featIdMap.entrySet())
		{
			writer.println(fid.getKey() + " " + fid.getValue());
		}
			
		m_featIdMap = null;
		dict = null;
		writer.close();
	}
	
	/**
	 * 获取特征总数.
	 *
	 * @return the feat num
	 */
	public int getFeatNum()
	{
		return m_featCounter;
	}
	
	/**
	 * Gets the tag map.
	 *
	 * @return the tag map
	 */
	public Map<String, Integer> getTagMap()
	{
		return m_tagMap;
	}
	
//	/**
//	 * Gets the template file.
//	 *
//	 * @return the template file
//	 */
//	public String getTemplateFilePath()
//	{
//		return m_template.getPath();
//	}
	
//	/**
//	 * Gets the feat map.
//	 *
//	 * @return the feat map
//	 */
//	public Map<String, Integer> getFeatMap()
//	{
//		return m_featIdMap;
//	}

//	/**
//	 * Gets the unigram feat.
//	 *
//	 * @param event the event
//	 * @param idx the idx
//	 * @return the unigram feat
//	 */
//	public List<Integer> getUnigramFeat(CRFEvent event, int idx)
//	{
//		if (idx<0 || idx>=event.inputs.length)
//			throw new IllegalArgumentException("idx 必须在 0～" +(event.inputs.length-1) + "之间");
//		
//		List<Integer> feats = new ArrayList<Integer>();
//		List<String> unigramPred = m_template.getUnigramPred(event, idx);
//		
//		for (String predicate : unigramPred)
//		{
//			String unigramFeat = predicate;
//			
//			if (m_featIdMap.containsKey(unigramFeat))
//				feats.add(m_featIdMap.get(unigramFeat));
//		}	
//		return feats;
//	}
//	
//	/**
//	 * Gets the bigram feat.
//	 *
//	 * @param bigramPred the bigram pred
//	 * @param preTag the pre tag
//	 * @return the bigram feat
//	 */
//	public List<Integer> getBigramFeat( List<String> bigramPred, int preTag)
//	{
//		List<Integer> feats = new ArrayList<Integer>();
//		for (String predicate : bigramPred)
//		{
//			String bigramFeat = predicate + FEATURE_JOIN +preTag;
//			
//			if (m_featIdMap.containsKey(bigramFeat))
//				feats.add(m_featIdMap.get(bigramFeat));
//		}
//		return feats;
//	}
//	
//	public List<Integer>getBigramFeat(CRFEvent event, int preTag, int idx)
//	{
//		if (idx<=0||idx>=event.inputs.length)
//			throw new IllegalArgumentException("idx必须在1～"+(event.inputs.length-1)+"之间");
//		List<String> preds = m_template.getBigramPred(event, idx);
//		return getBigramFeat(preds, preTag);
//	}
}
