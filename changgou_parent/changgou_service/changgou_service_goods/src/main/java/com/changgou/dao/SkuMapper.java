package com.changgou.dao;
import com.changgou.goods.pojo.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

/****
 * @Author:shenkunlin
 * @Description:Sku的Dao
 * @Date 2019/6/14 0:12
 *****/
public interface SkuMapper extends Mapper<Sku> {
    //购买商品，删减库存
    @Update(value = "update tb_sku set num=num-#{num} where id=#{id} and num >=#{num")
    // @param注解是为了注明该属性的名称，以免到了class文件中变为了arg0,arg1
    int decrCount(@Param(value = "id") Long id, @Param(value = "num")Integer num);
}
