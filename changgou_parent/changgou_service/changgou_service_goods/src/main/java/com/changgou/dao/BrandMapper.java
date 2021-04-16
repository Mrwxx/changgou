package com.changgou.dao;

import com.changgou.goods.pojo.Brand;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/****
 * @Author:shenkunlin
 * @Description:Brand的Dao
 * @Date 2019/6/14 0:12
 *****/
public interface BrandMapper extends Mapper<Brand> {

    /***
     * 查询分类对应的品牌集合
     * @param categoryId
     * @return
     */
    @Select("SELECT tb.* FROM tb_brand tb, tb_category_brand tcb " +
            "WHERE tcb.category_id=#{category_id} AND tb.id=tcb.brand_id")
    List<Brand> findByCategory(Integer categoryId);
}
