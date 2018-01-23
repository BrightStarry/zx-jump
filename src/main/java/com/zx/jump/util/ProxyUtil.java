package com.zx.jump.util;

import com.zx.jump.config.ProxyConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.net.InetSocketAddress;

/**
 * author:ZhengXing
 * datetime:2018-01-21 17:12
 * 工具类
 */
public class ProxyUtil {
	/**
	 * 从channel中解析出客户端ip
	 */
	public static String getIpByChannel(Channel channel) {
		return ((InetSocketAddress)channel.remoteAddress()).getAddress().getHostAddress();
	}

	/**
	 * 从request中获取 客户端请求的目标服务器的 ip和port
	 */
	public static InetSocketAddress getAddressByRequest(FullHttpRequest request) {
		//获取请求的主机和端口
		String[] temp1 = request.headers().get("host").split(ProxyConfig.HOST_SEPARATOR);
		//有些host没有端口,则默认为80
		return  new InetSocketAddress(temp1[0], temp1.length == 1 ? 80 : Integer.parseInt(temp1[1]));
	}

	/**
	 * 从ctx中获取到当前通道的id
	 */
	public static String getChannelId(ChannelHandlerContext ctx) {
		return ctx.channel().id().asShortText();
	}

}
