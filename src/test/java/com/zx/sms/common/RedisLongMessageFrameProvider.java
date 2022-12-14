package com.zx.sms.common;

import org.apache.commons.lang3.StringUtils;
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
		String host = StringUtils.isBlank(System.getenv("REDIS_HOST")) ? System.getProperty("RedisHost"):System.getenv("REDIS_HOST");
		String port = StringUtils.isBlank(System.getenv("REDIS_PORT")) ? System.getProperty("RedisPort"):System.getenv("REDIS_PORT");
		JedisPool pool = new JedisPool(jedisPoolConfig, StringUtils.isBlank(host)?"127.0.0.1":host, Integer.parseInt(StringUtils.isBlank(port)?"6379":port));
		return new RedisLongMessageFrameCache(pool, "Test_");
	}

	@Override
	public int order() {
		return 1;
	}

}
