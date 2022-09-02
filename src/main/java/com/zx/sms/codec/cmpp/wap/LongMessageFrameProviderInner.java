package com.zx.sms.codec.cmpp.wap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.zx.sms.LongSMSMessage;
import com.zx.sms.codec.LongMessageFrameCache;
import com.zx.sms.codec.LongMessageFrameProvider;

public class LongMessageFrameProviderInner implements LongMessageFrameProvider {
	private static final Logger logger = LoggerFactory.getLogger(LongMessageFrameProviderInner.class);
	private static final RemovalListener<String, ImmutablePair<BitSet,List<LongMessageFrame>>> removealListener = new RemovalListener<String, ImmutablePair<BitSet,List<LongMessageFrame>>>() {

		@Override
		public void onRemoval(RemovalNotification<String, ImmutablePair<BitSet,List<LongMessageFrame>>> notification) {
			RemovalCause cause = notification.getCause();
			ImmutablePair<BitSet,List<LongMessageFrame>> h = notification.getValue();
			switch (cause) {
			case EXPIRED:
			case SIZE:
			case COLLECTED:
				logger.error("Long Message Lost cause by {}. {}", cause,longMessageFrameListToString(h.right));
			default:
				return;
			}
		}
	};
	
	/**
	 * 以服务号码+帧唯一码为key
	 * 注意：这里使用的jvm缓存保证长短信的分片。如果是集群部署，从网关过来的长短信会随机发送到不同的主机，需要使用集群缓存
	 * ，如redis,memcached来保存长短信分片。 由于可能有短信分片丢失，造成一直不能组装完成，为防止内存泄漏，这里要使用支持过期失效的缓存。
	 */
	private static Cache<String, ImmutablePair<BitSet,List<LongMessageFrame>>> cache = CacheBuilder.newBuilder().expireAfterAccess(2, TimeUnit.HOURS).removalListener(removealListener).build();
	private static ConcurrentMap<String, ImmutablePair<BitSet,List<LongMessageFrame>>> map = cache.asMap();
	@Override
	public LongMessageFrameCache create() {
		return new LongMessageFrameCacheInner();
	}

	@Override
	public int order() {
		//内部实现，序号为0
		return 0;
	}

	private static String longMessageFrameListToString(List<LongMessageFrame> list) {
		StringBuffer sb = new StringBuffer();
		for(LongMessageFrame m : list) {
			sb.append(m.toString()).append("\n");
		}
		return sb.toString();
	}
	
	private class LongMessageFrameCacheInner implements LongMessageFrameCache{

		@Override
		public boolean addAndGet(LongSMSMessage msg, String key, LongMessageFrame currFrame) {
			//pkTotal ,pkNumber 是byte，可能为负数
			int pkTotal = (int) (currFrame.getPktotal() & 0x0ff);
			int pkNumber =  (int)(currFrame.getPknumber()& 0x0ff);
			
			ImmutablePair<BitSet,List<LongMessageFrame>> oldFrameList = map.get(key);
			if(oldFrameList!=null) {
				//map里已经有其它分片了
				
				//List.add操作也要线程安全，避免多线程操作同一个ArrayList,因此上面使用synchronizedList
				oldFrameList.right.add(currFrame);
				//这里做原子的设置并判断是否接收全部分片
				synchronized (oldFrameList.left) {
					oldFrameList.left.set(pkNumber);
					//返回是否合并成功
					return  pkTotal == oldFrameList.left.cardinality();
				}
			}else {
				List<LongMessageFrame> currFrameList = new ArrayList<LongMessageFrame>() ;
				currFrameList.add(currFrame);
				BitSet currBitSet = new BitSet(pkTotal);
				currBitSet.set(pkNumber);
				
				//putIfAbsent是线程安全的
				 oldFrameList = map.putIfAbsent(key,ImmutablePair.of(currBitSet, Collections.synchronizedList(currFrameList)));
	
				if(oldFrameList != null) {
					//map里已经有其它分片了
					
					//List.add操作也要线程安全，避免多线程操作同一个ArrayList,因此上面使用synchronizedList
					oldFrameList.right.add(currFrame);
					//这里做原子的设置并判断是否接收全部分片
					synchronized (oldFrameList.left) {
						oldFrameList.left.set(pkNumber);
						//返回是否合并成功
						return  pkTotal == oldFrameList.left.cardinality();
					}
				}else {
					return false;
				}
			}
		}

		@Override
		public List<LongMessageFrame> getAndDel(String key) {
			
			ImmutablePair<BitSet,List<LongMessageFrame>> pair = map.remove(key);
			return pair.right;
		}
	}
}
