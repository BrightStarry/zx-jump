package com.zx.jump.config;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * author:ZhengXing
 * datetime:2018-01-21 17:15
 * 配置类
 */
@Component
@ConfigurationProperties(prefix = "custom")
@Data
@Validated
public class ProxyConfig {
	//静态属性
	//请求头中的host属性的主机名和端口分隔符
	public static final String HOST_SEPARATOR = ":";

	/**
	 * socket 连接相关参数
	 */
	@Valid
	private SocketConfig socket = new SocketConfig();

	@Data
	public static class SocketConfig{
		/**
		 * http消息聚合处理器能处理的最大长度
		 */

		@Max(value = Integer.MAX_VALUE,message = "maxContentLength长度需要小于Integer.MAX_VALUE")
		@Min(value = 6553600,message = "maxContentLength长度需要大于6553600")
		private Integer maxContentLength = Integer.MAX_VALUE;

		/**
		 * 连接到目标主机超时时间
		 */
		@Range(max = 10000, min = 1000, message = "连接到目标主机超时时间范围有误(1000-10000)")
		private Integer connectTimeoutMillis = 3000;

		/**
		 * 代理服务器监听的端口
		 */
		@Range(min = 1024,max = 49151,message = "服务器监听端口范围有误(1024-49151)")
		private Integer proxyPort = 9000;


	}
}
