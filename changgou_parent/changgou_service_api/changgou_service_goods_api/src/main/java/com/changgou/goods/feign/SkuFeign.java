package com.changgou.goods.feign;

import com.changgou.goods.pojo.Sku;
import entity.Result;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "goods")
@RequestMapping(value = "/sku")
public interface SkuFeign {

    @GetMapping(value = "/status/{status}")
    Result<List<Sku>> findByStatus(@PathVariable String status);

    /***
     * 根据SPUID来搜索对应的所有的SKU信息
     * @param sku
     * @return
     */
    @PostMapping(value = "/search" )
    Result<List<Sku>> findList(@RequestBody(required = false) @ApiParam(name = "Sku对象",value = "传入JSON数据",required = false) Sku sku);

    /***
     * 根据ID查询Sku数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    Result<Sku> findById(@PathVariable Long id);

    /***
     * 购买商品，删减该商品的库存
     * @param decrmap
     * @return
     */
    @GetMapping(value = "/decr/count")
    public Result  decrCount(@RequestParam Map<String, Integer> decrmap);

}
