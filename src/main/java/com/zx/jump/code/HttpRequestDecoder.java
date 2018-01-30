package com.zx.jump.code;

import com.zx.jump.util.ProxyUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
//		byte[] bytes = new byte[in.readableBytes()];
//		in.readBytes(bytes);
//		String str = new String(bytes);
//		log.info(LOG_PRE + ",消息:\r\n{}", ProxyUtil.getChannelId(ctx),str);
//
//		//报文分割 ,不忽略空行
//		String[] arr = StringUtils.splitByWholeSeparatorPreserveAllTokens(str, "\r\n");
//		//请求头分割
//		String[] headerArr = StringUtils.split(arr[0], " ");
//		//获取方法枚举
//		MethodEnum methodEnum = MethodEnum.get(headerArr[0]);
//		//如果是https后续请求,直接过滤
//		if (methodEnum.equals(MethodEnum.UNKNOWN)) {
//			out.add(in);
//			return;
//		}
//		//获取uri
//		String uri = headerArr[1];
//		//请求头key/value解析
//		Map<String, String> header = new HashMap<>();
//		for (int i = 1; i < arr.length; i++) {
//			if(StringUtils.isBlank(arr[i]))
//				break;
//			String[] keyValuePair = StringUtils.splitByWholeSeparatorPreserveAllTokens(arr[i], ": ");
//			header.put(keyValuePair[0],keyValuePair[1]);
//		}
//		//目标服务器 主机名/端口
//		String host = header.get("Host");
//		Integer port = StringUtils.split(host,":").length == 2 ? Integer.parseInt(StringUtils.split(host,":")[1]) : 80;
//
//
//		//解析请求主体
//		String[] arr2 = StringUtils.splitByWholeSeparatorPreserveAllTokens(str, "\r\n\r\n");
//		String body = arr2.length == 2 ? arr2[1] : "";
//
//		CustomHttpRequest customHttpRequest = new CustomHttpRequest(uri, header, new InetSocketAddress(host,port), methodEnum,body,in);
//
//		out.add(customHttpRequest);

	}
}
