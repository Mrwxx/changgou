package com.changgou.oauth.interceptor;

import com.changgou.oauth.util.AdminToken;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenRequestInterceptor implements RequestInterceptor {

    /***
     * Feign执行之前进行拦截
     * @param template
     */
    @Override
    public void apply(RequestTemplate template) {
        /***
         * 从数据库加载查询用户信息
         * 1. 没有令牌，Feign调用之前，生成admin令牌
         * 2. Feign调用之前，需要将令牌放在头文件中
         *
         */
        // 调用自定义的管理员令牌生成器
        String token = AdminToken.adminToken();
        // 将令牌放置在请求的头文件中
        template.header("Authorization", "bearer " + token);
    }
}
