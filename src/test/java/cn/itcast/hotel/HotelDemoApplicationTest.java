package cn.itcast.hotel;

import cn.itcast.hotel.service.IHotelService;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class HotelDemoApplicationTest {
    private RestHighLevelClient client;

    @Autowired
    private IHotelService hotelService;

//    @Test
//    void testSearchFilters() throws IOException {
//        Map<String, List<String>> filters = hotelService.filters();
//        System.out.println(filters);
//    }
}
