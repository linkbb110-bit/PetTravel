package com.nfu.pettravel.mapper;

import com.nfu.pettravel.entity.PetInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author 11707
* @description 针对表【pet_info(宠物信息档案表)】的数据库操作Mapper
* @createDate 2026-05-07 08:13:11
* @Entity com.nfu.pettravel.entity.PetInfo
*/
public interface PetInfoMapper extends BaseMapper<PetInfo> {
    int updateOwned(@Param("pet") PetInfo pet, @Param("userId") Long userId);

    int logicalDeleteOwned(@Param("petId") long petId, @Param("userId") long userId);

}




