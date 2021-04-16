package com.changgou.order.controller;

import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import entity.Result;
import entity.StatusCode;
import entity.TokenDecode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/order")

public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private TokenDecode tokenDecode;

    // 加入购物车
    // 1.加入购物车数量 2. 商品ID
    @GetMapping(value = "/add")
    public Result add(Integer num, Long id){
        // 用户名现在只能自定义一个
//        String userName = "wxx";
        // 使用工具类解析用户信息
        Map<String, String> userInfo = TokenDecode.getUserInfo();
        System.out.println(userInfo);
        String userName = userInfo.get("username");
        cartService.add(num, id, userName);
        return new Result(true, StatusCode.OK, "加入购物车成功 ");
    }


    // 查询购物车列表
    @GetMapping(value = "/list")
    public Result<List<OrderItem>> list(){
        //用户名
//        String userName = "wxx";
        // 使用工具类解析用户信息
        Map<String, String> userInfo = TokenDecode.getUserInfo();
        System.out.println(userInfo);
        String userName = userInfo.get("username");
        List<OrderItem> list = cartService.list(userName);
        return new Result<>(true, StatusCode.OK, "查询购物车列表成功", list);
    }
}
