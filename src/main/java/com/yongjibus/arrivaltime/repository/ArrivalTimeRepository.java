package com.yongjibus.arrivaltime.repository;

import com.yongjibus.arrivaltime.domain.ArrivalTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArrivalTimeRepository extends JpaRepository<ArrivalTime, Long> {
    Optional<List<ArrivalTime>> findByTimeIdAndDate(int timeId, LocalDate date);
}
