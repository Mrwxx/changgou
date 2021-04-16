package com.changgou.search.controller;

import com.changgou.search.feign.SkuFeign;
import com.changgou.search.pojo.SkuInfo;
import entity.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping(value = "/search")
public class SkuController {

    // 调用Feign
    @Autowired
    private SkuFeign skuFeign;

    /***
     * 搜索
     * @param searchMap
     * @param model
     * @return
     */
    @GetMapping(value = "/list")
    public String search(@RequestParam(required = false) Map searchMap, Model model){
        // 调用 changgou_service_search微服务
        Map resultMap = skuFeign.search(searchMap);
        model.addAttribute("result", resultMap);
        // 将搜索条件存储，便于回显操作
        model.addAttribute("searchMap", searchMap);

        // 计算分页
        Page<SkuInfo> pageInfo = new Page<SkuInfo>(
                Long.parseLong(resultMap.get("total").toString()),
                Integer.parseInt(resultMap.get("pageNumber").toString())+1,   // 注意存储的当前页是从0开始的，因此需要+1
                Integer.parseInt(resultMap.get("pageSize").toString())
        );
        model.addAttribute("pageInfo", pageInfo);

        //将搜索条件对应的地址存入model中
        // 两个url，url中包含排序参数，sortUrl中没有排序参数
        String[] urls = url(searchMap);
        model.addAttribute("url", urls[0]);
        model.addAttribute("sortUrl", urls[1]);

        return "search";
    }

    /***
     * 商品搜索条件URL组装
     */
    public String[] url(Map<String, String> searchMap){
        //URL地址
        String url = "/search/list";
        //排序地址
        String sortUrl = "/search/list";
        if(searchMap != null && searchMap.size() > 0){
            url += "?";
            sortUrl += "?";
            for(Map.Entry<String, String> entry : searchMap.entrySet()) {
                String key = entry.getKey();
                if(key.equalsIgnoreCase("pageNum")){
                    continue;
                }
                url += key+"="+entry.getValue()+"&";

                if(key.equalsIgnoreCase("sortField") || key.equalsIgnoreCase("sortRule")){
                    continue;
                }
                sortUrl += key+"="+entry.getValue()+"&";
            }
            //去掉最后一个&
            if(url.charAt(url.length()-1) == '&'){
                url = url.substring(0, url.length()-1);
            }
            if(sortUrl.charAt(sortUrl.length()-1) == '&'){
                sortUrl = sortUrl.substring(0, sortUrl.length()-1);
            }

        }
        return new String[]{url, sortUrl};
    }
}
