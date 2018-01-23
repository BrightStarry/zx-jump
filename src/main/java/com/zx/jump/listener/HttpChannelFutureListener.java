package com.zx.jump.listener;

import com.zx.jump.util.ProxyUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.extern.slf4j.Slf4j;

import java.net.UnknownHostException;

/**
 * author:ZhengXing
 * datetime:2018/1/22 0022 14:11
 * 用于http请求的 与目标主机建立连接后的监听器类
 */
@Slf4j
public class HttpChannelFutureListener implements ChannelFutureListener {
	private static final String LOG_PRE = "[http连接建立监听器]通道id:{}";

	/**
	 * 客户端要发送给目标主机的消息
	 */
	private Object msg;

	/**
	 * 通道上下文,如果与目标主机建立连接失败,返回失败响应给客户端,并关闭连接
	 */
	private ChannelHandlerContext ctx;

	public HttpChannelFutureListener(Object msg, ChannelHandlerContext ctx) {
		this.msg = msg;
		this.ctx = ctx;
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		//连接成功操作
		if(future.isSuccess()){
			//将客户端请求报文发送给服务端
			future.channel().writeAndFlush(msg);
			return;
		}
		//连接失败操作,暂且返回408,请求超时
		ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_TIMEOUT));

		Throwable cause = future.cause();
		if(cause instanceof ConnectTimeoutException)
			log.error(LOG_PRE + ",连接超时:{}", ProxyUtil.getChannelId(ctx), cause.getMessage());
		else if (cause instanceof UnknownHostException)
			log.error(LOG_PRE + ",未知主机:{}", ProxyUtil.getChannelId(ctx), cause.getMessage());
		else
			log.error(LOG_PRE + ",建立连接异常:{}", ProxyUtil.getChannelId(ctx),cause.getMessage(),cause);

		//并关闭 与客户端的连接
		ctx.close();
	}
}
