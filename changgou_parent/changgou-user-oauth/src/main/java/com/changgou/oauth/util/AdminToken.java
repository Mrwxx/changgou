package com.changgou.oauth.util;

import com.alibaba.fastjson.JSON;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

public class AdminToken {

    /***
     * 创建管理员令牌
     * @return
     */
    public static String adminToken(){
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
        return encoded;

    }
}
