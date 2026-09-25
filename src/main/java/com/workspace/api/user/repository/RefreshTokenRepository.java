package com.workspace.api.user.repository;

import com.workspace.api.user.domain.RefreshToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    // Read only the owner before acquiring the user's lock: do not cache a stale token entity.
    @Query("select t.userId from RefreshToken t where t.tokenHash = :hash")
    Optional<String> findOwner(@Param("hash") String hash);
    Optional<RefreshToken> findByTokenHash(String hash);

    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :family and t.revokedAt is null")
    void revokeFamily(@Param("family") String family, @Param("now") Instant now);

    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    void revokeUser(@Param("userId") String userId, @Param("now") Instant now);
}
