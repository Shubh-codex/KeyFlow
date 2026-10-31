package com.keyflow.config;

import com.keyflow.seller.entity.Seller;
import com.keyflow.seller.repository.SellerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * DEVELOPMENT ONLY. Active only with the "dev" profile. Ensures one known seller
 * exists so the product API can be exercised before authentication is built.
 */
@Component
@Profile("dev")
public class DevSellerSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSellerSeeder.class);
    private static final String DEV_EMAIL = "dev-seller@keyflow.local";

    private final SellerRepository sellerRepository;

    public DevSellerSeeder(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    @Override
    public void run(String... args) {
        sellerRepository.findByEmail(DEV_EMAIL).ifPresentOrElse(
                s -> log.info("Dev seller present: id={}", s.getId()),
                () -> {
                    Seller s = sellerRepository.save(new Seller(DEV_EMAIL, "Dev Seller"));
                    log.info("Dev seller created: id={}", s.getId());
                });
    }
}