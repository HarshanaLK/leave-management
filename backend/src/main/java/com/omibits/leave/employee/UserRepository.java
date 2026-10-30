package com.omibits.leave.employee;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository
  extends
    JpaRepository<UserAccount, Long>,
    JpaSpecificationExecutor<UserAccount>
{
  Optional<UserAccount> findByEmailIgnoreCase(String email);

  /** Execute and verify a real SQL password update; clear the persistence context
   * to ensure a subsequent read is from the database, not a cached entity. */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
    "update UserAccount u set u.passwordHash = :hash, u.updatedAt = :changedAt where u.id = :id"
  )
  int updatePasswordHash(
    @Param("id") Long userId,
    @Param("hash") String encodedPassword,
    @Param("changedAt") Instant changedAt
  );

  boolean existsByEmailIgnoreCase(String email);
  long countByRoleAndActiveTrue(UserAccount.Role role);
  java.util.List<UserAccount> findByRoleAndActiveTrue(UserAccount.Role role);
  java.util.List<UserAccount> findByRoleInAndActiveTrue(
    java.util.Collection<UserAccount.Role> roles
  );
}
