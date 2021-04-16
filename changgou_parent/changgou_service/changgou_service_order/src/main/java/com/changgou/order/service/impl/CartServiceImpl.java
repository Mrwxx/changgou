package com.changgou.order.service.impl;

import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.feign.SpuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    // Redis操作
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private SpuFeign spuFeign;

    /***
     * 添加商品到购物车
     * @param num
     * @param id
     */
    @Override
    public void add(Integer num, Long id, String username) {
        // 查询出商品的详细信息， 有SPU的数据，也有SKU的数据，因此需要feign的支持
        // 首先是根据商品的ID查询出SKU数据，再根据SKU中的SPU的ID查询出SPU数据

        // 判断num是否 <= 0
        if(num <= 0){
            // 删除购物车中该商品信息
            redisTemplate.boundHashOps("Cart_"+username).delete(id);
            // 如果该用户的整个购物车中的剩余值为0，则删除购物车
            Long size = redisTemplate.boundHashOps("Cart_" + username).size();
            if(size <= 0){
                // 删除该用户的整个购物车
                redisTemplate.delete("Cart_" + username);
            }
            return;
        }
        // 查询SKU数据
        Result<Sku> skuResult = skuFeign.findById(id);
        Sku sku = skuResult.getData();

        // 查询SPU数据
        Result<Spu> spuResult = spuFeign.findById(sku.getSpuId());
        Spu spu = spuResult.getData();
        // 创建OrderItem对象
        OrderItem orderItem = createOrderItem(num, id, sku, spu);

        // 将商品信息保存到Redis->username中
        redisTemplate.boundHashOps("Cart_" + username).put(id, orderItem);

    }

    /***
     * 从Redis中查询出某个用户的购物车数据
     * @return
     */
    @Override
    public List<OrderItem> list(String username) {
        return redisTemplate.boundHashOps("Cart_" + username).values();
    }

    /***
     * 创建一个OrderItem对象
     * @param num
     * @param id
     * @param sku
     * @param spu
     * @return
     */
    private OrderItem createOrderItem(Integer num, Long id, Sku sku, Spu spu) {
        // 封装商品信息到OrderItem中
        OrderItem orderItem = new OrderItem();
        orderItem.setCategoryId1(spu.getCategory1Id());
        orderItem.setCategoryId2(spu.getCategory2Id());
        orderItem.setCategoryId3(spu.getCategory3Id());
        orderItem.setSpuId(spu.getId());
        orderItem.setSkuId(id);
        //orderItem.setOrderId();
        orderItem.setName(sku.getName());
        orderItem.setPrice(sku.getPrice());
        orderItem.setNum(num);
        orderItem.setMoney(num * orderItem.getPrice());
//        orderItem.setPayMoney();
        orderItem.setImage(spu.getImage());
        // 运费，重量，退货状态 todo
        return orderItem;
    }
}
