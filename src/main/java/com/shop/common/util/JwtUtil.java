package com.shop.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:shopDemoSecret123456}")
    private String secret;

    @Value("${jwt.expire:86400000}")
    private Long expire;

    /**
     * 生成JWT令牌
     */
    public String generateToken(Long userId,String username){
        Map<String,Object> claims=new HashMap<>();
        claims.put("userId",userId);
        claims.put("username",username);

        Date now=new Date();
        Date expiration=new Date(now.getTime()+expire);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256,secret)
                .compact();
    }

    /**
     * 从JWT中解析用户ID
     */
    public Long getUserIdFromToken(String token){
        try{
            Claims claims=Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("userId",Long.class);
        }catch (Exception e){
            log.error("JWT解析失败：{}",e.getMessage());
            return null;
        }
    }

    /**
     * 验证JWT是否有效
     */
    public boolean validateToken(String token){
        try{
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
