package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Auction;
import com.huyin.inner_auction.entity.AuctionImage;
import com.huyin.inner_auction.projection.AuctionWithImageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, UUID> {

    @Query("select a from Auction a " +
            "where (:status is null or a.status = :status) " +
            "and (:qLike is null or (lower(a.title) like lower(:qLike) or lower(a.description) like lower(:qLike)))")
    Page<Auction> findByStatusAndQuery(
            @Param("status") String status,
            @Param("qLike") String qLike,
            Pageable pageable
    );



    @Query(
            value = "" +
                    "SELECT " +
                    "  a.id as id, " +
                    "  a.seller_id as sellerId, " +
                    "  a.title as title, " +
                    "  a.description as description, " +
                    "  a.starting_price as startingPrice, " +
                    "  a.current_price as currentPrice, " +
                    "  a.min_increment as minIncrement, " +
                    "  a.start_at as startAt, " +
                    "  a.end_at as endAt, " +
                    "  a.reserve_price as reservePrice, " +
                    "  a.status as status, " +
                    "  a.created_at as createdAt, " +
                    "  ai.url as imageUrl " +
                    "FROM auctions a " +
                    "LEFT JOIN LATERAL ( " +
                    "  SELECT url FROM auction_images ai WHERE ai.auction_id = a.id ORDER BY ai.order_index ASC LIMIT 1 " +
                    ") ai ON true " +
                    "WHERE (:status IS NULL OR a.status = :status) " +
                    "  AND (:q IS NULL OR a.title ILIKE concat('%', :q, '%')) ",
            countQuery = "" +
                    "SELECT count(*) FROM auctions a " +
                    "WHERE (:status IS NULL OR a.status = :status) " +
                    "  AND (:q IS NULL OR a.title ILIKE concat('%', :q, '%')) ",
            nativeQuery = true
    )
    Page<AuctionWithImageView> findWithImage(
            @Param("status") String status,
            @Param("q") String q,
            Pageable pageable
    );

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