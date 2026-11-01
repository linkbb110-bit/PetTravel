package com.nfu.pettravel.service;

import com.nfu.pettravel.entity.PetInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nfu.pettravel.dto.PetInfoRequest;

import java.util.List;

/**
* @author 11707
* @description 针对表【pet_info(宠物信息档案表)】的数据库操作Service
* @createDate 2026-05-07 08:13:11
*/
public interface PetInfoService extends IService<PetInfo> {
    List<PetInfo> listForUser(long userId);

    long createForUser(long userId, PetInfoRequest request);

    void updateForUser(long userId, long petId, PetInfoRequest request);

    void deleteForUser(long userId, long petId);

}
