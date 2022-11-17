package com.supportportal.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.supportportal.entity.UserPrinciple;
import com.supportportal.utility.SecurityConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;
import static com.supportportal.utility.SecurityConstant.*;

@Component
public class JwtTokenProvider {

    @Value(value = "${jwt.secret}")
    private String secret;

    public String generateJwtToken(UserPrinciple userPrinciple) {
        String[] claims = getClaimsFromUser(userPrinciple);
        return JWT.create()
                .withIssuer(GET_LISTS_LLC)
                .withAudience(GET_LISTS_ADMINISTRATION)
                .withIssuedAt(new Date())
                .withSubject(userPrinciple.getUsername()) // has to be unique to identify individual user
                .withArrayClaim(AUTHORITIES, claims)
                .withExpiresAt(new Date(System.currentTimeMillis()+ SecurityConstant.EXPIRATION_TIME))
                .sign((HMAC512(secret.getBytes())));
    }

    public List<GrantedAuthority> getAuthorities(String token) {
        String[] claims = getClaimsFromToken(token);
        return Arrays.stream(claims).map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    // if token gets verified, then all permissions regarding user and retrieved
    private String[] getClaimsFromToken(String token) {
        JWTVerifier jwtVerifier = getJWTVerifier();
        // returns a list of all the Authorities granted to a User via Token
        return jwtVerifier.verify(token).getClaim(AUTHORITIES).asArray(String.class);
    }

    // tells security to process request assuming user has been authenticated
    public Authentication getAuthentication(String username, List<GrantedAuthority> authorities, HttpServletRequest request) {
        // assuming that credentials already verified, hence null passed
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, null,authorities);
        // setting user details i.e. setting security context
        authenticationToken.setDetails(new WebAuthenticationDetailsSource()
                .buildDetails(request));
        return authenticationToken;
    }

    public boolean isTokenValid(String username, String token) {
        JWTVerifier jwtVerifier = getJWTVerifier();
        return StringUtils.isNotEmpty(username) && !isTokenExpired(jwtVerifier,token);
    }

    public String getSubject(String token) {
        JWTVerifier jwtVerifier = getJWTVerifier();
        return  jwtVerifier.verify(token).getSubject();
    }

    private boolean isTokenExpired(JWTVerifier jwtVerifier, String token) {
        Date expiration = jwtVerifier.verify(token).getExpiresAt();
        return expiration.before(new Date());
    }

    private JWTVerifier getJWTVerifier() {
        JWTVerifier jwtVerifier;
        try{
            Algorithm algorithm = HMAC512(secret);
            jwtVerifier = JWT.require(algorithm)
                    .withIssuer(GET_LISTS_LLC)
                    .build();

        }catch (JWTVerificationException jwtException){
            // throwing this to avoid user seeing inner workings of app
            throw new JWTVerificationException(TOKEN_CANNOT_BE_VERIFIED);
        }
        return jwtVerifier;
    }

    private String[] getClaimsFromUser(UserPrinciple userPrinciple) {
        // returns a list of all the Authorities granted to a User via UserName
        return userPrinciple.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority).toArray(String[]::new);
    }
}
