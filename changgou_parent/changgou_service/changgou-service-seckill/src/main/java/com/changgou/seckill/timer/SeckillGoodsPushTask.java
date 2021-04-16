package com.changgou.seckill.timer;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import entity.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/***
 * 定时任务
 */
@Component
public class SeckillGoodsPushTask {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;
    
    /****
     * 每30秒执行一次
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void loadGoodsPushRedis() throws ParseException {
        //System.out.println("task demo");
        // 查询出当前时间所在的5个时间段
        List<Date> dateMenus = DateUtil.getDateMenus();

        // 循环5个时间段
        for(Date dateMenu : dateMenus){
            // 将每个Date转换为字符串格式，作为Redis存储的timespace
            String timespace = "SeckillGoods_" + DateUtil.date2Str(dateMenu);
            //System.out.println(dateMenu);

            // 要查询的秒杀商品数据是有条件查询，因此构建Example以及Criterial
            Example example = new Example(SeckillGoods.class);
            Example.Criteria criteria = example.createCriteria();
            
            // 商品审核状态 必须是通过 1
            criteria.andEqualTo("status", "1");
            // 秒杀商品库存 > 0
            criteria.andGreaterThan("stockCount", 0);
            // 当前时间 >= 该商品所在秒杀时间段的开始时间
            // 加上8小时的毫秒数，因为在与数据库的UTC+8:00时区比较时会减去8小时毫秒数
            dateMenu.setTime(dateMenu.getTime() + 8 * 60 * 60 * 1000);
            criteria.andGreaterThanOrEqualTo("startTime", dateMenu);
            // 当前时间 < 该商品所在秒杀时间段的结束时间
            criteria.andLessThan("endTime", DateUtil.addDateHour(dateMenu,2));
            // 已存在Redis中的数据不会再次插入Redis，通过商品的ID号进行判断
            Set keys = redisTemplate.boundHashOps(timespace).keys();
            // 判空
            if(keys != null && keys.size() > 0){
                // 不在keys集合中即可
                criteria.andNotIn("id", keys);
            }
            // 查询数据
            List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectByExample(example);

            // 添加到Redis中
            for(SeckillGoods seckillGood : seckillGoods){

                redisTemplate.boundHashOps(timespace).put(seckillGood.getId(), seckillGood);
                //给每个商品添加一个库存队列
                //商品数据队列存储,防止高并发超卖
                Long[] ids = pushIds(seckillGood.getStockCount(), seckillGood.getId());
                redisTemplate.boundListOps("SeckillGoodsCountList_"+seckillGood.getId()).leftPushAll(ids);

            }
        }
    }

    /***
     * 将商品ID存入到数组中
     * @param len:长度
     * @param id :值
     * @return
     */
    public Long[] pushIds(int len,Long id){
        Long[] ids = new Long[len];
        for (int i = 0; i <ids.length ; i++) {
            ids[i]=id;
        }
        return ids;
    }
}
