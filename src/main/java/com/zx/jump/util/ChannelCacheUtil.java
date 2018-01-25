package com.zx.jump.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.zx.jump.handler.ProxyServerHandler;

import java.util.concurrent.TimeUnit;

/**
 * author:ZhengXing
 * datetime:2018/1/23 0023 10:26
 * 通道缓存工具类
 * <p>
 * see {@link com.zx.jump.handler.ProxyServerHandler.ChannelCache}
 */
public class ChannelCacheUtil {

	//创建缓存
	private static final LoadingCache<String, ProxyServerHandler.ChannelCache> cache = Caffeine.newBuilder()
			.initialCapacity(1024)
			//不限制缓存大小
//			.maximumSize(10240)
			//超过x秒未读写操作,自动删除
			.expireAfterAccess(15, TimeUnit.SECONDS)
			//传入缓存加载策略,key不存在时调用该方法返回一个value回去
			//此处直接返回空
			.build(key -> null);

	/**
	 * 获取数据
	 */
	public static ProxyServerHandler.ChannelCache get(String channelId) {
		return cache.getIfPresent(channelId);
	}

	/**
	 * 存入数据
	 */
	public static void put(String channelId, ProxyServerHandler.ChannelCache channelCache) {
		cache.put(channelId, channelCache);
	}

	/**
	 * 删除数据
	 */
	public static void remove(String channelId) {
		cache.invalidate(channelId);
	}

}
