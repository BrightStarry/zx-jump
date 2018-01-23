package com.zx.jump.main;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * author:ZhengXing
 * datetime:2018-01-21 13:35
 * 启动器
 */
@Component
@Slf4j
public class MainRunner implements CommandLineRunner{
    private final ProxyServer proxyServer;
    public MainRunner(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void run(String... strings) throws Exception {
        log.info("[启动器]启动器启动中...");
        proxyServer.start();

        log.info("[启动器]启动器启动完成");
    }
}
