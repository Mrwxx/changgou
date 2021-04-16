package com.changgou.seckill.task;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.dao.SeckillOrderMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.pojo.SeckillOrder;
import entity.IdWorker;
import entity.SeckillStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MultiThreadingCreateOrder {

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private IdWorker idWorker;

    /***
     * 多线程下单操作
     */
    @Async
    public void addOrder(){

        try {
            System.out.println("睡觉");
            Thread.sleep(10000);

            // 从Redis队列中获取订单排队状态
            SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();
            // 判空操作
            if(seckillStatus == null ){
                return;
            }
            //时间区间
            String time = seckillStatus.getTime();
            //用户登录名
            String username = seckillStatus.getUsername();
            //用户抢购商品
            Long id = seckillStatus.getGoodsId();

            // 先到该商品的库存队列中获取库存，如果能够获取到，则说明还有库存，则创建订单
            // 否则说明没有库存，删除与该用户下单相关的信息
            Object sgoods = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).rightPop();
            if(sgoods == null){
                // 没有库存，则删除该用户该次下单信息
                clearUserQueue(username);
                return;
            }

//            //时间区间
//            String time = "2021030818";
//            //用户登录名
//            String username = "itheima";
//            //用户抢购商品
//            Long id = 1131814839027634176L;

            //System.out.println("开始下单");
            // 创建秒杀订单
            // 查询秒杀商品
            String namespace = "SeckillGoods_" + time;
            SeckillGoods seckillGoods = (SeckillGoods)redisTemplate.boundHashOps(namespace).get(id);

            // 判断是否还有库存
            if(seckillGoods == null || seckillGoods.getStockCount() <= 0){
                // 买完了
                throw new RuntimeException("已售罄");
            }
            //创建秒杀订单
            SeckillOrder secKillOrder = new SeckillOrder();
            secKillOrder.setSeckillId(id);  // 商品ID
            secKillOrder.setMoney(seckillGoods.getCostPrice()); //支付金额
            secKillOrder.setUserId(username);   // 用户名
            secKillOrder.setCreateTime(new Date()); //创建时间
            secKillOrder.setStatus("0");    // 未支付状态
            secKillOrder.setId(idWorker.nextId());    // 订单ID


            // 订单创建完成，需要存储订单到Redis中
            // 由于一个用户只允许一个订单，因此使用HashMap即可
            redisTemplate.boundHashOps("SeckillOrder").put(username, secKillOrder);
            System.out.println("下单成功");
            // 递减库存，如果该商品是最后一个商品，还需要删除Redis中的是商品数据
            seckillGoods.setStockCount(seckillGoods.getStockCount()-1);
            // 获取Redis商品库存队列中的库存数据
            Long size = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).size();

            if(size <= 0){
                // 该商品已经卖完了
                // 同步库存数据
                seckillGoods.setStockCount(size.intValue());
                // 同步数据到MySQL中
                seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);
                // 删除Redis中的商品信息
                redisTemplate.boundHashOps(namespace).delete(id);
            }else{
                // 更新商品库存
                redisTemplate.boundHashOps(namespace).put(id, seckillGoods);
            }

            // 更新排队订单状态
            seckillStatus.setOrderId(secKillOrder.getId());
            seckillStatus.setStatus(2); //代付款
            seckillStatus.setMoney(Float.valueOf(seckillGoods.getCostPrice()));
            // 更新订单状态
            redisTemplate.boundHashOps("UserQueueStatus").put(username, seckillStatus);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /***
     * 如果没有库存了，用户没有取到购买资格，则清理用户排队抢单信息
     * @param username
     */
    public void clearUserQueue(String username){
        // 清理排队标识
        redisTemplate.boundHashOps("UserQueueCount").delete(username);
        // 清理用户排队信息
        redisTemplate.boundHashOps("UserQueueStatus").delete(username);
    }
}
