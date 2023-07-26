package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class HotelSearchTest {
    private RestHighLevelClient client;

    private void handleResponse(SearchResponse response) {
        // 4.解析结果数据
        SearchHits searchHits = response.getHits();

        // 获取返回结果的文档总数
        long total = searchHits.getTotalHits().value;
        System.out.println("搜索的文档数据的总数为：" + total);

        SearchHit[] hits = searchHits.getHits();
        // 遍历
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            // 获取高亮字段数组
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)) {
                HighlightField highlightField = highlightFields.get("name");
                if (highlightField != null) {
                    String highlightName = highlightField.getFragments()[0].string();
                    // 替换原字段
                    hotelDoc.setName(highlightName);
                }
            }

            System.out.println("HotelDoc = " + hotelDoc);
        }
    }

    @Test
    void testMatchAll() throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("hotel");

        // 2.准备DSL
        request.source().query(QueryBuilders.matchAllQuery());

        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // System.out.println(response);
        handleResponse(response);

    }

    @Test
    void testMatch() throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("hotel");

        // 2.准备DSL
        request.source().query(QueryBuilders.matchQuery("name", "如家"));

        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // System.out.println(response);

        // 4.解析结果数据
        handleResponse(response);
    }

    @Test
    void testBool() throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("hotel");

        // 2.准备DSL
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.termQuery("city", "上海"));
        // boolQuery.filter(QueryBuilders.rangeQuery("price").lte(300));

        request.source().query(boolQuery);

        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // System.out.println(response);

        // 4.解析结果数据
        handleResponse(response);
    }

    @Test
    void testPageSort() throws IOException {
        int page = 2, size = 5;
        // 1.准备Request
        SearchRequest request = new SearchRequest("hotel");

        // 2.准备DSL
        request.source().query(QueryBuilders.matchAllQuery());
        request.source().from((page - 1) * size).size(size);
        request.source().sort("price", SortOrder.ASC);

        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // System.out.println(response);

        // 4.解析结果数据
        handleResponse(response);
    }

    @Test
    void testHighLight() throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("hotel");

        // 2.准备DSL
        request.source().query(QueryBuilders.matchQuery("name", "如家"));
        request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));

        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // System.out.println(response);

        // 4.解析结果数据
        handleResponse(response);
    }

    @Test
    void testAggregation() throws IOException {
        // 1.准备Request
        SearchRequest request = new SearchRequest("hotel");

        // 2.准备DSL
        request.source().size(0);
        request.source().aggregation(AggregationBuilders
                                     .terms("brandAgg")
                                     .field("brand")
                                     .size(20)
        );

        // 3.发送Request
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // 4.解析结果
        // System.out.println(response);
        Aggregations aggregations = response.getAggregations();
        Terms brandTerms = aggregations.get("brandAgg");
        List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
        for (Terms.Bucket bucket :buckets) {
            String key = bucket.getKeyAsString();
            System.out.println(key);
        }
    }

    @Test
    void testSuggest() throws IOException {
        // 1.准备request
        SearchRequest request = new SearchRequest("hotel");

        // 2.准备DSL
        request.source().suggest(new SuggestBuilder().addSuggestion(
                "suggestions", SuggestBuilders
                                    .completionSuggestion("suggestion")
                                    .prefix("ht")
                                    .skipDuplicates(true)
                                    .size(10)
        ));

        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // System.out.println(response);

        // 4.处理返回结果
        Suggest suggest = response.getSuggest();
        CompletionSuggestion completionSuggestion = suggest.getSuggestion("suggestions");
        List<CompletionSuggestion.Entry.Option> options = completionSuggestion.getOptions();
        for (CompletionSuggestion.Entry.Option option : options) {
            String text = option.getText().string();
            System.out.println(text);
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
