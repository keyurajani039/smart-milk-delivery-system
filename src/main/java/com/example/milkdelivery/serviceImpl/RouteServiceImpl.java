package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.entity.Customer;
import com.example.milkdelivery.entity.Route;
import com.example.milkdelivery.entity.RouteDetail;
import com.example.milkdelivery.entity.User;
import com.example.milkdelivery.exception.ResourceNotFoundException;
import com.example.milkdelivery.repository.CustomerRepository;
import com.example.milkdelivery.repository.RouteDetailRepository;
import com.example.milkdelivery.repository.RouteRepository;
import com.example.milkdelivery.repository.UserRepository;
import com.example.milkdelivery.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RouteServiceImpl implements RouteService {

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RouteDetailRepository routeDetailRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @CacheEvict(value = "routes", allEntries = true)
    public Route generateDailyRoute(Long milkmanId) {
        User milkman = userRepository.findById(milkmanId)
                .orElseThrow(() -> new ResourceNotFoundException("Milkman not found"));

        List<Customer> activeCustomers = customerRepository.findByUser_IdAndActiveTrue(milkmanId);
        if (activeCustomers.isEmpty()) {
            return null;
        }

        // Delete existing route for today if any
        Optional<Route> existingRoute = routeRepository.findByMilkman_IdAndRouteDate(milkmanId, LocalDate.now());
        existingRoute.ifPresent(routeRepository::delete);

        // Nearest Neighbor algorithm initialization
        // Starting Depot: Surat/Gujarat standard coord (21.1702, 72.8311) or the first customer's position
        double currentLat = 21.1702;
        double currentLon = 72.8311;

        List<Customer> unvisited = new ArrayList<>(activeCustomers);
        List<Customer> orderedPath = new ArrayList<>();
        double totalDistance = 0.0;

        while (!unvisited.isEmpty()) {
            Customer nearest = null;
            double minDistance = Double.MAX_VALUE;

            for (Customer c : unvisited) {
                double lat = c.getLatitude() != null ? c.getLatitude() : 21.1702;
                double lon = c.getLongitude() != null ? c.getLongitude() : 72.8311;
                double dist = calculateDistance(currentLat, currentLon, lat, lon);
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = c;
                }
            }

            if (nearest != null) {
                orderedPath.add(nearest);
                unvisited.remove(nearest);
                totalDistance += minDistance;
                currentLat = nearest.getLatitude() != null ? nearest.getLatitude() : currentLat;
                currentLon = nearest.getLongitude() != null ? nearest.getLongitude() : currentLon;
            } else {
                break;
            }
        }

        // Create Route summary
        Route route = Route.builder()
                .routeDate(LocalDate.now())
                .milkman(milkman)
                .totalDistance(totalDistance)
                .totalCustomers(orderedPath.size())
                .build();

        Route savedRoute = routeRepository.save(route);

        // Save Route details
        for (int i = 0; i < orderedPath.size(); i++) {
            RouteDetail detail = RouteDetail.builder()
                    .route(savedRoute)
                    .customer(orderedPath.get(i))
                    .sequenceNumber(i + 1)
                    .build();
            routeDetailRepository.save(detail);
        }

        return savedRoute;
    }

    @Override
    @Cacheable(value = "routes", key = "'today-' + #milkmanId")
    public List<RouteDetail> getTodayRoute(Long milkmanId) {
        Optional<Route> routeOpt = routeRepository.findByMilkman_IdAndRouteDate(milkmanId, LocalDate.now());
        if (routeOpt.isEmpty()) {
            // Generate dynamically if none found
            Route newRoute = generateDailyRoute(milkmanId);
            if (newRoute == null) {
                return new ArrayList<>();
            }
            return routeDetailRepository.findByRoute_IdOrderBySequenceNumberAsc(newRoute.getId());
        }
        return routeDetailRepository.findByRoute_IdOrderBySequenceNumberAsc(routeOpt.get().getId());
    }

    @Override
    public List<RouteDetail> getOptimizedRoute(Long milkmanId) {
        // This leverages OSRM or nearest neighbor directly. We default to nearest neighbor sequence.
        return getTodayRoute(milkmanId);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371 * c; // distance in KM
    }
}
