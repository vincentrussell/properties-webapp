package com.github.vincentrusell.web;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.function.BiConsumer;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@SpringBootApplication()
@EnableSwagger2
@EnableWebMvc
@EnableWebSecurity
@Import(ServerConfiguration.class)
@ComponentScan(basePackages = { "none.dont.scan.anything"})
public class PropertiesWebappApp extends WebSecurityConfigurerAdapter {

    @Autowired(required = false)
    UserDetailsService userDetailsService;

    @Bean
    public Docket api() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2);

        String swaggerHost = System.getProperty("swagger.host");

        if (StringUtils.isNotEmpty(swaggerHost)) {
            docket.host(swaggerHost);
        }

        return docket
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }



    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();

        if (System.getProperty("https.authorized.dns") != null) {
            http.authorizeRequests().anyRequest().authenticated()
                    .and()
                    .x509()
                    .subjectPrincipalRegex("(.*)?+")
                    .userDetailsService(userDetailsService);
        } else if (System.getProperty("https.authorized.hostnames") != null) {
            http.authorizeRequests().anyRequest().authenticated()
                    .and()
                    .apply(new HostnameConfigurer<>())
                    .userDetailsService(userDetailsService);
        } else {
            http.authorizeRequests().anyRequest().permitAll();
        }
    }


    public static void main(String[] args) {
        System.getenv().forEach((s, s2) -> {
            System.out.println(
                    String.format("setting system property from environment variable key=%s,value=%s", s, s2));
            System.setProperty(s, s2);
        });

        //remove surrounding quotes from properties...this will cause errors downstream
        System.getProperties().entrySet().forEach(entry ->
                System.setProperty(entry.getKey().toString(), entry.getValue().toString()
                        .replaceAll("^\"|\"$", "")));

        final String httpPort = firstNonNull(System.getProperty("server.port"), "80");
        SpringApplication app = new SpringApplication(PropertiesWebappApp.class);
        app.setDefaultProperties(ImmutableMap.<String,Object>builder()
                .put("server.port", httpPort)
                .put("spring.mvc.view.prefix", "/WEB-INF/jsp")
                .put("spring.mvc.view.suffix", ".jsp").build());
        app.run(args);
    }

}