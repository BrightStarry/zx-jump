package com.zx.jump.code;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * author:ZhengXing
 * datetime:2018/1/26 0026 09:47
 * 自定义请求消息解码器
 */
@Component
@Slf4j
public class HttpRequestDecoder extends ByteToMessageDecoder {
	private static final String LOG_PRE = "";

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		byte[] buf = new byte[in.readableBytes()];
		in.readBytes(buf);
		String str = new String(buf);
		log.info();

	}
}
