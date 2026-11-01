package com.nfu.pettravel.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.nfu.pettravel.entity.AppointmentOrder;
import com.nfu.pettravel.mapper.AppointmentOrderMapper;
import com.nfu.pettravel.service.impl.AppointmentOrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentOrderServiceImplTest {

    @Test
    void shouldBuildValidWorkbookBeforeWritingDownloadResponse() throws Exception {
        AppointmentOrderMapper mapper = mock(AppointmentOrderMapper.class);
        AppointmentOrder order = new AppointmentOrder();
        order.setId(11L);
        order.setUserId(22L);
        order.setRouteId(33L);
        when(mapper.selectList(org.mockito.ArgumentMatchers.<Wrapper<AppointmentOrder>>any()))
                .thenReturn(List.of(order));
        AppointmentOrderServiceImpl service = new AppointmentOrderServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.exportTimeoutOrders(response);

        assertThat(response.getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getContentAsByteArray())
                .startsWith(new byte[]{'P', 'K'});
    }
}
