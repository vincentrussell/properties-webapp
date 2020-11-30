package com.github.vincentrusell.web;

import com.github.vincentrusell.web.conditional.ConditionalOnSystemProperty;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;


@Configuration
public class ServerConfiguration {

    @Bean
    @ConditionalOnSystemProperty(name = "javax.net.ssl.keyStore")
    public ConfigurableServletWebServerFactory webServerFactory() {
        final int httpPort = Integer.parseInt(firstNonNull(System.getProperty("server.port"), "80"));
        final int httpsPort = Integer.parseInt(firstNonNull(System.getProperty("server.https.port"), "443"));
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addAdditionalTomcatConnectors(createSslConnector(httpsPort));
        factory.addConnectorCustomizers(connector -> {
            connector.setPort(httpPort);
            connector.setRedirectPort(httpsPort);
        });
        return factory;
    }

    private Connector createSslConnector(int httpsPort) {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        notNull(System.getProperty("javax.net.ssl.keyStore"), "javax.net.ssl.keyStore property is not set");
        notNull(System.getProperty("javax.net.ssl.keyStorePassword"), "javax.net.ssl.keyStorePassword property is not set");
        final String keystoreType = firstNonNull(System.getProperty("javax.net.ssl.keyStoreType"), "JKS");
        final String clientAuth = firstNonNull(System.getProperty("javax.net.ssl.clientAuth"), "false");
        final String sslProtocol = firstNonNull(System.getProperty("javax.net.ssl.sslProtocol"), "TLSv1.2");

        notNull(System.getProperty("javax.net.ssl.trustStore"), "javax.net.ssl.trustStore property is not set");
        notNull(System.getProperty("javax.net.ssl.trustStorePassword"), "javax.net.ssl.trustStorePassword property is not set");
        final String truststoreType = firstNonNull(System.getProperty("javax.net.ssl.trustStoreType"), "JKS");

        File keystore = new File(System.getProperty("javax.net.ssl.keyStore"));
        File truststore = new File(System.getProperty("javax.net.ssl.trustStore"));

        isTrue(keystore.exists(), "keystore %s doesn't exist", keystore.getAbsolutePath());
        isTrue(truststore.exists(), "truststore %s doesn't exist", keystore.getAbsolutePath());

        connector.setScheme("https");
        connector.setSecure(true);
        connector.setPort(httpsPort);
        protocol.setSSLEnabled(true);
        protocol.setKeystoreFile(keystore.getAbsolutePath());
        protocol.setKeystorePass(System.getProperty("javax.net.ssl.keyStorePassword"));
        protocol.setKeystoreType(keystoreType);
        protocol.setTruststoreFile(truststore.getAbsolutePath());
        protocol.setTruststorePass(System.getProperty("javax.net.ssl.trustStorePassword"));
        protocol.setTruststoreType(truststoreType);
        protocol.setClientAuth(clientAuth);
        protocol.setSSLProtocol(sslProtocol);

        if (System.getProperty("javax.net.ssl.keyAlias") != null) {
            protocol.setKeyAlias(System.getProperty("javax.net.ssl.keyAlias"));
        }

        return connector;

    }

}
