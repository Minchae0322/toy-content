package com.example.toycontent.app.carrier.repository;

import com.example.toycontent.app.carrier.domain.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CarrierRepository extends JpaRepository<Carrier, Long> {

    List<Carrier> findAllByUserIdOrderByIsDefaultDescCreatedAtAsc(Long userId);

    Optional<Carrier> findByIdAndUserId(Long id, Long userId);

    Optional<Carrier> findByUserIdAndIsDefaultTrue(Long userId);

    int countByUserId(Long userId);
}
