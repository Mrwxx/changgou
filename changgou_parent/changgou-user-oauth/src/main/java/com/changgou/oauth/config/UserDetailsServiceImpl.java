package com.changgou.oauth.config;
import com.changgou.oauth.util.UserJwt;
import com.changgou.user.feign.UserFeign;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/*****
 * 自定义授权认证类
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    ClientDetailsService clientDetailsService;

    @Autowired
    private UserFeign userFeign;

    /****
     * 自定义授权认证
     * @param username
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //================客户端信息认证 开始====================
        //取出身份，如果身份为空说明没有认证
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //没有认证统一采用httpbasic认证，httpbasic中存储了client_id和client_secret，开始认证client_id和client_secret
        if(authentication==null){
            // 获取数据库表对应的客户端信息对象，根据配置的客户端信息从数据库中读取
            ClientDetails clientDetails = clientDetailsService.loadClientByClientId(username);
            if(clientDetails!=null){
                //秘钥
                String clientSecret = clientDetails.getClientSecret();
                //静态方式，最后一个参数是返回权限角色的list列表
                //return new User(username,new BCryptPasswordEncoder().encode(clientSecret), AuthorityUtils.commaSeparatedStringToAuthorityList(""));
                //数据库查找方式，数据库中的客户端密钥已经加密了，最后一个参数是返回权限角色的list列表，将角色信息用逗号分隔
                // User是UserDetails的子接口，因此返回的依然是一个客户端信息对象
                return new User(username,clientSecret, AuthorityUtils.commaSeparatedStringToAuthorityList("user, admin"));
            }
        }
        //================客户端信息认证 结束====================

        //================用户信息认证 开始====================
        if (StringUtils.isEmpty(username)) {
            return null;
        }

        // 从调用用户微服务的feign接口从数据库中加载用户信息
        Result<com.changgou.user.pojo.User> userResult = userFeign.findById(username);

        // 判空操作
        if(userResult == null || userResult.getData() == null){
            return null;
        }
        //根据用户名查询用户信息,加密用户密码
        //String pwd = new BCryptPasswordEncoder().encode("szitheima");
        String pwd = userResult.getData().getPassword();
        //创建User对象， 用户权限角色,这里需要从数据库中查询，可惜数据库表中没有设计，因此采用写死的方式
        String permissions = "user,vip, admin";
        // 封装用户权限信息的对象，最后一个参数是权限角色列表
        // 即将上面从数据库中获取的用户的信息都封装到用户信息对象中
        UserJwt userDetails = new UserJwt(username,pwd,AuthorityUtils.commaSeparatedStringToAuthorityList(permissions));

        //userDetails.setComy(songsi);
        //================用户信息认证 结束====================
        return userDetails;
    }
}
