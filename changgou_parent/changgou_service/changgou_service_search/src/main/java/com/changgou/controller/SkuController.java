package com.changgou.controller;

import com.changgou.service.SkuService;
import com.netflix.discovery.converters.Auto;
import entity.Result;
import entity.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/search")
@CrossOrigin
public class SkuController {

    @Autowired
    private SkuService skuService;

    /***
     * 搜索
     * @param searchMap
     * @return
     */
    @GetMapping
    public Map search(@RequestParam(required = false) Map searchMap){

        return skuService.search(searchMap);
    }

    /***
     * 导入SKU数据到ES中
     * @return
     */
    @GetMapping(value = "/import")
    public Result importSku(){
        skuService.importSku();
        return new Result(true, StatusCode.OK, "导入SKU数据到ES成功");
    }
}
