package com.zx.jump.initializer;

import com.zx.jump.handler.HttpsConnectHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * author:ZhengXing
 * datetime:2018/1/22 0022 11:28
 * 用于客户端https请求的 通道初始化器
 */
public class HttpsConnectChannelInitializer extends ChannelInitializer<SocketChannel> {

	/**
	 * 与客户端连接的处理器(ProxyServerHandler)中的ctx,
	 * 用于将目标主机响应的消息 发送回 客户端
	 *
	 * 此处将其传给http连接对应的处理器类
	 */
	private final ChannelHandlerContext ctx;



	public HttpsConnectChannelInitializer(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		socketChannel.pipeline()
				//https请求无法解析,不做任何编解码操作
				//自定义处理器
				.addLast(new HttpsConnectHandler(ctx));
	}

}
