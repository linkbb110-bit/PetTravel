package com.nfu.pettravel.service;

import com.nfu.pettravel.entity.CampRoute;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nfu.pettravel.dto.BookRouteResponse;
import com.nfu.pettravel.dto.RouteUpsertRequest;

import java.util.List;

/**
* @author 11707
* @description 针对表【camp_route(宠物营地路线库存表)】的数据库操作Service
* @createDate 2026-05-07 08:13:11
*/
public interface CampRouteService extends IService<CampRoute> {

    /**
     * 获取热门营地路线（引入 Redis 缓存）
     */
    List<CampRoute> getHotRoutesFromCache();

    void initRouteStockToRedis();

    BookRouteResponse bookRoute(Long routeId, Long petId);

    long createRoute(RouteUpsertRequest request);

    void updateRoute(long routeId, RouteUpsertRequest request);
}
