package com.zx.sms.codec;

import java.util.List;

import com.zx.sms.LongSMSMessage;
import com.zx.sms.codec.cmpp.wap.LongMessageFrame;

public interface LongMessageFrameCache {

	/**
	 * 将新收到的分片保存，并获取全部的分片。因为多个分片可能同时从不同连接到达，因此这个方法要线程安全。
	 * @param msg
	 * 	当前收到的分片的消息对象
	 * @param  key 
	 * 短信唯一性Key,相同的key表示同一个长短信
	 * @param  currFrame 
	 * 当前收到的分片
	 * @return
	 * 多线程同时执行时，只能有一个线程返回true ,表示分片接收完成
	 */
	boolean addAndGet(LongSMSMessage msg,String key,LongMessageFrame currFrame);
	
	/**
	 * 全部分片接收完成后，返回全部分片，并删除缓存中的分片。这个方法不用线程安全，。因为只有一个线程会执行一次。
 	 * @param  key 
	 * 短信唯一性Key,相同的key表示同一个长短信
	 */
	List<LongMessageFrame> getAndDel(String key);
}
