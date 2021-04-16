package com.changgou.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.changgou.dao.SkuEsMapper;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.search.pojo.SkuInfo;
import com.changgou.service.SkuService;
import entity.Result;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.Field;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.naming.event.ObjectChangeListener;
import java.util.*;

@Service
public class SkuServiceImpl implements SkuService {

    // goods中的SKU的Feign接口
    @Autowired
    private SkuFeign skuFeign;
    // Es的插入
    @Autowired
    private SkuEsMapper skuEsMapper;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    /***
     * 导入SKU数据
     * 首先需要通过feign调用goods的数据库查询接口
     * 然后通过ES的插入接口插入ES中
     */
    @Override
    public void importSku() {
        // 查询审核通过的SKU
        Result<List<Sku>> skuListResult = skuFeign.findByStatus("1");
        // 将数据转换为 search.skuInfo
        // 先转换为Json，再将Json解析为List数组
        List<SkuInfo> skuInfos = JSON.parseArray(JSON.toJSONString(skuListResult.getData()), SkuInfo.class);
        // 还有规格参数需要动态地转换出来
        for(SkuInfo skuInfo : skuInfos){
            Map<String, Object> specMap = JSON.parseObject(skuInfo.getSpec());
            skuInfo.setSpecMap(specMap);
        }
        skuEsMapper.saveAll(skuInfos);
    }

    /***
     * 搜索商品
     * @param searchMap
     * @return
     */
    @Override
    public Map<String, Object> search(Map<String, String> searchMap) {
        // 搜索条件封装
        NativeSearchQueryBuilder nativeSearchQueryBuilder = buildBasicQuery(searchMap);

        // 返回集合查询
        Map resultMap = searchList(nativeSearchQueryBuilder);

        // 当SKU种类已经被前端选择时，就不需要SKU种类的分组查询了
        // SKU种类分组查询
        if(searchMap == null ||  StringUtils.isEmpty(searchMap.get("category"))){
            List<String> categoryList = searchCategoryList(nativeSearchQueryBuilder);
            resultMap.put("searchCategory", categoryList);
        }

        if(searchMap == null || StringUtils.isEmpty(searchMap.get("brand"))){
            // 当SKU品牌已经被前端选择时，就不需要SKU品牌的分组查询了
            //SKU 品牌分组查询
            List<String> brandList = searchBrandList(nativeSearchQueryBuilder);
            resultMap.put("searchBrand", brandList);
        }

        Map<String, Set<String>> specList = searchSpecList(nativeSearchQueryBuilder);
        resultMap.put("searchSpec", specList);

        return resultMap;

    }

    // 封装搜索条件
    private NativeSearchQueryBuilder buildBasicQuery(Map<String, String> searchMap) {
        // 构建条件搜索构造对象，需要通过build()方法来转换为query对象用于搜索
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        // 使用bool条件构建多个bool条件， must, must_not, should
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        if(searchMap != null && searchMap.size() > 0){
            // 获取关键词
            String keywords = searchMap.get("keywords");
            // 关键词非空，搜索关键词
            if(!StringUtils.isEmpty(keywords)){
                // 设定搜索的指定域，ES中的域
                //nativeSearchQueryBuilder.withQuery(QueryBuilders.queryStringQuery(keywords).field("name"));
                boolQueryBuilder.must(QueryBuilders.queryStringQuery(keywords).field("name"));
            }

            // 过滤分类，当输入了分类时，需要限制该分类所关联的其他属性
            if(!StringUtils.isEmpty(searchMap.get("category"))){
                // 分类名整体搜索，不分词
                boolQueryBuilder.must(QueryBuilders.termsQuery("categoryName", searchMap.get("category")));
            }

            // 过滤品牌，当输入了品牌后，需要限制品牌所关联的其他属性
            if(!StringUtils.isEmpty(searchMap.get("brand"))){
                // 品牌名整体搜索，不分词
                boolQueryBuilder.must(QueryBuilders.termsQuery("brandName", searchMap.get("brand")));
            }

            // 规格过滤实现，规格有多个，因此对每个传过来的参数进行过滤
            for (Map.Entry<String, String> entry : searchMap.entrySet()) {
                String key = entry.getKey();
                // 对key进行校验，检查是否是spec_开头的参数，即规格参数
                if(key.startsWith("spec_")){
                    // 规格参数对应的值
                    String value = entry.getValue();
                    // 对这个值进行ES过滤搜索, 构建ES中的spec某个规格参数对应的索引值，keyword保证不分词
                    boolQueryBuilder.must(QueryBuilders.termsQuery("specMap." + key.substring(5) + ".keyword",value ));
                }
            }

            // 价格区间过滤
            // 首先取出区间的两个数字
            String price = searchMap.get("price");
            if(!StringUtils.isEmpty(price)){
                price = price.replace("元", "").replace("以上", "");
                String[] prices = price.split("-");
                if(prices != null && prices.length > 0){
                    // 第一个参数是左边界
                    boolQueryBuilder.must(QueryBuilders.rangeQuery("price").gt(Integer.parseInt(prices[0])));
                    // 如果还有第二个参数
                    if(prices.length == 2){
                        boolQueryBuilder.must(QueryBuilders.rangeQuery("price").lt(Integer.parseInt(prices[1])));
                    }
                }
            }

            // 根据价格进行排序
            String sortField = searchMap.get("sortField"); // 指定排序的域
            String sortRule = searchMap.get("sortRule");   // 指定排序的规则
            if(!StringUtils.isEmpty(sortField) && !StringUtils.isEmpty(sortRule)){
                nativeSearchQueryBuilder.withSort(
                        new FieldSortBuilder(sortField)         // 指定排序域
                                .order(SortOrder.valueOf(sortRule)) // 指定排序规则 ， SortOrder是一个enum枚举类，存有排序规则
                );
            }

            // 分页，如果用户不传页数，默认为1
            Integer pageNum = converterPage(searchMap);
            // 默认页面数据量
            Integer size = 10;
            // PageRequest.of() page参数以0为起点
            nativeSearchQueryBuilder.withPageable(PageRequest.of(pageNum-1, size));
        }
        // 将bool条件对象插入到native中
        nativeSearchQueryBuilder.withQuery(boolQueryBuilder);
        return nativeSearchQueryBuilder;
    }


