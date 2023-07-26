package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    private RestHighLevelClient client;

    private PageResult handleResponse(SearchResponse response) {
        SearchHits searchHits = response.getHits();
        // 获取结果总数
        long total = searchHits.getTotalHits().value;

        PageResult pageResult = new PageResult();
        List<HotelDoc> hotels = new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        // 获取结果数据
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();

            // 将String转换为Bean对象
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            hotels.add(hotelDoc);
        }

        pageResult.setTotal(total);
        pageResult.setHotels(hotels);

        return pageResult;
    }

    @Override
    public PageResult search(RequestParams requestParams) {
        // 获取参数
        int page = requestParams.getPage();
        int size = requestParams.getSize();

        // 准备request
        SearchRequest request = new SearchRequest("hotel");
        buildBasicQuery(requestParams, request);

        // 分页
        request.source().from((page - 1) * size);
        request.source().size(size);

        // System.out.println(request.source().toString());

        // 发送request
        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);

            // 分析结果参数
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildBasicQuery(RequestParams requestParams, SearchRequest request) {
        String key = requestParams.getKey();

        String brand = requestParams.getBrand();
        String starName = requestParams.getStarName();
        String city = requestParams.getCity();

        Integer minPrice = requestParams.getMinPrice();
        Integer maxPrice = requestParams.getMaxPrice();


        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 准备DSL
        if (key == null || "".equals(key.trim())) {
            boolQuery.must(QueryBuilders.matchAllQuery());
        } else {
            boolQuery.must(QueryBuilders.matchQuery("name", key));
        }

        if (city != null && !"".equals(city)) {
            boolQuery.filter(QueryBuilders.termQuery("city", city));
        }

        if (starName != null && !"".equals(starName)) {
            boolQuery.filter(QueryBuilders.termQuery("starName", starName));
        }

        if (brand != null && !"".equals(brand)) {
            boolQuery.filter(QueryBuilders.termQuery("brand", brand));
        }

        if (minPrice != null && maxPrice != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(minPrice));
            boolQuery.filter(QueryBuilders.rangeQuery("price").lte(maxPrice));
        }

        FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(
                boolQuery,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                QueryBuilders.termQuery("isAD", true),
                                ScoreFunctionBuilders.weightFactorFunction(10)
                        )
                });

        // System.out.println(functionScoreQuery.toString());

        request.source().query(functionScoreQuery);
    }

    private List<String> handleAggResponse(String aggName, SearchResponse response) {
        List<String> aggList = new ArrayList<>();

        Aggregations aggregations = response.getAggregations();
        Terms terms = aggregations.get(aggName);
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            String keyStr = bucket.getKeyAsString();
            aggList.add(keyStr);
        }

        return aggList;
    }

//    @Override
//    public Map<String, List<String>> filters() {
//        Map<String, List<String>> filtersMap = new HashMap<>();
//
//        // 1.准备request
//        SearchRequest request = new SearchRequest("hotel");
//
//        // 2.准备DSL
//        request.source().size(0);
//        Map<String, String> chineseFieldMap = new HashMap<>();
//        chineseFieldMap.put("city", "城市");
//        chineseFieldMap.put("brand", "品牌");
//        chineseFieldMap.put("starName", "星级");
//        List<String> fieldNames = Arrays.asList("city", "brand", "starName");
//        for (String fieldName : fieldNames) {
//            request.source().aggregation(AggregationBuilders
//                                         .terms(fieldName + "Agg")
//                                         .field(fieldName)
//                                         .size(20));
//
//            // 3.发送请求
//            try {
//                SearchResponse response = client.search(request, RequestOptions.DEFAULT);
//
//                // 4.解析结果
//                filtersMap.put(chineseFieldMap.get(fieldName), handleAggResponse(fieldName + "Agg", response));
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        return filtersMap;
//    }

    @Override
    public Map<String, List<String>> getFilters(RequestParams requestParams) {
        Map<String, List<String>> filtersMap = new HashMap<>();

        // 1.准备request
        SearchRequest request = new SearchRequest("hotel");

        // 2.准备DSL
        request.source().size(0);
        buildBasicQuery(requestParams, request);

        Map<String, String> chineseFieldMap = new HashMap<>();
        chineseFieldMap.put("brand", "品牌");
        chineseFieldMap.put("starName", "星级");
        chineseFieldMap.put("city", "城市");
        List<String> fieldNames = Arrays.asList("brand", "starName", "city");
        for (String fieldName : fieldNames) {
            request.source().aggregation(AggregationBuilders
                    .terms(fieldName + "Agg")
                    .field(fieldName)
                    .size(20));

            // 3.发送请求
            try {
                SearchResponse response = client.search(request, RequestOptions.DEFAULT);

                // 4.解析结果
                filtersMap.put(fieldName, handleAggResponse(fieldName + "Agg", response));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return filtersMap;
    }

    @Override
    public List<String> getSuggestions(String prefix) {
        // 1.准备request
        SearchRequest request = new SearchRequest("hotel");

        // 2.准备DSL
        request.source().suggest(new SuggestBuilder().addSuggestion("suggestions",
                SuggestBuilders.completionSuggestion("suggestion")
                               .prefix(prefix)
                               .skipDuplicates(true)
                               .size(10)
        ));

        // 3.发送请求
        SearchResponse response = null;
        try {
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 4.处理返回结果
        Suggest suggest = response.getSuggest();
        CompletionSuggestion completionSuggestion = suggest.getSuggestion("suggestions");
        List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getOptions();
        List<String> textList = new ArrayList<>(options.size());
        for (CompletionSuggestion.Entry.Option option : options) {
            String text = option.getText().string();
            textList.add(text);
        }

        return textList;
    }
}
