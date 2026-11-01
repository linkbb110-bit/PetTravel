package com.nfu.pettravel.service;

import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.dto.PetInfoRequest;
import com.nfu.pettravel.enums.VaccineStatus;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.mapper.PetInfoMapper;
import com.nfu.pettravel.service.impl.PetInfoServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PetInfoServiceImplTest {

    @Test
    void shouldCreatePetOwnedByCurrentUserWithSelectedVaccineState() {
        PetInfoMapper mapper = mock(PetInfoMapper.class);
        when(mapper.insert(any())).thenAnswer(invocation -> {
            com.nfu.pettravel.entity.PetInfo pet = invocation.getArgument(0);
            pet.setId(9L);
            return 1;
        });
        PetInfoServiceImpl service = new PetInfoServiceImpl(mapper);

        long id = service.createForUser(3L,
                new PetInfoRequest("团子", "柯基", 24, null, VaccineStatus.COMPLETE, "[\"亲人\"]"));

        assertThat(id).isEqualTo(9L);
    }

    @Test
    void shouldRejectUpdateWhenOwnerConditionalUpdateAffectsNoRows() {
        PetInfoMapper mapper = mock(PetInfoMapper.class);
        when(mapper.updateOwned(any(), any(Long.class))).thenReturn(0);
        PetInfoServiceImpl service = new PetInfoServiceImpl(mapper);

        assertThatThrownBy(() -> service.updateForUser(3L, 9L,
                new PetInfoRequest("团子", "柯基", 24, null, VaccineStatus.COMPLETE, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PET_NOT_FOUND));
    }
}
