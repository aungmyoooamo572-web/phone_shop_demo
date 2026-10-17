package com.example.demo.catalog.dao;

import com.example.demo.catalog.entity.PhoneVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhoneVariantRepository extends JpaRepository<PhoneVariant, UUID> {

    Optional<PhoneVariant> findBySku(String sku);

    List<PhoneVariant> findByPhoneId(UUID phoneId);

    // Derived from the "deleted" property of PhoneVariant (the JSON name is
    // still "isDeleted"); kept aligned with the field rename.
    List<PhoneVariant> findByPhoneIdAndDeletedFalse(UUID phoneId);

    /*
     * Reads the requested variants with an exclusive write lock, ordered by id so
     * every transaction takes the locks in the same order (no deadlocks).
     * The locking read bypasses any stale copy in the persistence context, which
     * is what makes the following stock check + deduction race free.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from PhoneVariant v where v.id in :ids order by v.id")
    List<PhoneVariant> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);

}
