package com.huyin.inner_auction.repository;

import com.huyin.inner_auction.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {
    List<Bid> findByAuctionIdOrderByCreatedAtDesc(UUID auctionId);



    /**
     * Lấy trang các bid của một auction, sắp xếp theo thời gian giảm dần (mới nhất trước).
     *
     * @param auctionId id auction
     * @param pageable  paging params
     * @return Page<Bid>
     */
    Page<Bid> findByAuctionIdOrderByCreatedAtDesc(UUID auctionId, Pageable pageable);

    /**
     * Lấy tất cả bid của một auction (không phân trang).
     *
     * @param auctionId id auction
     * @return danh sách Bid
     */
    List<Bid> findByAuctionId(UUID auctionId);

    /**
     * Lấy các bid của một user (theo thời gian giảm dần).
     *
     * @param userId id user
     * @return danh sách Bid
     */
    List<Bid> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Lấy bid cao nhất của một auction (ưu tiên amount lớn nhất, nếu bằng thì ưu tiên createdAt mới nhất).
     *
     * @param auctionId id auction
     * @return Optional chứa bid cao nhất nếu có
     */
    Optional<Bid> findTopByAuctionIdOrderByAmountDescCreatedAtDesc(UUID auctionId);

    /**
     * (Hữu ích trong một số trường hợp) Lấy top N bid theo amount giảm dần cho một auction.
     * Nếu cần dùng, có thể kết hợp với Pageable trên phương thức findByAuctionIdOrderByAmountDesc.
     */
    Page<Bid> findByAuctionIdOrderByAmountDesc(UUID auctionId, Pageable pageable);

}