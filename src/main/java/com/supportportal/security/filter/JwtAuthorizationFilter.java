package com.supportportal.security.filter;

import com.google.common.net.HttpHeaders;
import com.supportportal.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.supportportal.utility.constants.SecurityConstant.OPTIONS_HTTP_METHOD;
import static com.supportportal.utility.constants.SecurityConstant.TOKEN_PREFIX;

@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthorizationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(request.getMethod().equalsIgnoreCase(OPTIONS_HTTP_METHOD)){
            response.setStatus(HttpStatus.OK.value());
        }else{
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if(authorizationHeader == null || !authorizationHeader.startsWith(TOKEN_PREFIX)){
                filterChain.doFilter(request,response);
                return;
            }
            String token = authorizationHeader.substring(TOKEN_PREFIX.length());
            // if at this point the token is tampered with, then it will not work
            String username = jwtTokenProvider.getSubject(token);
            if(jwtTokenProvider.isTokenValid(username,token) && SecurityContextHolder.getContext().getAuthentication() == null){
                List<GrantedAuthority> authorities = jwtTokenProvider.getAuthorities(token);
                Authentication authentication = jwtTokenProvider.getAuthentication(username,authorities, request);
                // Setting security context provided token was valid and security context was not set
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            else{
                // if things haven't checked out, clear entire context
                SecurityContextHolder.clearContext();
            }
        }
        // Everything has checked out, let request continue its course
        filterChain.doFilter(request,response);
    }
}
