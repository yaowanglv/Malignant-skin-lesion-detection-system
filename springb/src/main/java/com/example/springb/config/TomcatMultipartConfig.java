package com.example.springb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * 批量图片上传会为每张图片创建一个 multipart part，需要放宽 Tomcat 默认 part 数量限制。
 */
@Configuration
public class TomcatMultipartConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    @Value("${app.multipart.max-part-count:1200}")
    private int maxPartCount;

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addConnectorCustomizers(connector -> connector.setMaxPartCount(maxPartCount));
    }
}
