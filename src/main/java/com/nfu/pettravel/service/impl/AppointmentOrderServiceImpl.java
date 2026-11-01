package com.nfu.pettravel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nfu.pettravel.common.ErrorCode;
import com.nfu.pettravel.entity.AppointmentOrder;
import com.nfu.pettravel.enums.OrderStatus;
import com.nfu.pettravel.exception.BusinessException;
import com.nfu.pettravel.mapper.AppointmentOrderMapper;
import com.nfu.pettravel.service.AppointmentOrderService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@Slf4j
public class AppointmentOrderServiceImpl
        extends ServiceImpl<AppointmentOrderMapper, AppointmentOrder>
        implements AppointmentOrderService {

    @Override
    public boolean pay(long orderId, long userId) {
        if (getBaseMapper().markPaid(orderId, userId) != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        return true;
    }

    @Override
    public boolean complete(long orderId) {
        if (getBaseMapper().markCompleted(orderId) != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        return true;
    }

    @Override
    public void exportTimeoutOrders(HttpServletResponse response) throws IOException {
        List<AppointmentOrder> orders = list(new LambdaQueryWrapper<AppointmentOrder>()
                .eq(AppointmentOrder::getOrderStatus, OrderStatus.CANCELLED));

        Path temporaryFile = Files.createTempFile("pettravel-timeout-orders-", ".xlsx");
        try {
            SXSSFWorkbook workbook = new SXSSFWorkbook(100);
            workbook.setCompressTempFiles(true);
            try (workbook;
                 OutputStream temporaryOutput = Files.newOutputStream(temporaryFile)) {
                Sheet sheet = workbook.createSheet("超时订单报表");
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("订单编号");
                header.createCell(1).setCellValue("用户ID");
                header.createCell(2).setCellValue("路线ID");
                header.createCell(3).setCellValue("状态");

                for (int index = 0; index < orders.size(); index++) {
                    AppointmentOrder order = orders.get(index);
                    Row row = sheet.createRow(index + 1);
                    row.createCell(0).setCellValue(order.getId().toString());
                    row.createCell(1).setCellValue(order.getUserId());
                    row.createCell(2).setCellValue(order.getRouteId());
                    row.createCell(3).setCellValue("已取消");
                }
                workbook.write(temporaryOutput);
            } finally {
                workbook.dispose();
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=timeout_orders.xlsx");
            response.setContentLengthLong(Files.size(temporaryFile));
            ServletOutputStream outputStream = response.getOutputStream();
            Files.copy(temporaryFile, outputStream);
            outputStream.flush();
        } finally {
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupFailure) {
                temporaryFile.toFile().deleteOnExit();
                log.warn("Failed to delete temporary order export: {}", temporaryFile);
            }
        }
    }
}
