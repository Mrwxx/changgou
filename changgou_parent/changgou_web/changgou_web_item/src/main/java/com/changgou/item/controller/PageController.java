package com.changgou.item.controller;

import com.changgou.item.service.PageService;
import entity.Result;
import entity.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/page")
public class PageController {


    @Autowired
    private PageService pageService;

    /**
     * 根据SPU商品ID生成静态页面
     * @param id
     * @return
     */
    @RequestMapping("/createHtml/{id}")
    public Result createHtml(@PathVariable(name="id") Long id){
        pageService.createPageHtml(id);
        return new Result(true, StatusCode.OK,"ok");
    }

    @RequestMapping(value = "/test")

    public String test(){
        return "item";
    }
}
