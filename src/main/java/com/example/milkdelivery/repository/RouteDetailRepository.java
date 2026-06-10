package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.RouteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RouteDetailRepository extends JpaRepository<RouteDetail, Long> {
    List<RouteDetail> findByRoute_IdOrderBySequenceNumberAsc(Long routeId);
}
