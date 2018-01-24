package com.zx.jump.main;

import com.zx.jump.config.ProxyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018-01-21 13:35
 * 启动器
 */
@Component
@Slf4j
public class MainRunner implements CommandLineRunner,InitializingBean{
    private final ProxyServer proxyServer;
    private final ProxyConfig proxyConfig;
    public MainRunner(ProxyServer proxyServer,ProxyConfig proxyConfig) {
        this.proxyServer = proxyServer;
        this.proxyConfig = proxyConfig;
    }

    /**
     * 服务启动时运行
     * @param strings
     * @throws Exception
     */
    @Override
    public void run(String... strings) throws Exception {
        log.info("[启动器]启动器启动中...");
        proxyServer.start();

        log.info("[启动器]启动器启动完成");
    }

    //所有bean加载完毕后运行
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("当前配置:{}",proxyConfig);
    }
}
