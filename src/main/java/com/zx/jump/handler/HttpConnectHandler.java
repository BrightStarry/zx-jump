package com.zx.jump.handler;

import com.zx.jump.util.ProxyUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * author:ZhengXing
 * datetime:2018/1/22 0022 11:22
 * 用于将客户端发送的http请求和目标主机建立连接后,
 * 处理目标主机的输入事件的处理器
 *
 * 每建立一个连接,都需要创建一个该对象
 */
@Slf4j
public class HttpConnectHandler extends ChannelInboundHandlerAdapter {

	private static final String LOG_PRE = "[Http连接处理类]通道id:{}";

	/**
	 * 与客户端连接的处理器(ProxyServerHandler)中的ctx,
	 * 用于将目标主机响应的消息 发送回 客户端
	 */
	private final ChannelHandlerContext ctx;
	public HttpConnectHandler(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}



	/**
	 * 当目标服务器取消注册
	 */
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx0) throws Exception {
		log.info(LOG_PRE + ",在目标服务器取消注册",ProxyUtil.getChannelId(ctx));
		//刷出所有内容
		ctx.flush();
		//关闭与客户端的通道
		ctx.close();
	}


	@Override
	public void channelRead(ChannelHandlerContext ctx0, Object msg) throws Exception {
			//目标主机的响应数据
			FullHttpResponse response = (FullHttpResponse) msg;
			//发回给客户端
			if(ctx.channel().isWritable())
				ctx.writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
	}


	/**
	 * 异常处理
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx0, Throwable cause) throws Exception {
		log.error(LOG_PRE + ",发生异常:{}", ProxyUtil.getChannelId(ctx),cause.getMessage(),cause);
		//关闭 与目标服务器的连接
		ctx0.close();
		//关闭 与客户端的连接
		ctx.close();
	}

}
