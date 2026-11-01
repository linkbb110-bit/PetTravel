package com.nfu.pettravel.controller;

import com.nfu.pettravel.common.ApiResponse;
import com.nfu.pettravel.common.annotation.AdminOnly;
import com.nfu.pettravel.common.annotation.Log;
import com.nfu.pettravel.dto.BookRouteRequest;
import com.nfu.pettravel.dto.BookRouteResponse;
import com.nfu.pettravel.dto.RouteUpsertRequest;
import com.nfu.pettravel.entity.CampRoute;
import com.nfu.pettravel.service.CampRouteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/route")
@Validated
public class CampRouteController {
    private final CampRouteService campRouteService;

    public CampRouteController(CampRouteService campRouteService) {
        this.campRouteService = campRouteService;
    }

    @Log(title = "获取热门路线列表")
    @GetMapping("/hot")
    public ApiResponse<List<CampRoute>> getHotRoutes() {
        return ApiResponse.success(campRouteService.getHotRoutesFromCache());
    }

    @Log(title = "手动初始化营地库存")
    @AdminOnly
    @PostMapping("/init-stock")
    public ApiResponse<Void> initStock() {
        campRouteService.initRouteStockToRedis();
        return ApiResponse.success();
    }

    @Log(title = "预约营地")
    @PostMapping("/book/{routeId}")
    public ApiResponse<BookRouteResponse> bookRoute(
            @PathVariable @Positive Long routeId,
            @Valid @RequestBody BookRouteRequest request) {
        return ApiResponse.success(campRouteService.bookRoute(routeId, request.petId()));
    }

    @AdminOnly
    @PostMapping
    public ApiResponse<Long> createRoute(@Valid @RequestBody RouteUpsertRequest request) {
        return ApiResponse.success(campRouteService.createRoute(request));
    }

    @AdminOnly
    @PutMapping("/{routeId}")
    public ApiResponse<Void> updateRoute(
            @PathVariable @Positive long routeId,
            @Valid @RequestBody RouteUpsertRequest request) {
        campRouteService.updateRoute(routeId, request);
        return ApiResponse.success();
    }
}
