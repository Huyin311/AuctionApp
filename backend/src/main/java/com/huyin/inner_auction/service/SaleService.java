package com.huyin.inner_auction.service;

import com.huyin.inner_auction.entity.Sale;

import java.util.UUID;

/**
 * Service xử lý các hành động liên quan đến Sale (kết quả của auction).
 *
 * Vietnamese:
 * - confirmDelivery: buyer xác nhận đã nhận hàng -> gọi để release tiền cho seller
 * - releaseFunds: thực hiện chuyển tiền (net_amount) cho seller, tạo transaction, ghi escrow entry
 * - autoReleasePendingSales: job gọi định kỳ để tự động release theo policy (scheduler sẽ gọi)
 */
public interface SaleService {

    /**
     * Buyer xác nhận đã nhận hàng -> system sẽ release tiền cho seller.
     *
     * @param buyerId id của buyer (người gọi)
     * @param saleId  id của sale (auction đã kết thúc)
     */
    void confirmDelivery(UUID buyerId, UUID saleId);

    /**
     * Thực hiện release funds cho sale (có thể do admin hoặc system gọi).
     *
     * @param adminOrSystemUserId id admin hoặc null nếu hệ thống gọi
     * @param saleId             id của sale cần release
     * @return Sale đã được cập nhật
     */
    Sale releaseFunds(UUID adminOrSystemUserId, UUID saleId);

    /**
     * Scheduler gọi để auto-release các sale đang ở trạng thái ESCROWED
     * theo policy (ví dụ buyer không confirm trong X ngày hoặc shipped_at + N ngày).
     */
    void autoReleasePendingSales();
}