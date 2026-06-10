package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByMilkman_IdAndRouteDate(Long milkmanId, LocalDate routeDate);
    List<Route> findByMilkman_Id(Long milkmanId);
}
