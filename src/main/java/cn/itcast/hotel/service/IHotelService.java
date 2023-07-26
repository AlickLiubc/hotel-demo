package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface IHotelService extends IService<Hotel> {

    PageResult search(RequestParams requestParams);

    /**
     * 查询城市，星级、品牌的聚合结果
     * @return 聚合结果，格式：{"城市": ["上海", "北京], "品牌": ["如家", "希尔顿"]}
     */
    //Map<String, List<String>> filters();

    /**
     * 查询城市，星级、品牌的聚合结果
     * @return 聚合结果，格式：{"城市": ["上海", "北京], "品牌": ["如家", "希尔顿"]}
     */
    Map<String, List<String>> getFilters(RequestParams requestParams);


    List<String> getSuggestions(String prefix);

}
