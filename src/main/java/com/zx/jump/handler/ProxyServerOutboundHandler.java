package com.zx.jump.handler;

import com.zx.jump.util.ProxyUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018/1/22 0022 17:33
 * 处理代理服务器的输出io事件.
 */
@Component
@ChannelHandler.Sharable
@Slf4j
public class ProxyServerOutboundHandler extends ChannelOutboundHandlerAdapter {
	private static final String LOG_PRE = "[代理服务器输出事件处理类]通道id:{}";


	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		super.close(ctx,promise);
		log.info(LOG_PRE + ",关闭通道", ProxyUtil.getChannelId(ctx));
	}


}
