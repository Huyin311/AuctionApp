package com.huyin.inner_auction.service.impl;

import com.huyin.inner_auction.service.SaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler chạy định kỳ để tự động release funds cho các sale thỏa điều kiện.
 *
 * Vietnamese:
 * - Chạy định kỳ và gọi saleService.autoReleasePendingSales()
 * - Interval cấu hình qua property app.sale.auto-release-interval-ms (mặc định 1 giờ).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoReleaseScheduler {

    private final SaleService saleService;

    // Mặc định chạy mỗi 1 giờ; cho test có thể set nhỏ hơn trong application.properties
    @Scheduled(fixedDelayString = "${app.sale.auto-release-interval-ms:3600000}")
    public void runAutoRelease() {
        try {
            log.info("AutoReleaseScheduler: bắt đầu chạy auto-release pending sales");
            saleService.autoReleasePendingSales();
            log.info("AutoReleaseScheduler: hoàn tất");
        } catch (Exception ex) {
            log.error("AutoReleaseScheduler: lỗi khi chạy auto release", ex);
        }
    }
}