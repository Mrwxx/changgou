package com.changgou.canal.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.changgou.content.feign.ContentFeign;
import com.changgou.content.pojo.Content;
import com.changgou.item.feign.PageFeign;
import com.xpand.starter.canal.annotation.*;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@CanalEventListener
public class CanalDataEventListener {

    /***
     * 导入Feign类，调用content中的方法
     */
    @Autowired
    private ContentFeign contentFeign;

    /***
     * 调用web_item中的生成静态页方法
     */
    @Autowired
    private PageFeign pageFeign;

    // 字符串Redis
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /***
     * 增加数据的监听
     * @param eventType  数据库修改事件类型
     * @param rowData    修改的一行数据
     */
    @InsertListenPoint
    public void onEventInsert(CanalEntry.EntryType eventType, CanalEntry.RowData rowData){
        rowData.getAfterColumnsList().forEach((c) -> {
            System.out.println("By--Annotation: " + c.getName() + " :: "+ c.getValue());
        });
    }

    /***
     * 修改数据监听
     * @param rowData
     */
    @UpdateListenPoint
    public void onEnventUpdate(CanalEntry.RowData rowData){
        System.out.println("UpdateListenPoint");
        rowData.getAfterColumnsList().forEach( (c) ->{
            System.out.println("By --Annotation: " + c.getName() + " :: " + c.getValue());
        });
    }

    /***
     * 删除数据监听
     * @param eventType
     */
    @DeleteListenPoint
    public void onEventDelete(CanalEntry.EventType eventType){
        System.out.println("DeleteListenPoint");
    }

    /***
     * 自定义数据库的操作， 监听
     * @param eventType
     * @param rowData
     */
    @ListenPoint(destination = "example", schema = "changgou_content",
    table = {"tb_content_category", "tb_content"}, eventType = {
            CanalEntry.EventType.UPDATE,
            CanalEntry.EventType.DELETE,
            CanalEntry.EventType.INSERT})
    public void onEventCustomUpdate(CanalEntry.EventType eventType, CanalEntry.RowData rowData){
        // 获取categoryId的值，广告分类
        String categoryId = getColumnValue(eventType, rowData);
        // 调用Feign接口中的方法，获取该分类下的所有广告集合
        Result<List<Content>> categoryResult = contentFeign.findByCategory(Long.valueOf(categoryId));
        // 获取Result中的Data
        List<Content> data = categoryResult.getData();
        // 将list存储到Redis中
        stringRedisTemplate.boundValueOps("content_" + categoryId).set(JSON.toJSONString(data));
    }

    // 获取广告分类列的值，即广告的分类
    private String getColumnValue(CanalEntry.EventType eventType, CanalEntry.RowData rowData){

        String categoryId = "";
        // 如果是删除操作，则获取删除前的list
        if(eventType == CanalEntry.EventType.DELETE){
            for( CanalEntry.Column column :  rowData.getBeforeColumnsList()){
                if(column.getName().equalsIgnoreCase("category_id")){
                    categoryId = column.getValue();
                    return categoryId;
                }
            }
        }else{
            // 如果是添加或者更新，则获取afterList
            for(CanalEntry.Column column : rowData.getAfterColumnsList()){
                // 广告分类的列
                if(column.getName().equalsIgnoreCase("category_id")){
                    categoryId = column.getValue();
                    return categoryId;
                }
            }
        }

        return categoryId;
    }

    /***
     * SPU表修改监听
     * @param eventType
     * @param rowData
     */
    @ListenPoint(destination = "example",
                schema = "changgou_goods",
                table = {"tb_spu"},
                eventType = {CanalEntry.EventType.UPDATE, CanalEntry.EventType.INSERT,
                CanalEntry.EventType.DELETE})
    public void onEventCustomSpu(CanalEntry.EventType eventType, CanalEntry.RowData rowData){
        // 判断操作类型
        // 如果是DELETE
        if(eventType == CanalEntry.EventType.DELETE){
            String spuId = "";
            // 获取删除前的数据
            List<CanalEntry.Column> beforeColumnsList = rowData.getBeforeColumnsList();
            for(CanalEntry.Column column : beforeColumnsList){
                if(column.getName().equals("id")){
                    spuId = column.getValue();
                    break;
                }
            }
            // todo  删除静态页面
            // 提示：可以在PageFeign中添加删除静态页面的函数
        }else{
            // 新增 或者 更新
            List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();
            String spuId = "";
            for(CanalEntry.Column column : afterColumnsList){
                if(column.getName().equals("id")){
                    spuId = column.getValue();
                    break;
                }
            }
            // 更新 生成静态页
            pageFeign.createHtml(Long.valueOf(spuId));
        }
    }

}
