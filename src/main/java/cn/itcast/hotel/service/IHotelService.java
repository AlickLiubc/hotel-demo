package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.itcast.hotel.pojo.Hotel;

public interface IHotelService extends IService<Hotel> {

    PageResult search(RequestParams requestParams);

}