    /***
     * 从前台获取点击的页码
     * @param searchMap
     * @return
     */
    public Integer converterPage(Map<String, String> searchMap){
        if(searchMap != null ){
            if(!StringUtils.isEmpty(searchMap.get("pageNum"))){
                return Integer.parseInt(searchMap.get("pageNum"));
            }else{
                return 1;
            }
        }
        // 默认页码为1
        return 1;
    }

    // 搜索返回集合
    private Map searchList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {
        // 高亮配置
        // 高亮的域
        HighlightBuilder.Field field = new HighlightBuilder.Field("name");
        // 设置前缀
        field.preTags("<em style=\"color:red;\">");
        // 设置后缀
        field.postTags("</em>");
        // 设置碎片长度
        field.fragmentSize(100);
        // 添加高亮到搜索条件中
        nativeSearchQueryBuilder.withHighlightFields(field);

        // 执行搜索，生成响应结果,可以看到它需要的是一个query对象，以及相应结果集合需要转换的类型,还有搜索结果集的封装
        //AggregatedPage<SkuInfo> page = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);
        AggregatedPage<SkuInfo> page = elasticsearchTemplate.queryForPage(
                nativeSearchQueryBuilder.build(),   // 搜索条件封装
                SkuInfo.class,                      // 数据集合要转换的类型字节码
                new SearchResultMapper() {          // 将数据结果集封装到该对象中，接口需要创建一个实现类
                    @Override
                    public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                        // 存储所有高亮数据
                        ArrayList<T> list = new ArrayList<>();
                        // 执行查询，获取所有数据 -》 结果集
                        for (SearchHit hit : searchResponse.getHits()) {
                            // 分析结果集数据，获取非高亮数据
                            SkuInfo skuInfo = JSON.parseObject(hit.getSourceAsString(), SkuInfo.class);
                            // 获取高亮数据，只有某个域
                            HighlightField highlightField = hit.getHighlightFields().get("name");
                            if (highlightField != null && highlightField.getFragments() != null) {
                                // 读取高亮数据,之前已经配置过了高亮数据的格式
                                Text[] fragments = highlightField.getFragments();
                                StringBuffer buffer = new StringBuffer();
                                // 设置新的高亮数据
                                for (Text fragment : fragments) {
                                    buffer.append(fragment.toString());
                                }
                                skuInfo.setName(buffer.toString());
                            }
                            // 将高亮数据添加到集合
                            list.add((T) skuInfo);
                        }
                        /***
                         * 1. 搜索的集合数据，携带高亮数据
                         * 2. 分页对象信息
                         * 3. 搜索记录的总条数
                         */
                        return new AggregatedPageImpl<T>(list, pageable, searchResponse.getHits().getTotalHits());
                    }
                });


