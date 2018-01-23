package com.zx.jump.main;

import com.zx.jump.config.ProxyConfig;
import com.zx.jump.handler.ProxyServerHandler;
import com.zx.jump.handler.ProxyServerOutboundHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


/**
 * author:ZhengXing
 * datetime:2018-01-21 13:56
 * 代理服务器
 */
@Component
@Slf4j
public class ProxyServer {
    //静态参数-netty处理器的名字,用于在https请求时,剔除channel中绑定的编解码相关处理类,因为https请求无法解析其加密的数据
    public static final String NAME_HTTP_CODE_HANDLER = "httpCode";
    public static final String NAME_HTTP_AGGREGATOR_HANDLER = "httpAggregator";
    public static final String NAME_PROXY_SERVER_HANDLER = "proxyServerHandler";

    /**
     * 代理服务器处理类  主要逻辑都在这个类
     * 继承 {@link io.netty.channel.ChannelInboundHandlerAdapter}
     * 处理输入事件,例如 收到消息/通道激活/通道绑定等
     */
    private final ProxyServerHandler proxyServerHandler;
    /**
     * 继承{@link io.netty.channel.ChannelOutboundHandlerAdapter}
     * 处理输出事件,例如写入事件 , 处理该代理服务器向 客户端发送回去的 报文等
     */
    private final ProxyServerOutboundHandler proxyServerOutboundHandler;
    private final ProxyConfig proxyConfig;
    @Autowired
    public ProxyServer(ProxyServerHandler proxyServerHandler, ProxyServerOutboundHandler proxyServerOutboundHandler, ProxyConfig proxyConfig) {
        this.proxyServerHandler = proxyServerHandler;
        this.proxyServerOutboundHandler = proxyServerOutboundHandler;
        this.proxyConfig = proxyConfig;
    }

    /**
     * 启动Netty server,监听指定端口的TCP连接.
     * 此处监听客户端向我们发送的http报文
     */
    @SneakyThrows
    public void start() {

        //1 用于接收Client的连接 的线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(8);
        //2 用于实际业务操作的线程组
        EventLoopGroup workerGroup = new NioEventLoopGroup(30);
        //3 创建一个辅助类Bootstrap（引导程序）,对server进行配置
        ServerBootstrap serverBootStrap = new ServerBootstrap();
        //4 将两个线程组加入 bootstrap
        serverBootStrap.group(bossGroup, workerGroup)
                //指定使用这种类型的通道
                .channel(NioServerSocketChannel.class)
                //使用 childHandler 绑定具体的事件处理器
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        //设置字符串形式的解码  这样serverHandler中获取到的msg可以直接(String)msg转为string
                        socketChannel.pipeline()
                                //心跳检测：超过xs未触发触发读取事件，则触发userEventTriggered()事件
//                                .addLast("idleState handler",new IdleStateHandler(0,0,2, TimeUnit.SECONDS))

                                //组合了http请求解码器和http响应编码器的一个类,可自定义各种最大长度
                                .addLast(NAME_HTTP_CODE_HANDLER, new HttpServerCodec())
                                //消息聚合器,注意,需要添加在http编解码器(HttpServerCodec)之后
                                .addLast(NAME_HTTP_AGGREGATOR_HANDLER, new HttpObjectAggregator(65536))
                                //自定义 输入事件 处理器
                                .addLast(proxyServerOutboundHandler)
                                //自定义 客户端输入事件 处理器
                                .addLast(NAME_PROXY_SERVER_HANDLER, proxyServerHandler);
                    }
                })
                /**
                 * TCP连接的 参数
                 * tcp三次握手：
                 *  客户端发送有SYN标志的包（第一次）
                 *  服务器收到后，向客户端发送SYN ACK（第二次）
                 *  此时，TCP内核模块把客户端连接放入A队列，
                 *  然后服务器收到客户端再次发来的ACK时（第三次）
                 *  TCP内核把客户端连接放入B队列，连接完成,完成accept()方法
                 *  此时，TCP内核会被客户端连接从队列B中取出。完成。
                 *
                 *  A队列和B队列长度之和就是backlog，如果大于backlog，新连接会被拒绝
                 *  注意，backlog对程序支持的连接数并无影响，backlog只影响还未完成accept()方法的连接。
                 */
                //服务端接受连接的队列长度
                .option(ChannelOption.SO_BACKLOG, 2048)
                //保持连接,类似心跳检测,超过2小时空闲才激活
//                .childOption(ChannelOption.SO_KEEPALIVE, true)
                //接收缓冲区大小
                .option(ChannelOption.SO_RCVBUF, 128 * 1024);
        log.info("代理服务器启动,在{}端口",proxyConfig.getSocket().getProxyPort());
        //5 绑定端口,进行监听 异步的  可以开启多个端口监听
        ChannelFuture future = serverBootStrap.bind(proxyConfig.getSocket().getProxyPort()).sync();
        //6 关闭前阻塞
        future.channel().closeFuture().sync();
        //7 关闭线程组
        bossGroup.shutdownGracefully().sync();
        workerGroup.shutdownGracefully().sync();
    }
}
