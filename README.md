#### ZX-Jump Http/Https代理服务器 用于穿越长城
* 之前想安装个chrome插件,结果蓝灯崩了,而github上许多的翻墙软件也被封了.还有的软件下载界面全是反共的宣言,  
不太信得过.于是去vultr买了服务器,装了shadowsocks,但用起来不顺手,就想着自己写一个代理服务器.

* ctx.writeAndFlush() 和 ctx.channel().writeAndFlush()的区别,前者会直接从当前handler发送消息,后者还会经过后续的handler.
* netty的每次创建出一个连接channel的时候都会调用在之前定义的通道初始化器初始化该channel.


#### bug


#### 思路
* 简单说下浏览器的处理流程
    >
        假设我们访问http://www.baidu.com,浏览器会自动帮我们创建出一个请求的字符串参数,大致如下
        GET /  HTTP/1.1     # 请求行: 请求方法 请求路径 请求协议版本
        Host: www.baidu.com
        User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36
        Connection: keep-alive
        content-length: 0
        
        通过host得知访问的域名是www.baidu.com,通过DNS解析出ip,向该IP的80端口发送这段符合Http协议的消息.
        此时,假设百度这台服务器上部署的是Tomcat容器,那么,Tomcat会自动解析该字符串,将其组装为HttpServletRequest.然后响应Response
        浏览器收到响应后,解析响应内容,然后进行后续操作(将html解析为页面,继续请求其中的css/js等,然后运行js等操作)
    >   
* 代理服务器的简单实现
    >
        1. 代理服务器:创建Socket服务端,监听消息.
        2. 本地配置Internet选项的Http代理,将自己电脑(浏览器)的所有请求转发到代理服务器.
        3. 代理服务器获取到本机发送的Http协议请求数据后,转发给其本来要发送的目标主机(也就请求行中的主机信息,例如百度)
        4. 目标主机响应后,将响应发送回自己的电脑(浏览器)即可.
    >
* 如上只适用于普通的Http请求,但如果是Https,大致流程如下(参考:https://zhuanlan.zhihu.com/p/28767664):
    ![](img/1.jpg)
    1. 浏览器发送Http Connect连接请求
    > CONNECT baidu.com:443 HTTP/1.1
    2. 代理服务器收到请求后,同样解析出 目标主机 和 端口(443),然后与目标主机建立TCP连接,并先响应给浏览器如下报文
    > HTTP/1.1 200 Connection Established
    3. 建立完连接后,浏览器继续发送后续的请求内容,我们需要将其转发给目标主机,然后目标主机也会发送回响应,我们同样将其发送回浏览器.
    4. 如上发送/响应可能会进行多次,并且内容都是经过加密的,我们是无法解析的.

 
    
* 总结
    >
        对于普通Http请求,我们要做的只是转发请求到目标主机,并且中间可以任意获取/篡改请求或响应内容.
        而对于Https请求,我们需要事先建立和目标主机的连接,然后告诉浏览器连接建立成功,然后让双方任意发送消息.
        如此,也可以得出,Http想比于Https,安全性实在太弱.
    >


#### 大致流程
* 使用SpringBoot搭建.
* 使用Netty作为服务端.然后设置好internet中的代理为127.0.0.1:port(注意,需要禁用其中的自动脚本,因为它的优先级比普通代理高)
* 于是,在读取事件监听中获取到访问网页的请求如下:
    * Http
        >
            GET http://csdnimg.cn/public/favicon.ico HTTP/1.1
            Host: csdnimg.cn
            Proxy-Connection: keep-alive
            Pragma: no-cache
            Cache-Control: no-cache
            User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36
            Accept: image/webp,image/apng,image/*,*/*;q=0.8
            Referer: http://blog.csdn.net/zuoxiaolong8810/article/details/65441709
            Accept-Encoding: gzip, deflate
            Accept-Language: zh-CN,zh;q=0.9
            content-length: 0
        
            第一行为请求行,包括了http的请求方式,请求的主机和请求的http协议版本.
            后面都是请求头,包括了Cookies等信息(如果有的话).最后的content-length: 0,是因为GET请求没有请求体.
        >
    * Https
        >
            CONNECT webim.tim.qq.com:443 HTTP/1.1
            Host: webim.tim.qq.com:443
            Proxy-Connection: keep-alive
            User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36
            
            对于HTTPS来说,第一次的请求都是CONNECT(连接请求).请求头的信息通常也就这么3个主要的.
        >
    * 代理和非代理报文区别
        >
            可以发现,在使用ip代理后,请求报文有如下变化:
                1. 浏览器会自动在请求行上添加要请求的完整路径.(例如, GET /public/favicon.ico HTTP/1.1 变为了 GET http://csdnimg.cn/public/favicon.ico HTTP/1.1)
                    这个设计是因为在早期Http设计中,没有http代理时,目标服务器收到请求后,假设请求行中的uri为/a/b.
                    那么目标服务器可以很清楚的知道它要访问的是自己的/a/b路径.
                    而使用代理后,并不知道 目标服务器的完整地址,所以需要携带目标服务器的完整路径.
                    后来,为了解决虚拟主机的问题,几乎所有的浏览器都会在请求头中携带host属性,也就解决了这个问题.
                2. 请求头中的Connection属性变为Proxy-Connection
                    (Http1.1中,默认keep-alive,除非显式指定Connection: close)
                    因为老旧代理(Http1.0)不认识Connection属性,会将其作为无关属性直接转发给目标服务器.
                    但目标服务器会根据其要求(Connection: keep-alive),保持长连接,而代理则不会保持这个连接.
                    客户端收到代理转发回去的响应后(浏览器也会根据其要求),保持长连接,但此时代理已经关闭了这个连接.
                    为了解决这个问题,就出现了Proxy-Connection,
                        如果代理是Http1.1,那么,可将其自动重写为Connection.再发送给目标服务器.
                        如果代理是1.0,那么,服务器会收到Proxy-Connection,就发现它是代理,因为它没有自动将其转为Connection.就会在响应中添加Connection:close即可.
        >
        
* 测试:接收请求后,返回自定义的响应报文
    * 伪代码(Http规定头必须有两个连续的\r\n,一旦读取到\r\n\r\n,就会将往后的部分识别为请求主体) 
    >
        //拼接出自定义响应报文
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 200 OK\r\n")
                .append("Content-Type: text/html; charset=UTF-8\r\n\r\n")
                .append("<html>" +
                        "<head></head>" +
                        "<body>" +
                        "<h1>测试响应</h1>" +
                        "</body>" +
                        "</html>");
        //将自定义响应报文发送回浏览器                
        sendResponse(sb.toString().getBytes("UTF-8"));
    >
    * 响应报文
    >
        HTTP/1.1 200 OK
        Content-Type: text/html; charset=UTF-8
        
        <html><head></head><body><h1>测试响应</h1></body></html>
        
        
        第一行为响应行,然后是响应头,然后是响应主体
    >
    * 此时,浏览器访问Http网页时,将会在页面上显示 测试响应


#### 开始编码
* Netty服务端启动类,监听9000端口
