package jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Test;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import java.util.Date;

public class JwtEncodeTest {

    /****
     * 创建Jwt令牌
     */
    @Test
    public void testCreateJwt(){
        // 创建JWT令牌的对象
        JwtBuilder builder = Jwts.builder()
                .setId("888")   //设置唯一的编号
                .setSubject("wxx")   // 设置主题，Json数据
                .setIssuedAt(new Date())    //设置签发日期
                .setExpiration(new Date(System.currentTimeMillis()+20000))
                .signWith(SignatureAlgorithm.HS256,"wxxx"); // 设置签名，使用HS256算法，并设置密钥字符串
        // 构建，并返回一个字符串
        System.out.println(builder.compact());
    }

    @Test
    public void testParseJwt(){
        String compactJwt = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4ODgiLCJzdWIiOiJ3eHgiLCJpYXQiOjE2MTQxNDQ2NzgsImV4cCI6MTYxNDE0NDY5OH0.6F7zmzCoga9T2-SBDFvuDJxZjbO6qTEdtLZ1J9yZMq4";
        Claims claims = Jwts.parser()
                .setSigningKey("wxxx")       //设置密钥
                .parseClaimsJws(compactJwt) // 解析jwt
                .getBody();
        System.out.println(claims);
    }
}
