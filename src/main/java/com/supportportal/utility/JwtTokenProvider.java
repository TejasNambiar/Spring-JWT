package com.supportportal.utility;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.supportportal.entity.UserPrinciple;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;
import static com.supportportal.utility.SecurityConstant.*;
import static org.apache.naming.ResourceRef.AUTH;

public class JwtTokenProvider {
    @Value(value = "${jwt.secret}")
    private String secret;

    public String generateJwtToken(UserPrinciple userPrinciple){
        String[] claims = getClaimsFromUser(userPrinciple);
        return JWT.create()
                .withIssuer(GET_LISTS_LLC)
                .withAudience(GET_LISTS_ADMINISTRATION)
                .withIssuedAt(new Date())
                .withSubject(userPrinciple.getUsername()) // has to be unique to identify individual user
                .withArrayClaim(AUTHORITIES, claims)
                .withExpiresAt(new Date(System.currentTimeMillis()+SecurityConstant.EXPIRATION_TIME))
                .sign((HMAC512(secret.getBytes())));
    }

    public List<GrantedAuthority> getAuthorities(String token){
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
