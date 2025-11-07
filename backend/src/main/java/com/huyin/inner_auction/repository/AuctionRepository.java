package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, UUID> {
    List<Auction> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Find auctions that have ended (endAt before provided time) and are not yet settled.
     * Used by finalizer/scheduler.
     */
    List<Auction> findByEndAtBeforeAndSettledFalse(OffsetDateTime time);

    /**
     * Danh sách auctions theo status và search trong title (phân trang).
     *
     * @param status  trạng thái (ví dụ "PUBLISHED", "FINISHED")
     * @param title   chuỗi tìm kiếm trong title
     * @param pageable pageable
     * @return trang Auction
     */
    Page<Auction> findByStatusAndTitleContainingIgnoreCase(String status, String title, Pageable pageable);

    /**
     * Danh sách auctions theo status (phân trang).
     */
    Page<Auction> findByStatus(String status, Pageable pageable);

    /**
     * Tìm auctions theo title chứa chuỗi (phân trang).
     */
    Page<Auction> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * Tìm auctions active trong khoảng thời gian (ví dụ để kiểm tra active).
     *
     * @param start  thời điểm hiện tại (so sánh start_at < now)
     * @param end    thời điểm hiện tại (so sánh end_at > now)
     * @param status trạng thái (thường "PUBLISHED")
     * @return danh sách auctions thỏa điều kiện
     */
    List<Auction> findByStartAtBeforeAndEndAtAfterAndStatus(OffsetDateTime start, OffsetDateTime end, String status);

    /**
     * Tìm auctions theo seller và trạng thái (dùng cho dashboard seller).
     */
    List<Auction> findBySellerIdAndStatus(UUID sellerId, String status);
}