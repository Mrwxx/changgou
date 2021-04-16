package com.changgou.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.goods.feign.CategoryFeign;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.feign.SpuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import com.changgou.item.service.PageService;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PageServiceImpl implements PageService {

    @Autowired
    private SpuFeign spuFeign;
    @Autowired
    private CategoryFeign categoryFeign;
    @Autowired
    private SkuFeign skuFeign;
    @Autowired
    private TemplateEngine templateEngine;

    // 生成的html文件路径，在yml中已经定义了
    @Value("${pagepath}")
    private String pagepath;

    /***
     * 首先根据SPU ID 查询出SPU，再查询分类，再查询SKU，构建数据模型
     * @param spuId
     * @return
     */
    private Map<String, Object> buildDataModel(Long spuId){
        HashMap<String, Object> dataMap = new HashMap<>();
        //获取SPU
        Result<Spu> result = spuFeign.findById(spuId);
        Spu spu = result.getData();

        //获取分类信息
        dataMap.put("category1", categoryFeign.findById(spu.getCategory1Id()).getData());
        dataMap.put("category2", categoryFeign.findById(spu.getCategory2Id()).getData());
        dataMap.put("category3", categoryFeign.findById(spu.getCategory3Id()).getData());
        if(spu.getImages() != null){
            dataMap.put("imageList", spu.getImages().split(","));
        }

        dataMap.put("specificationList", JSON.parseObject(spu.getSpecItems(), Map.class));
        dataMap.put("spu", spu);

        // 根据SPU ID查询SKU集合
        Sku skuCondition = new Sku();
        skuCondition.setSpuId(spu.getId());
        Result<List<Sku>> resultSku = skuFeign.findList(skuCondition);
        dataMap.put("skuList", resultSku.getData());
        return dataMap;
    }

    /***
     * 生成静态页
     * @param spuId
     */
    @Override
    public void createPageHtml(Long spuId) {
        // 将生成的dataMap放入上下文，作为model
        Context context = new Context();
        Map<String, Object> dataModel = buildDataModel(spuId);
        context.setVariables(dataModel);

        // 准备静态文件的路径
        File dir = new File(pagepath);
        if(!dir.exists()){
            dir.mkdirs();
        }

        //文件以SPU来命名
        File dest = new File(dir, spuId + ".html");
        //生成页面
        try(PrintWriter writer = new PrintWriter(dest, "UTF-8")){
            templateEngine.process("item", context, writer);
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
