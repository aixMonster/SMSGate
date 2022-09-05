package com.zx.sms.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.codec.LongMessageFrameCache;
import com.zx.sms.codec.LongMessageFrameProvider;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisLongMessageFrameProvider implements LongMessageFrameProvider {
	private static final Logger logger = LoggerFactory.getLogger(RedisLongMessageFrameProvider.class);
	@Override
	public LongMessageFrameCache create() {
		   JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
           jedisPoolConfig.setMaxTotal(20);
           jedisPoolConfig.setMaxIdle(10);
           JedisPool pool =  new JedisPool(jedisPoolConfig, "127.0.0.1", 6379);
		return new RedisLongMessageFrameCache(pool);
	}

	@Override
	public int order() {
		return 1;
	}

}
