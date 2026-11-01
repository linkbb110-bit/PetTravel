package com.nfu.pettravel.mapper;

import com.nfu.pettravel.entity.CampRoute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
* @author 11707
* @description 针对表【camp_route(宠物营地路线库存表)】的数据库操作Mapper
* @createDate 2026-05-07 08:13:11
* @Entity com.nfu.pettravel.entity.CampRoute
*/
public interface CampRouteMapper extends BaseMapper<CampRoute> {
    int decreaseStock(@Param("routeId") long routeId, @Param("quantity") int quantity);

    int increaseStock(@Param("routeId") long routeId, @Param("quantity") int quantity);

    Integer selectAvailableStockForUpdate(@Param("routeId") long routeId);

    int updateDefinition(@Param("routeId") long routeId,
                         @Param("title") String title,
                         @Param("price") BigDecimal price,
                         @Param("totalStock") int totalStock,
                         @Param("status") int status);

}




