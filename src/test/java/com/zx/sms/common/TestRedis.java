package com.zx.sms.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class TestRedis {
	private static final Logger logger = LoggerFactory.getLogger(TestRedis.class);

	@Test
	public void test() {
		   JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
           jedisPoolConfig.setMaxTotal(20);
           jedisPoolConfig.setMaxIdle(10);
           JedisPool pool =  new JedisPool(jedisPoolConfig, "127.0.0.1", 6379);
		Jedis jedis = pool.getResource();
		String key = "ABC"+RandomUtils.nextInt();
		jedis.set(key, "9");
		jedis.incr(key);
		logger.info("get- key: {}:{}",key,jedis.get(key));
		jedis.del(key);
		bitsetAndCount(jedis,key,98);
		long ll = bitsetAndCount(jedis,key,97);
		 System.out.println(ll);
	}
	
	private static String Lua_ge_64 = "redis.call('setbit',KEYS[1],ARGV[1],1) \n redis.call('expire',KEYS[1],ARGV[2]) \n return redis.call('bitcount',KEYS[1])";
		
	private long bitsetAndCount(Jedis jedis,String bitsetKey,int pkNumber) {
		List<String> params = new ArrayList<String>();
		params.add(String.valueOf(pkNumber));
		params.add(String.valueOf(7200));
		Long b_count = (Long)jedis.eval(Lua_ge_64,Collections.singletonList(bitsetKey),params);
		return b_count.longValue();
	}
}
