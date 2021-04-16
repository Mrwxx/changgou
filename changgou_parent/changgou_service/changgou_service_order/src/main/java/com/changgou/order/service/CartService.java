package com.changgou.order.service;

import com.changgou.order.pojo.OrderItem;

import java.util.List;

public interface CartService {

    /***
     * 添加到购物车
     * @param num
     * @param id
     */
    public void add(Integer num, Long id, String username);

    /***
     * 从Redis中查询某个用户名的购物车数据
     * @return
     */
    List<OrderItem> list(String username);
}
