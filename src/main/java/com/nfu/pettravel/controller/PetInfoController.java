package com.nfu.pettravel.controller;

import com.nfu.pettravel.common.ApiResponse;
import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.dto.PetInfoRequest;
import com.nfu.pettravel.entity.PetInfo;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.service.PetInfoService;
import com.nfu.pettravel.utils.UserContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@Validated
public class PetInfoController {
    private final PetInfoService petInfoService;

    public PetInfoController(PetInfoService petInfoService) {
        this.petInfoService = petInfoService;
    }

    @GetMapping
    public ApiResponse<List<PetInfo>> list() {
        return ApiResponse.success(petInfoService.listForUser(currentUserId()));
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody PetInfoRequest request) {
        return ApiResponse.success(petInfoService.createForUser(currentUserId(), request));
    }

    @PutMapping("/{petId}")
    public ApiResponse<Void> update(
            @PathVariable @Positive long petId,
            @Valid @RequestBody PetInfoRequest request) {
        petInfoService.updateForUser(currentUserId(), petId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{petId}")
    public ApiResponse<Void> delete(@PathVariable @Positive long petId) {
        petInfoService.deleteForUser(currentUserId(), petId);
        return ApiResponse.success();
    }

    private long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }
}
