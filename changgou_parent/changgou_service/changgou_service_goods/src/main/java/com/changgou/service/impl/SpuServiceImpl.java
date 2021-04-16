package com.changgou.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.dao.BrandMapper;
import com.changgou.dao.CategoryMapper;
import com.changgou.dao.SkuMapper;
import com.changgou.dao.SpuMapper;
import com.changgou.goods.pojo.*;
import com.changgou.service.SpuService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import entity.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/****
 * @Author:shenkunlin
 * @Description:Spu业务层接口实现类
 * @Date 2019/6/14 0:16
 *****/
@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private BrandMapper brandMapper;

    /***
     * 恢复逻辑删除的商品
     * @param spuId
     */
    @Override
    public void restore(Long spuId) {
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        // 检查是否已删除
        if(!spu.getIsDelete().equals("1")){
            throw new RuntimeException("此商品未删除");
        }
        // 设置未删除
        spu.setIsDelete("0");
        // 恢复该商品需要审核，设置未审核
        spu.setStatus("0");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /***
     * 逻辑删除商品
     * @param spuId
     */
    @Override
    public void logicDelete(Long spuId) {
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        // 检查该商品是否已下架
        if(!spu.getIsMarketable().equals("0")){
            throw new RuntimeException("必须先下架该商品才能删除");
        }
        // 删除
        spu.setIsDelete("1");
        // 设置未审核
        spu.setStatus("0");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /***
     * 批量下架
     * @param ids
     * @return
     */
    @Override
    public int pullMany(Long[] ids) {
        // 修改spu
        Spu spu = new Spu();
        spu.setIsMarketable("0");   //下架
        //批量修改，通过条件对象来匹配
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        // 判断id是否在ids数组中
        criteria.andIn("id", Arrays.asList(ids));
        // 是否已上架
        criteria.andEqualTo("isMarketable", "1");
        // 是否已通过审核
        criteria.andEqualTo("status", "1");
        // 是否未删除
        criteria.andEqualTo("isDelete", "0");
        // 根据条件对象搜索出spu对象并用新的spu对象更新上架标志
        return spuMapper.updateByExampleSelective(spu, example);
    }

    /***
     * 批量上架
     * @param ids
     * @return
     */
    @Override
    public int putMany(Long[] ids) {
        // 修改spu
        Spu spu = new Spu();
        spu.setIsMarketable("1");   //上架
        //批量修改，通过条件对象来匹配
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        // 判断id是否在ids数组中
        criteria.andIn("id", Arrays.asList(ids));
        // 是否为下架
        criteria.andEqualTo("isMarketable", "0");
        // 是否已通过审核
        criteria.andEqualTo("status", "1");
        // 是否未删除
        criteria.andEqualTo("isDelete", "0");
        // 根据条件对象更新
        return spuMapper.updateByExampleSelective(spu, example);
    }

    /***
     * 商品上架
     * @param spuId
     */
    @Override
    public void put(Long spuId) {
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        // 是否删除
        if(spu.getIsDelete().equals("1")){
            throw new RuntimeException("此商品已删除");
        }
        // 是否审核通过
        if(!spu.getStatus().equals("1")){
            throw new RuntimeException("未通过审核");
        }
        // 修改上架状态
        spu.setIsMarketable("1");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /***
     * 商品下架，修改状态即可
     * @param spuId
     */
    @Override
    public void pull(Long spuId) {
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        if(spu.getIsDelete().equals("1")){
            throw new RuntimeException(("此商品已删除！"));
        }
        spu.setIsMarketable("0");   //下架
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /***
     * 商品审核
     * @param spuId
     */
    @Override
    public void audit(Long spuId) {
        Spu spu = spuMapper.selectByPrimaryKey(spuId);
        // 判断商品是否已删除
        if(spu.getIsDelete().equalsIgnoreCase("1")){
            throw new RuntimeException("该商品已删除！");
        }
        // 实现上架和审核
        spu.setStatus("1"); //审核通过
        spu.setIsMarketable("1");   // 自动上架
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /***
     * 根据SPU 的ID查询Goods
     * @param id
     * @return
     */
    @Override
    public Goods findGoodsById(Long id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //根据SPU ID查询SKU
        Sku sku = new Sku();
        sku.setSpuId(id);
        List<Sku> skuList = skuMapper.select(sku);
        Goods goods = new Goods();
        goods.setSpu(spu);
        goods.setSkuList(skuList);
        return goods;
    }

    /***
     * 添加商品
     * @param goods
     */
    @Override
    public void saveGoods(Goods goods) {
        // 设置SPU
        // 设置一些前端没有设置的数据，对照着数据表来找
        Spu spu = goods.getSpu();

        // 判断SPU的ID是否为空
        if(spu.getId() == null){
            //为空，则增加Goods
            spu.setId(idWorker.nextId());
            spuMapper.insertSelective(spu);
        }else{
            // 修改
            spuMapper.updateByPrimaryKeySelective(spu);

            // 直接删除所有的SKU，再添加即可
            Sku sku = new Sku();
            sku.setSpuId(spu.getId());
            skuMapper.delete(sku);
        }

        Date date = new Date();

        // 3级分类
        Category category = categoryMapper.selectByPrimaryKey(spu.getCategory3Id());

        // 品牌名称
        Brand brand = brandMapper.selectByPrimaryKey(spu.getBrandId());

        //设置SKU
        List<Sku> skuList = goods.getSkuList();
        for(Sku sku : skuList){
            sku.setId(idWorker.nextId());
            // sku的name是由spu的name和sku的spec规格拼接起来的
            String name = spu.getName();
            // 防止spec为空，防止空指针异常
            if(StringUtils.isEmpty(sku.getSpec())){
                sku.setSpec("{}");
            }
            // 将spech转为map
            Map<String, String> specMap = JSON.parseObject(sku.getSpec(), Map.class);
            for(Map.Entry<String, String> entry : specMap.entrySet()){
                name += " " + entry.getValue();
            }

            sku.setName(name);
            sku.setCreateTime(date);
            sku.setUpdateTime(date);
            sku.setSpuId(spu.getId());
            // SKU的分类是SPU的第三级具体分类
            sku.setCategoryId(spu.getCategory3Id());
            sku.setCategoryName(category.getName());
            sku.setBrandName(brand.getName());
            skuMapper.insertSelective(sku);
        }
    }

    /**
     * Spu条件+分页查询
     * @param spu 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public PageInfo<Spu> findPage(Spu spu, int page, int size){
        //分页
        PageHelper.startPage(page,size);
        //搜索条件构建
        Example example = createExample(spu);
        //执行搜索
        return new PageInfo<Spu>(spuMapper.selectByExample(example));
    }

    /**
     * Spu分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageInfo<Spu> findPage(int page, int size){
        //静态分页
        PageHelper.startPage(page,size);
        //分页查询
        return new PageInfo<Spu>(spuMapper.selectAll());
    }

    /**
     * Spu条件查询
     * @param spu
     * @return
     */
    @Override
    public List<Spu> findList(Spu spu){
        //构建查询条件
        Example example = createExample(spu);
        //根据构建的条件查询数据
        return spuMapper.selectByExample(example);
    }


    /**
     * Spu构建查询对象
     * @param spu
     * @return
     */
    public Example createExample(Spu spu){
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(spu!=null){
            // 主键
            if(!StringUtils.isEmpty(spu.getId())){
                    criteria.andEqualTo("id",spu.getId());
            }
            // 货号
            if(!StringUtils.isEmpty(spu.getSn())){
                    criteria.andEqualTo("sn",spu.getSn());
            }
            // SPU名
            if(!StringUtils.isEmpty(spu.getName())){
                    criteria.andLike("name","%"+spu.getName()+"%");
            }
            // 副标题
            if(!StringUtils.isEmpty(spu.getCaption())){
                    criteria.andEqualTo("caption",spu.getCaption());
            }
            // 品牌ID
            if(!StringUtils.isEmpty(spu.getBrandId())){
                    criteria.andEqualTo("brandId",spu.getBrandId());
            }
            // 一级分类
            if(!StringUtils.isEmpty(spu.getCategory1Id())){
                    criteria.andEqualTo("category1Id",spu.getCategory1Id());
            }
            // 二级分类
            if(!StringUtils.isEmpty(spu.getCategory2Id())){
                    criteria.andEqualTo("category2Id",spu.getCategory2Id());
            }
            // 三级分类
            if(!StringUtils.isEmpty(spu.getCategory3Id())){
                    criteria.andEqualTo("category3Id",spu.getCategory3Id());
            }
            // 模板ID
            if(!StringUtils.isEmpty(spu.getTemplateId())){
                    criteria.andEqualTo("templateId",spu.getTemplateId());
            }
            // 运费模板id
            if(!StringUtils.isEmpty(spu.getFreightId())){
                    criteria.andEqualTo("freightId",spu.getFreightId());
            }
            // 图片
            if(!StringUtils.isEmpty(spu.getImage())){
                    criteria.andEqualTo("image",spu.getImage());
            }
            // 图片列表
            if(!StringUtils.isEmpty(spu.getImages())){
                    criteria.andEqualTo("images",spu.getImages());
            }
            // 售后服务
            if(!StringUtils.isEmpty(spu.getSaleService())){
                    criteria.andEqualTo("saleService",spu.getSaleService());
            }
            // 介绍
            if(!StringUtils.isEmpty(spu.getIntroduction())){
                    criteria.andEqualTo("introduction",spu.getIntroduction());
            }
            // 规格列表
            if(!StringUtils.isEmpty(spu.getSpecItems())){
                    criteria.andEqualTo("specItems",spu.getSpecItems());
            }
            // 参数列表
            if(!StringUtils.isEmpty(spu.getParaItems())){
                    criteria.andEqualTo("paraItems",spu.getParaItems());
            }
            // 销量
            if(!StringUtils.isEmpty(spu.getSaleNum())){
                    criteria.andEqualTo("saleNum",spu.getSaleNum());
            }
            // 评论数
            if(!StringUtils.isEmpty(spu.getCommentNum())){
                    criteria.andEqualTo("commentNum",spu.getCommentNum());
            }
            // 是否上架,0已下架，1已上架
            if(!StringUtils.isEmpty(spu.getIsMarketable())){
                    criteria.andEqualTo("isMarketable",spu.getIsMarketable());
            }
            // 是否启用规格
            if(!StringUtils.isEmpty(spu.getIsEnableSpec())){
                    criteria.andEqualTo("isEnableSpec",spu.getIsEnableSpec());
            }
            // 是否删除,0:未删除，1：已删除
            if(!StringUtils.isEmpty(spu.getIsDelete())){
                    criteria.andEqualTo("isDelete",spu.getIsDelete());
            }
            // 审核状态，0：未审核，1：已审核，2：审核不通过
            if(!StringUtils.isEmpty(spu.getStatus())){
                    criteria.andEqualTo("status",spu.getStatus());
            }
        }
        return example;
    }

    /**
     * 物理删除商品
     * @param id
     */
    @Override
    public void delete(Long id){
        Spu spu = spuMapper.selectByPrimaryKey(id);
        // 检查是否未被逻辑删除，只有先被逻辑删除才能被物理删除
        if(!spu.getIsDelete().equals("1")){
            throw new RuntimeException("此商品首先要被逻辑删除");
        }
        spuMapper.deleteByPrimaryKey(id);
    }

    /**
     * 修改Spu
     * @param spu
     */
    @Override
    public void update(Spu spu){
        spuMapper.updateByPrimaryKey(spu);
    }

    /**
     * 增加Spu
     * @param spu
     */
    @Override
    public void add(Spu spu){
        spuMapper.insert(spu);
    }

    /**
     * 根据ID查询Spu
     * @param id
     * @return
     */
    @Override
    public Spu findById(Long id){
        return  spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 查询Spu全部数据
     * @return
     */
    @Override
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }
}
