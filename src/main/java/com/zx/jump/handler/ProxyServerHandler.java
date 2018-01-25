package com.zx.jump.handler;

import com.zx.jump.factory.BootstrapFactory;
import com.zx.jump.initializer.HttpConnectChannelInitializer;
import com.zx.jump.config.ProxyConfig;
import com.zx.jump.initializer.HttpsConnectChannelInitializer;
import com.zx.jump.listener.HttpChannelFutureListener;
import com.zx.jump.listener.HttpsChannelFutureListener;
import com.zx.jump.main.ProxyServer;
import com.zx.jump.util.ChannelCacheUtil;
import com.zx.jump.util.ProxyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * author:ZhengXing
 * datetime:2018-01-21 14:05
 * 代理服务器 输入事件处理类
 * <p>
 * 可共享,线程安全
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ProxyServerHandler extends ChannelInboundHandlerAdapter {
    private static final String LOG_PRE = "[代理服务器处理类]通道id:{}";

    //属性
    private final ProxyConfig proxyConfig;
    //bootstrap工厂
    private final BootstrapFactory bootstrapFactory;

    public ProxyServerHandler(ProxyConfig proxyConfig, BootstrapFactory bootstrapFactory) {
        this.proxyConfig = proxyConfig;
        this.bootstrapFactory = bootstrapFactory;
    }

    /**
     * 通道读取到消息 事件
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //通道id
        String channelId = ProxyUtil.getChannelId(ctx);

        //HTTP/HTTPS : 如果是 http报文格式的,此时已经被编码解码器转为了该类,
        if (msg instanceof FullHttpRequest) {
            final FullHttpRequest request = (FullHttpRequest) msg;
            //获取ip和端口
            InetSocketAddress address = ProxyUtil.getAddressByRequest(request);

            //HTTPS :
            if (HttpMethod.CONNECT.equals(request.method())) {
                log.info(LOG_PRE + ",https请求.目标:{}", channelId, request.uri());

                //给客户端响应成功信息 HTTP/1.1 200 Connection Established  .如果失败时关闭客户端通道 - 该方法是自己封装的
                //此处没有添加Connection Established,似乎也没问题
                if (!ProxyUtil.writeAndFlush(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK), true))
                    return;

                //此处将 该通道 的用于报文编码解码的处理器去除,因为后续发送的https报文都是加密过的,不符合一般报文格式,我们直接转发即可
                ctx.pipeline().remove(ProxyServer.NAME_HTTP_CODE_HANDLER);
                ctx.pipeline().remove(ProxyServer.NAME_HTTP_AGGREGATOR_HANDLER);

                //用通道id作为key,将 目标服务器地址存入, 此时 第二个参数(ChannelFuture)为null,因为 我们还未和目标服务器建立连接
                ChannelCacheUtil.put(channelId, new ChannelCache(address, null));

                //直接退出等待下一次双方连接即可.
                return;
            }
            //HTTP: 运行到这里表示是http请求
            log.info(LOG_PRE + ",http请求.目标:{}", channelId, request.uri());

            //连接到目标服务器.并将当前的通道上下文(ctx)/请求报文(msg) 传入
            connect(true, address, ctx, msg, proxyConfig);

            return;
        }

        //其他格式数据(建立https connect后的客户端再次发送的加密数据):

        //从缓存获取之前处理https请求时缓存的 目标服务器地址 和 与目标服务器的连接通道
        ChannelCache cache = ChannelCacheUtil.get(ProxyUtil.getChannelId(ctx));

        //如果缓存为空,应该是缓存已经过期,直接返回客户端请求超时,并关闭连接
        if (Objects.isNull(cache)) {
            log.info(LOG_PRE + ",缓存过期", channelId);
            ProxyUtil.writeAndFlush(ctx, new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.REQUEST_TIMEOUT), false);
//            ctx.close();
            return;
        }

        //HTTPS: 如果此时 与目标服务器建立的连接通道 为空,则表示这个Https协议,是客户端第二次传输数据过来,因为第一次我们只是返回客户端 200信息,并没有真的去连接目标服务器
        if (Objects.isNull(cache.getChannelFuture())) {
            log.info(LOG_PRE + ",https,正在与目标建立连接",channelId);
            //连接到目标服务器,获取到 连接通道,并将该通道更新到缓存中
            ChannelCacheUtil.put(channelId,
                    cache.setChannelFuture(
                            connect(false, cache.getAddress(), ctx, msg, proxyConfig)));
        } else {

            //此处,表示https协议的请求第x次访问(x > 2; 第一次我们响应200,第二次同目标主机建立连接, 此处直接发送消息即可)
            //如果此时通道是可写的,写入消息
            if (cache.getChannelFuture().channel().isWritable()) {
                log.info(LOG_PRE + ",https,正在向目标发送后续消息",channelId);
                cache.getChannelFuture().channel().writeAndFlush(msg);
            } else {
                log.info(LOG_PRE + ",https,与目标通道不可写,关闭与客户端连接",channelId);
                //返回 表示失败的 408状态码响应
                ProxyUtil.responseFailedToClient(ctx);
            }
        }
    }


    /**
     * 处理用户自定义事件
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //如果不是 空闲状态事件,直接返回
        if (!(evt instanceof IdleStateEvent))
            return;

        IdleStateEvent event = (IdleStateEvent) evt;
        //如果是 所有空闲超时事件
        if (event.state() == IdleState.ALL_IDLE) {
            log.debug(LOG_PRE + ",空闲超时,关闭.", ProxyUtil.getChannelId(ctx));
            ctx.close();
        }
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error(LOG_PRE + ",发生异常:{}", ProxyUtil.getChannelId(ctx), cause.getMessage(), cause);
//        //关闭
//        ctx.close();
    }

    /**
     * 和 目标主机 建立连接
     */
    private ChannelFuture connect(boolean isHttp, InetSocketAddress address,
                                  ChannelHandlerContext ctx, Object msg,
                                  ProxyConfig proxyConfig) {
        ChannelFuture channelFuture;
        //用工厂类构建出一个bootstrap,用来建立socket连接
        Bootstrap bootstrap = bootstrapFactory.build();
        //如果是http请求
        if (isHttp) {
            //与目标主机建立连接
            channelFuture = bootstrap
                    //设置上http连接的通道初始化器
                    .handler(new HttpConnectChannelInitializer(ctx, proxyConfig))
                    //连接
                    .connect(address);
            //添加监听器,当连接建立成功后.进行相应操作
            return channelFuture.addListener(new HttpChannelFutureListener(msg, ctx));
        }
        //如果是Https请求
        channelFuture = bootstrap
                .handler(new HttpsConnectChannelInitializer(ctx))
                .connect(address);
        return channelFuture.addListener(new HttpsChannelFutureListener(msg, ctx));
    }


    /**
     * 用于存储每个通道各自信息的缓存类
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public class ChannelCache {
        //目标服务器的地址
        private InetSocketAddress address;
        //当前请求与目标主机建立的连接通道
        private ChannelFuture channelFuture;
    }
}
