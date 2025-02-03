package com.yongjibus.arrivaltime.repository;

import com.yongjibus.arrivaltime.domain.ArrivalTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ArrivalTimeRepository extends JpaRepository<ArrivalTime, Long> {
    List<ArrivalTime> findByTimeIdAndDate(int timeId, LocalDate date);
    List<ArrivalTime> findByDate(LocalDate date);
}
