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
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        request.source().query(boolQuery);
    }

}
