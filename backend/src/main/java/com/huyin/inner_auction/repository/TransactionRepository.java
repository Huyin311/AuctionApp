package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Lấy lịch sử giao dịch của user (theo thời gian giảm dần), có phân trang.
     *
     * @param userId   id của user
     * @param pageable paging params
     * @return trang các Transaction
     */
    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Lấy lịch sử giao dịch của user theo loại giao dịch (type), có phân trang.
     *
     * @param userId   id của user
     * @param type     loại giao dịch (ví dụ "TOPUP","HOLD","RELEASE","PAYMENT","PAYOUT","REFUND")
     * @param pageable paging params
     * @return trang các Transaction
     */
    Page<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, String type, Pageable pageable);

    /**
     * Tìm tất cả giao dịch liên quan tới một reference id (ví dụ holds, sale, payout).
     *
     * @param referenceId id tham chiếu
     * @return danh sách transactions
     */
    List<Transaction> findByReferenceId(UUID referenceId);

    /**
     * Tìm transaction mới nhất tham chiếu tới referenceId (nếu cần kiểm tra trạng thái gần nhất).
     */
    Optional<Transaction> findTopByReferenceIdOrderByCreatedAtDesc(UUID referenceId);

    /**
     * Tìm các giao dịch theo related_entity và reference_id (hữu ích khi bạn lưu nhiều loại liên quan).
     *
     * @param relatedEntity ví dụ "HOLD","SALE","PAYOUT"
     * @param referenceId   id tham chiếu
     * @return danh sách transactions tương ứng
     */
    List<Transaction> findByRelatedEntityAndReferenceId(String relatedEntity, UUID referenceId);

}