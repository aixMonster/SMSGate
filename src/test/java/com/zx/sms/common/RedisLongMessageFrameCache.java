package com.zx.sms.common;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.LongSMSMessage;
import com.zx.sms.codec.LongMessageFrameCache;
import com.zx.sms.codec.cmpp.wap.LongMessageFrame;
import com.zx.sms.common.util.FstObjectSerializeUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisLongMessageFrameCache implements LongMessageFrameCache {
	private static final Logger logger = LoggerFactory.getLogger(RedisLongMessageFrameCache.class);
	final static String BitSetPre = "_BitSetdfj_";
	final static Long ttl = 2 * 3600L;
	private JedisPool jedispool;
	private String scriptHash ;

	public RedisLongMessageFrameCache(JedisPool jedispool) {
		this.jedispool = jedispool;
		init();
	}

	private void init() {
		//初始化lua hash
		Jedis jedis = jedispool.getResource();
		try {
			scriptHash = jedis.scriptLoad(Lua_ge_64);
		}finally {
			jedis.close();
		}
	}
	@Override
	public boolean addAndGet(LongSMSMessage msg, String key, LongMessageFrame currFrame) {
		// pkTotal ,pkNumber 是byte，可能为负数
		int pkTotal = (int) (currFrame.getPktotal() & 0x0ff);
		int pkNumber = (int) (currFrame.getPknumber() & 0x0ff);
		String bitsetKey = BitSetPre + key;
		Jedis jedis = jedispool.getResource();
		try {
			// 将短信分片加入reids集合

			jedis.sadd(key.getBytes(StandardCharsets.UTF_8), FstObjectSerializeUtil.write(currFrame));
			jedis.expire(key.getBytes(StandardCharsets.UTF_8), ttl);
			// 使用原子方法设置bitset并判断是否接收全部分片
			/*
			 * 相当于下面代码加全局分布锁，使用Lua实现 jedis.setbit(bitsetKey, pkNumber, true); long bitCount
			 * = jedis.bitcount(bitsetKey); return pkTotal == bitCount;
			 */

			long b_count = bitsetAndCount(jedis, bitsetKey, pkNumber);
			return pkTotal == b_count;

		} catch (Exception e) {
			logger.warn("", e);
		} finally {
			jedis.close();
		}

		return false;
	}

	@Override
	public List<LongMessageFrame> getAndDel(String key) {
		Jedis jedis = jedispool.getResource();
		try {
			Set<byte[]> allFrame = jedis.smembers(key.getBytes(StandardCharsets.UTF_8));
			List<LongMessageFrame> frames = new ArrayList<LongMessageFrame>();
			for (byte[] arr : allFrame) {
				try {
					LongMessageFrame f = (LongMessageFrame) FstObjectSerializeUtil.read(arr);
					frames.add(f);
				} catch (Exception e) {
					logger.warn("", e);
				}
			}
			jedis.del(key.getBytes(StandardCharsets.UTF_8));

			String bitsetKey = BitSetPre + key;
			jedis.del(bitsetKey.getBytes(StandardCharsets.UTF_8));
			return frames;
		} finally {
			jedis.close();
		}
	}

	private static String Lua_ge_64 = "redis.call('setbit',KEYS[1],ARGV[1],1);redis.call('expire',KEYS[1],ARGV[2]);return redis.call('bitcount',KEYS[1])";

	private long bitsetAndCount(Jedis jedis, String bitsetKey, int pkNumber) {
		List<String> params = new ArrayList<String>();
		params.add(String.valueOf(pkNumber));
		params.add(String.valueOf(ttl));
		Long b_count = (Long) jedis.evalsha(scriptHash, Collections.singletonList(bitsetKey), params);
		return b_count.longValue();
	}
}
