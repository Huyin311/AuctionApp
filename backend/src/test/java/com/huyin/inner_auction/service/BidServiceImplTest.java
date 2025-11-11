package com.huyin.inner_auction.service;

import com.huyin.inner_auction.entity.*;
import com.huyin.inner_auction.repository.*;
import com.huyin.inner_auction.service.impl.BidServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BidServiceImplTest {

    @Autowired
    private BidService bidService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private HoldRepository holdRepository;

    @Autowired
    private BidRepository bidRepository;

    private User userA;
    private User userB;
    private Auction auction;

    @BeforeEach
    void setUp() {
        userA = User.builder().id(UUID.randomUUID()).balance(BigDecimal.valueOf(1_000_000)).build();
        userRepository.save(userA);

        userB = User.builder().id(UUID.randomUUID()).balance(BigDecimal.valueOf(1_000_000)).build();
        userRepository.save(userB);

        auction = Auction.builder()
                .id(UUID.randomUUID())
                .title("Test")
                .status("PUBLISHED")
                .startingPrice(BigDecimal.valueOf(100_000))
                .minIncrement(BigDecimal.valueOf(5000))
                .startAt(Instant.now().minusSeconds(60))
                .endAt(Instant.now().plusSeconds(3600))
                .build();
        auctionRepository.save(auction);
    }

    @Test
    void whenUserIncreasesBid_onlyAdditionalIsHeld() {
        // userA places initial bid 100k
        Bid b1 = bidService.placeBid(userA.getId(), auction.getId(), BigDecimal.valueOf(100_000));
        assertNotNull(b1);
        Hold h1 = holdRepository.findTopByAuctionIdAndStatusOrderByCreatedAtDesc(auction.getId(), "HELD").orElse(null);
        assertNotNull(h1);
        assertEquals(0, BigDecimal.valueOf(100_000).compareTo(h1.getAmount()));

        // userA increases to 120k (additional 20k should be held)
        Bid b2 = bidService.placeBid(userA.getId(), auction.getId(), BigDecimal.valueOf(120_000));
        assertNotNull(b2);

        Hold h2 = holdRepository.findTopByAuctionIdAndStatusOrderByCreatedAtDesc(auction.getId(), "HELD").orElse(null);
        assertNotNull(h2);
        // final held for userA should be 120k
        assertEquals(0, BigDecimal.valueOf(120_000).compareTo(h2.getAmount()));

        // user's balance should have decreased by total held (120k)
        User refreshed = userRepository.findById(userA.getId()).orElseThrow();
        assertTrue(refreshed.getBalance().compareTo(BigDecimal.valueOf(1_000_000).subtract(BigDecimal.valueOf(120_000))) == 0);
    }

    @Test
    void whenOutbid_previousUserIsRefunded() {
        // userA bids 150k
        bidService.placeBid(userA.getId(), auction.getId(), BigDecimal.valueOf(150_000));
        Hold holdA = holdRepository.findTopByAuctionIdAndStatusOrderByCreatedAtDesc(auction.getId(), "HELD").orElse(null);
        assertNotNull(holdA);
        assertEquals(userA.getId(), holdA.getUserId());

        BigDecimal balanceAAfterHold = userRepository.findById(userA.getId()).get().getBalance();

        // userB outbids with 200k
        bidService.placeBid(userB.getId(), auction.getId(), BigDecimal.valueOf(200_000));

        // previous hold for userA should be RELEASED and user's balance refunded (i.e., increased by previous hold amount)
        Hold prev = holdRepository.findById(holdA.getId()).orElse(null);
        assertNotNull(prev);
        assertEquals("RELEASED", prev.getStatus());

        BigDecimal balanceAAfterRefund = userRepository.findById(userA.getId()).get().getBalance();
        assertEquals(0, balanceAAfterRefund.compareTo(balanceAAfterHold.add(holdA.getAmount())));
    }
}