package com.github.vincentrusell.web;

import com.github.vincentrusell.web.conditional.ConditionalOnSystemProperty;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.apache.catalina.Host;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;
import java.util.Set;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@SpringBootApplication
@EnableSwagger2
@EnableWebMvc
@EnableWebSecurity
@Import(ServerConfiguration.class)
public class PropertiesWebappApp extends WebSecurityConfigurerAdapter implements WebMvcConfigurer {

    @Autowired
    UserDetailsService userDetailsService;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
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



    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        return userDetailsService;
    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }


    @Override
    public void configure(HttpSecurity http) throws Exception {
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
        //remove surrounding quotes from properties...this will cause errors downstream
        System.getProperties().entrySet().forEach(entry ->
                System.setProperty(entry.getKey().toString(), entry.getValue().toString()
                        .replaceAll("^\"|\"$", "")));

        final String httpPort = firstNonNull(System.getProperty("server.port"), "80");
        SpringApplication app = new SpringApplication(PropertiesWebappApp.class);
        app.setDefaultProperties(Collections
                .singletonMap("server.port", httpPort));
        app.run(args);
    }

}