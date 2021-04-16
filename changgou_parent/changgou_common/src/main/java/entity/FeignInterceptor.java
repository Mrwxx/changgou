package entity;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;

public class FeignInterceptor implements RequestInterceptor {

    /***
     * Feign执行之前进行拦截
     * @param template
     */
    @Override
    public void apply(RequestTemplate template) {
        /***
         * 从数据库加载查询用户信息
         * 1. 获取用户令牌，
         * 2. 将令牌再次封装到头文件中
         *
         */
        // 记录了当前用户请求的所有数据，包括请求头，请求参数
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        Enumeration<String> headerNames = requestAttributes.getRequest().getHeaderNames();
        while(headerNames.hasMoreElements()){
            // 请求头的key
            String headerKey = headerNames.nextElement();
            // 请求头的值
            String headerValue = requestAttributes.getRequest().getHeader(headerKey);
            System.out.println(headerKey + ": " + headerValue);
            // 将请求头信息封装到头中，使用Feign调用时，会传递给下一个微服务
            template.header(headerKey, headerValue);
        }
//        // 调用自定义的管理员令牌生成器
//        String token = AdminToken.adminToken();
//        // 将令牌放置在请求的头文件中
//        template.header("Authorization", "bearer " + token);
    }
}
