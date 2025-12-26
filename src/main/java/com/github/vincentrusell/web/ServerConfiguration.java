package com.github.vincentrusell.web;

import com.github.vincentrusell.web.conditional.ConditionalOnSystemProperty;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.util.Set;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;


@Configuration
public class ServerConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }


    @Override
    public void addViewControllers(final ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui.html");
        registry.addRedirectViewController("/swagger-ui", "/swagger-ui.html");
    }


    @Bean(name="userDetailsService")
    @ConditionalOnSystemProperty(name = "https.authorized.dns")
    public UserDetailsService authorizedDnsUserDetailsService() {
        final Set<String> authorizedDns = Sets.newHashSet(Splitter.on(",,")
                .split(firstNonNull(System.getProperty("https.authorized.dns"), "")));
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String dn) {
                if (authorizedDns.contains(dn)) {
                    return new User(dn, "",
                            AuthorityUtils
                                    .commaSeparatedStringToAuthorityList("ROLE_USER"));
                }
                throw new UsernameNotFoundException("User not found!");
            }
        };
    }

    @Bean(name="userDetailsService")
    @ConditionalOnSystemProperty(name = "https.authorized.hostnames")
    public UserDetailsService authorizedHostnamesUserDetailsService() {
        final Set<String> authorizedHostnames = Sets.newHashSet(Splitter.on(",")
                .split(firstNonNull(System.getProperty("https.authorized.hostnames"), "")));
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String hostname) {
                if (authorizedHostnames.contains(hostname)) {
                    return new User(hostname, "",
                            AuthorityUtils
                                    .commaSeparatedStringToAuthorityList("ROLE_USER"));
                }
                throw new UsernameNotFoundException("User not found!");
            }
        };
    }

    @Bean(name="userDetailsService")
    @ConditionalOnSystemProperty(absentProperties = {"https.authorized.hostnames", "https.authorized.dns"})
    public UserDetailsService defaultUserDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
                return new User(s, "",
                        AuthorityUtils
                                .commaSeparatedStringToAuthorityList("ROLE_USER"));
            }
        };
    }

    @Bean
    PropertiesController propertiesController() {
        return new PropertiesController();
    }

    @Bean
    @ConditionalOnSystemProperty(name = "javax.net.ssl.keyStore")
    SecretsManager secretsManager() throws Exception {
        String keyStore = System.getProperty("javax.net.ssl.keyStore");
        String keystorePassword = System.getProperty("javax.net.ssl.keyStorePassword");
        String keyPassword = firstNonNull(System.getProperty("javax.net.ssl.keyPassword"),
                System.getProperty("javax.net.ssl.keyStorePassword"));
        String keyAlias = System.getProperty("javax.net.ssl.keyAlias");
        String keystoreType = firstNonNull(System.getProperty("javax.net.ssl.keyStoreType"), "JKS");

        notNull(keyStore, "javax.net.ssl.keyStore property is not set");
        notNull(keystorePassword, "javax.net.ssl.keyStorePassword property is not set");
        notNull(keyPassword, "javax.net.ssl.keyPassword property is not set");
        notNull(keyAlias, "javax.net.ssl.keyAlias property is not set");

        return new SecretsManager(keyStore, keystorePassword, keystoreType, keyPassword, keyAlias);
    }

    @Bean
    @ConditionalOnBean(SecretsManager.class)
    SecretsController secretsController(final SecretsManager secretsManager) {
        return new SecretsController(secretsManager);
    }



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
