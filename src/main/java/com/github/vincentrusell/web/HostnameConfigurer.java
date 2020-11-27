package com.github.vincentrusell.web;

import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class HostnameConfigurer<H extends HttpSecurityBuilder<H>> extends
        AbstractHttpConfigurer<HostnameConfigurer<H>, H> {
    private HostnameAuthenticationFilter hostnameAuthenticationFilter;
    private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> authenticationUserDetailsService;
    private AuthenticationDetailsSource<HttpServletRequest, PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails> authenticationDetailsSource;


    public HostnameConfigurer() {
    }



    public HostnameConfigurer<H> userDetailsService(UserDetailsService userDetailsService) {
        UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken> authenticationUserDetailsService = new UserDetailsByNameServiceWrapper<>();
        authenticationUserDetailsService.setUserDetailsService(userDetailsService);
        return authenticationUserDetailsService(authenticationUserDetailsService);
    }


    public HostnameConfigurer<H> authenticationUserDetailsService(
            AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> authenticationUserDetailsService) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
        return this;
    }


    // @formatter:off
    @Override
    public void init(H http) throws Exception {
        PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(getAuthenticationUserDetailsService(http));

        http
                .authenticationProvider(authenticationProvider)
                .setSharedObject(AuthenticationEntryPoint.class, new Http403ForbiddenEntryPoint());
    }
    // @formatter:on

    @Override
    public void configure(H http) throws Exception {
        HostnameAuthenticationFilter filter = getFilter(http
                .getSharedObject(AuthenticationManager.class));
        http.addFilter(filter);
    }

    private HostnameAuthenticationFilter getFilter(AuthenticationManager authenticationManager) {
        if (hostnameAuthenticationFilter == null) {
            hostnameAuthenticationFilter = new HostnameAuthenticationFilter();
            hostnameAuthenticationFilter.setAuthenticationManager(authenticationManager);
            if (authenticationDetailsSource != null) {
                hostnameAuthenticationFilter
                        .setAuthenticationDetailsSource(authenticationDetailsSource);
            }
            hostnameAuthenticationFilter = postProcess(hostnameAuthenticationFilter);
        }

        return hostnameAuthenticationFilter;
    }

    private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> getAuthenticationUserDetailsService(
            H http) {
        if (authenticationUserDetailsService == null) {
            userDetailsService(http.getSharedObject(UserDetailsService.class));
        }
        return authenticationUserDetailsService;
    }

    public static class HostnameAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

        @Override
        protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
            InetAddress inetAddress = getInetAddress(request);
            return inetAddress.getHostName();
        }

        @Override
        protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
            return getInetAddress(request);
        }

        private InetAddress getInetAddress(HttpServletRequest request) {
            String value = request.getRemoteHost();
            try {
                return InetAddress.getByName(value);
            } catch (UnknownHostException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }


    }

}
