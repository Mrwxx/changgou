package com.changgou.filter;

import com.changgou.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/***
 * 全局过滤器，实现用户权限鉴别
 */
@Component
public class AuthorizerFilter implements GlobalFilter, Ordered {
    //令牌头名字
    private static final String AUTHORIZE_TOKEN = "Authorization";

    /***
     * 全局拦截
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 对于用户登录，注册等不需要做权限认证的请求，直接放行
        String uri = request.getURI().toString();
        if(URLFilter.hasAuthorize(uri)){
            return chain.filter(exchange);
        }

        // 获取令牌信息
        // 从头文件中获取
        String token = request.getHeaders().getFirst(AUTHORIZE_TOKEN);
        // 令牌是否在头文件中，因为后期令牌需要在微服务之间传递
        boolean hasToken = true;

        // 从参数中获取
        if(StringUtils.isEmpty(token)){
            token = request.getQueryParams().getFirst(AUTHORIZE_TOKEN);
            hasToken = false;
        }
        //从Cookies中获取
        if(StringUtils.isEmpty(token)){
            HttpCookie first = request.getCookies().getFirst(AUTHORIZE_TOKEN);
            if(first != null) {
                token = first.getValue();
            }
        }

        // 如果没有令牌信息，则需要进行拦截,这里只需要返回一个状态码即可
        if(StringUtils.isEmpty(token)){
            //设置没有权限的状态码
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            // 返回空数据
            return response.setComplete();
        }

        // 如果有令牌，则进行令牌的校验，如果校验通过则放行，如果不同过，依然要进行拦截
        try {
            //final Claims claims = JwtUtil.parseJWT(token);
            // 如果不在头文件中
            if(!hasToken){
                // 检测令牌格式是否正确，如果不正确则添加前缀
                if(!token.startsWith("bearer ") && !token.startsWith("Bearer ")){
                    token = "bearer " + token;
                }
                // 将令牌添加到头文件中，便于后续令牌在微服务之间的传递
                request.mutate().header(AUTHORIZE_TOKEN, token);
            }
            // 如果令牌在头文件中，也不需要校验格式，格式需由前端人员保证
        } catch (Exception e) {
            //设置没有权限的状态码
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            // 返回空数据
            return response.setComplete();
        }

        // 令牌校验通过，放行
        return chain.filter(exchange);

    }

    /***
     * 排序，越小越先执行
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
