package com.zx.jump.initializer;

import com.zx.jump.handler.HttpConnectHandler;
import com.zx.jump.config.ProxyConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018/1/22 0022 11:28
 * 用于客户端http请求的 通道初始化器
 */
@Component
@NoArgsConstructor
public class HttpConnectChannelInitializer extends ChannelInitializer<SocketChannel> {

	/**
	 * 与客户端连接的处理器(ProxyServerHandler)中的ctx,
	 * 用于将目标主机响应的消息 发送回 客户端
	 *
	 * 此处将其传给http连接对应的处理器类
	 */
	private  ChannelHandlerContext ctx;
	/**
	 * 属性
	 */
	private static ProxyConfig proxyConfig;


	public HttpConnectChannelInitializer(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		socketChannel.pipeline()
				//作为客户端时的请求编码解码
				.addLast(new HttpClientCodec())
				//数据聚合类,将http报文转为 FullHttpRequest和FullHttpResponse
				.addLast(new HttpObjectAggregator(proxyConfig.getSocket().getMaxContentLength()))
				//自定义处理器
				.addLast(new HttpConnectHandler(ctx));
	}

	@Autowired
	public void init(ProxyConfig proxyConfig) {
		HttpConnectChannelInitializer.proxyConfig = proxyConfig;
	}
}
