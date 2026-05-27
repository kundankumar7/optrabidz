package com.project.optrabidz.notification.application.rule;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NotificationRecipientResolver {
    private final JdbcTemplate jdbcTemplate;

    public NotificationRecipientResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> accountByStartupId(Long startupId) {
        return queryAccount("""
                select account_id
                from startup
                where startup_id = ?
                """, startupId);
    }

    public Optional<Long> accountByInvestorId(Long investorId) {
        return queryAccount("""
                select account_id
                from investor
                where investor_id = ?
                """, investorId);
    }

    public Optional<Long> startupAccountByListingId(Long listingId) {
        return queryAccount("""
                select s.account_id
                from funding_listing fl
                join startup s on s.startup_id = fl.startup_id
                where fl.listing_id = ?
                """, listingId);
    }

    private Optional<Long> queryAccount(String sql, Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Long.class, id));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }
}