        // 总记录数
        long totalElements = page.getTotalElements();
        List<SkuInfo> content = page.getContent();
        int totalPages = page.getTotalPages();
        Map resultMap = new HashMap<>();
        resultMap.put("rows", content);
        resultMap.put("total", totalElements);
        resultMap.put("totalPages", totalPages);

        // 获取搜索条件封装信息
        NativeSearchQuery query = nativeSearchQueryBuilder.build();
        Pageable pageable = query.getPageable();
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        // 分页数据
        resultMap.put("pageNumber", pageNumber);
        resultMap.put("pageSize", pageSize);

        return resultMap;
    }

    /***
     * 对种类进行分组查询
     * @param nativeSearchQueryBuilder
     * @return
     */
    private List<String> searchCategoryList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {
        // 分组查询分类集合
        // addAggregation(): 添加一个聚合操作
        // 1. 别名 2. 表示根据哪个域进行分组
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuCategory").field("categoryName"));
        AggregatedPage<SkuInfo> aggregatedPage =  elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);

        /***
         * 获取分组数据
         * aggregatedPage.getAggregations() 获取的是Aggregation集合，可以根据多个域进行分组
         */
        StringTerms stringTerms = aggregatedPage.getAggregations().get("skuCategory");
        List<String> categoryList = new ArrayList<>();
        for (StringTerms.Bucket bucket: stringTerms.getBuckets()) {
            String categoryName = bucket.getKeyAsString();
            categoryList.add(categoryName);
        }
        return categoryList;
    }

    /***
     * 对品牌进行分组查询
     * @param nativeSearchQueryBuilder
     * @return
     */
    private List<String> searchBrandList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {
        // 分组查询品牌集合
        // addAggregation(): 添加一个聚合操作
        // 1. 别名 2. 表示根据哪个域进行分组
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuBrand").field("brandName"));
        AggregatedPage<SkuInfo> aggregatedPage =  elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);

        /***
         * 获取分组数据
         * aggregatedPage.getAggregations() 获取的是Aggregation集合，可以根据多个域进行分组
         */
        StringTerms stringTerms = aggregatedPage.getAggregations().get("skuBrand");
        List<String> brandList = new ArrayList<>();
        for (StringTerms.Bucket bucket: stringTerms.getBuckets()) {
            String categoryName = bucket.getKeyAsString();
            brandList.add(categoryName);
        }
        return brandList;
    }

    /***
     * 对规格参数进行分组查询
     * @param nativeSearchQueryBuilder
     * @return
     */
    private Map<String, Set<String>> searchSpecList(NativeSearchQueryBuilder nativeSearchQueryBuilder) {
        // 分组查询规格参数集合
        // addAggregation(): 添加一个聚合操作
        // 1. 别名 2. 表示根据哪个域进行分组
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("skuSpec").field("spec.keyword").size(10000));
        AggregatedPage<SkuInfo> aggregatedPage =  elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), SkuInfo.class);

        /***
         * 获取分组数据
         * aggregatedPage.getAggregations() 获取的是Aggregation集合，可以根据多个域进行分组
         */
        StringTerms stringTerms = aggregatedPage.getAggregations().get("skuSpec");
        List<String> specList = new ArrayList<>();
        for (StringTerms.Bucket bucket: stringTerms.getBuckets()) {
            String categoryName = bucket.getKeyAsString();
            specList.add(categoryName);
        }
        // 规格汇总合并到Map<String, Set<String>>中
        Map<String, Set<String>> allSpec = putAllSpec(specList);

        return allSpec;
    }

    /***
     * 规格汇总合并
     * @param specList
     * @return
     */
    private Map<String, Set<String>> putAllSpec(List<String> specList) {
        // specList是多个字符串的组合，需要拆分
        // 合并后的map
        Map<String, Set<String>> allSpec = new HashMap<>();
        // 循环specList，划分每个Json字符串
        for(String spec : specList){
            // 将每个spec Json字符串转为Map
            Map<String, String> specMap = JSON.parseObject(spec, Map.class);
            // 合并所有map的键与值到allSpec中
            for(Map.Entry<String, String> entry : specMap.entrySet()){
                // 取出当前的map，并获取对应的key和value
                String key = entry.getKey();
                String value = entry.getValue();
                // 合并到allSpec中
                // 首先从allSpec中获取
                Set<String> specSet = allSpec.get(key);
                if(specSet == null){
                    specSet = new HashSet<String>();
                }
                specSet.add(value);
                allSpec.put(key, specSet);
            }
        }
        return allSpec;
    }
}
