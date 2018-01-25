package com.zx.jump.listener;

import com.zx.jump.util.ChannelCacheUtil;
import com.zx.jump.util.ProxyUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import lombok.extern.slf4j.Slf4j;

import java.net.UnknownHostException;

/**
 * author:ZhengXing
 * datetime:2018/1/22 0022 14:11
 * 用于https请求的 与目标主机建立连接后的监听器类
 */
@Slf4j
public class HttpsChannelFutureListener implements ChannelFutureListener {
	private static final String LOG_PRE = "[https连接建立监听器]通道id:{}";

	/**
	 * 客户端要发送给目标主机的消息
	 */
	private Object msg;

	/**
	 * 通道上下文,如果与目标主机建立连接失败,返回失败响应给客户端,并关闭连接
	 */
	private ChannelHandlerContext ctx;

	public HttpsChannelFutureListener(Object msg, ChannelHandlerContext ctx) {
		this.msg = msg;
		this.ctx = ctx;
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		String channelId = ProxyUtil.getChannelId(ctx);
		//连接成功操作
		if(future.isSuccess()){
			log.info(LOG_PRE + ",与目标主机建立连接成功.",channelId);
			//将客户端请求报文发送给服务端
			if(future.channel().isWritable()){
				future.channel().writeAndFlush(msg);
			}else{
				future.channel().close();
			}
			return;
		}
		log.info(LOG_PRE + ",与目标主机建立连接失败.",channelId);
		//给客户端响应连接超时信息
		ProxyUtil.responseFailedToClient(ctx);
		//清除缓存
		ChannelCacheUtil.remove(channelId);

		//日志记录
		Throwable cause = future.cause();
		if(cause instanceof ConnectTimeoutException)
			log.error(LOG_PRE + ",连接超时:{}",channelId , cause.getMessage());
		else if (cause instanceof UnknownHostException)
			log.error(LOG_PRE + ",未知主机:{}", channelId, cause.getMessage());
		else
			log.error(LOG_PRE + ",异常:{}", channelId,cause.getMessage(),cause);
		log.info(LOG_PRE + ",给客户端响应失败信息成功.",channelId);
		//关闭 与客户端的连接
		ctx.close();
	}
}
