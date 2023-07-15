package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class HotelSearchTest {
    private RestHighLevelClient client;

    @Test
    void testMatchAll() throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("hotel");

        // 2.准备DSL
        request.source().query(QueryBuilders.matchAllQuery());

        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // System.out.println(response);

        // 4.解析结果数据
        SearchHits searchHits = response.getHits();

        // 获取返回结果的文档总数
        long total = searchHits.getTotalHits().value;
        System.out.println("数据总量为：" + total);

        // 遍历
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();

            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);

            System.out.println(hotelDoc);
        }
    }

    @Test
    void testMatchQuery() throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("hotel");

        // 2.准备DSL
        request.source().query(QueryBuilders.matchQuery("name", "如家"));

        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // System.out.println(response);

        // 4.解析结果数据
        SearchHits searchHits = response.getHits();

        // 获取返回结果的文档总数
        long total = searchHits.getTotalHits().value;

        SearchHit[] hits = searchHits.getHits();
        // 遍历
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();

            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);

            System.out.println(hotelDoc);
        }

    }

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://127.0.0.1:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}