package com.itcast.hotel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcast.hotel.mapper.HotelMapper;
import com.itcast.hotel.pojo.Hotel;
import com.itcast.hotel.service.IHotelService;
import org.springframework.stereotype.Service;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
}
