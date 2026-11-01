package com.nfu.pettravel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.dto.PetInfoRequest;
import com.nfu.pettravel.entity.PetInfo;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.mapper.PetInfoMapper;
import com.nfu.pettravel.service.PetInfoService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PetInfoServiceImpl extends ServiceImpl<PetInfoMapper, PetInfo>
        implements PetInfoService {
    private final PetInfoMapper petMapper;

    public PetInfoServiceImpl(PetInfoMapper petMapper) {
        this.petMapper = petMapper;
    }

    @Override
    public List<PetInfo> listForUser(long userId) {
        return petMapper.selectList(new LambdaQueryWrapper<PetInfo>()
                .eq(PetInfo::getUserId, userId)
                .eq(PetInfo::getIsDeleted, 0)
                .orderByDesc(PetInfo::getCreateTime));
    }

    @Override
    public long createForUser(long userId, PetInfoRequest request) {
        PetInfo pet = toEntity(request);
        pet.setUserId(userId);
        pet.setCreateTime(LocalDateTime.now());
        pet.setUpdateTime(LocalDateTime.now());
        pet.setIsDeleted(0);
        if (petMapper.insert(pet) != 1 || pet.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return pet.getId();
    }

    @Override
    public void updateForUser(long userId, long petId, PetInfoRequest request) {
        PetInfo pet = toEntity(request);
        pet.setId(petId);
        if (petMapper.updateOwned(pet, userId) != 1) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }
    }

    @Override
    public void deleteForUser(long userId, long petId) {
        if (petMapper.logicalDeleteOwned(petId, userId) != 1) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }
    }

    private PetInfo toEntity(PetInfoRequest request) {
        PetInfo pet = new PetInfo();
        pet.setName(request.name());
        pet.setBreed(request.breed());
        pet.setAge(request.age());
        pet.setAvatarUrl(request.avatarUrl());
        pet.setVaccineStatus(request.vaccineStatus());
        pet.setCharacterTags(request.characterTags());
        return pet;
    }
}
