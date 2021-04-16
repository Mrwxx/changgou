package com.changgou.oauth;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

public class createTokenTest {

    @Test
    public void createToken(){
        // 证书文件路径
        String key_loc = "changgou.jks";
        // 密钥库密码
        String key_password = "changgou";
        // 密钥密码
        String password = "changgou";
        // 密钥别名
        String alias = "changgou";
        // 证书文件源
        ClassPathResource classPathResource = new ClassPathResource(key_loc);
        // 创建密钥工厂
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(classPathResource, key_password.toCharArray());

        // 获取密钥对
        KeyPair keyPair = keyStoreKeyFactory.getKeyPair(alias, password.toCharArray());
        // 获取私钥
        RSAPrivateKey aPrivate = (RSAPrivateKey) keyPair.getPrivate();

        // 自定义载荷
        Map<String , Object> tokenMap = new HashMap<>();
        tokenMap.put("id", "1");
        tokenMap.put("name", "itheima");
        tokenMap.put("authority", new String[]{"admin", "oauth"});

        // 生成jwt令牌，使用私钥签名
        Jwt jwt = JwtHelper.encode(JSON.toJSONString(tokenMap), new RsaSigner(aPrivate));
        // 取出令牌
        String encoded = jwt.getEncoded();
        System.out.println(encoded);

    }

    /***
     * 使用公钥解析令牌
     */
    @Test
    public void parseToken(){
        // 令牌
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdXRob3JpdHkiOlsiYWRtaW4iLCJvYXV0aCJdLCJuYW1lIjoiaXRoZWltYSIsImlkIjoiMSJ9.S3eUDD5H64QRy6PfFUDdmF47_a956CJ7PPsaGj4xmNvBt5OyPvwBgF4gfRQTEy-_ckBVwbAe0oNQIzmOewkztOIS_HXzd8O7j-WUKCX_9un76_ToXIJZPXU1LluzVoPGTYBOqsWrJWUqVFDv_W4fABnDwSG6mdvRkFeNJUW9m5SPt8BfQZnFKJTt0KeP-isTwWo0y9oUGgiPe-ncAwXL7AVasetZKu6hQKm5sPxTwhz1-APQ8XgU-qjwRU-qwGXNVEm8W8Ac9epm1anl2Fw0MAaB15KNjUEaIXekLTNNXnjZ0OizBGDqb0JHsf-mUYtE1nO4SQxOBuNpjEy5MbLHCg";
        // 公钥
        String pub = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqurQLycWJIU2M24omfbzBV71X79Pk0I+oQslinZ6DdJPCZn0yPUaDSIGwMUse8UdBrBXKQm7lqjZU5Tw0MV+POOZiSA8mkyvi9g87Q8gNAiuJJ95LDZwh47R14F1s0exkFZdFhAIHjRr+J8WAvkfUmb0gCE6W1drUwzE2jhLGZewQVUt0n+KnwRsuTOx3AHrxr1CDSQMxCwe63lMnHFThVlywkOWbbxindu4s14UAePKwUtYDyf/De7kzD3u7E8lbRVfN3bHiro4y5BR6+DdtFWVE9tvwv1c6lX9CvAEzosBCPUKpA3ao4OTRX15ZUXhAsiAJQDgBLHHet5bpBXhoQIDAQAB-----END PUBLIC KEY-----";
        // 校验令牌，需要使用到RsaVerify，传入一个RsaPublicKey初始化
        Jwt jwt = JwtHelper.decodeAndVerify(token, new RsaVerifier(pub));
        // 获取载荷内容
        String claims = jwt.getClaims();
        System.out.println(claims);


    }
}
