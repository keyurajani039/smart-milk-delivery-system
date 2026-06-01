package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.dto.DeliveryDto;
import com.example.milkdelivery.dto.MonthlySummaryDto;
import com.example.milkdelivery.entity.Delivery;
import com.example.milkdelivery.exception.ResourceNotFoundException;
import com.example.milkdelivery.repository.DeliveryRepository;
import com.example.milkdelivery.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Override
    public Delivery saveDelivery(Delivery delivery) {

        return deliveryRepository.save(delivery);
    }

    @Override
    public List<Delivery> getAllDeliveries() {

        return deliveryRepository.findAll();
    }

    @Override
    public Delivery getDeliveryById(Long id) {

        return deliveryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Delivery not found"
                        ));
    }

    @Override
    public List<DeliveryDto> getDeliveriesByCustomer(Long customerId) {

        List<Delivery> deliveries =
                deliveryRepository.findByCustomer_Id(customerId);

        if (deliveries.isEmpty()) {

            throw new ResourceNotFoundException(
                    "No deliveries found for this customer"
            );
        }

        return deliveries.stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public List<DeliveryDto> getTodayDeliveries() {

        List<Delivery> deliveries =
                deliveryRepository.findByDeliveryDate(
                        LocalDate.now()
                );

        if (deliveries.isEmpty()) {

            throw new ResourceNotFoundException(
                    "No deliveries found today"
            );
        }

        return deliveries.stream()
                .map(this::convertToDto)
                .toList();
    }

    @Override
    public List<DeliveryDto> getMonthlyDeliveries(
            Long customerId,
            int year,
            int month) {

        LocalDate startDate =
                LocalDate.of(year, month, 1);

        LocalDate endDate =
                startDate.withDayOfMonth(
                        startDate.lengthOfMonth()
                );

        List<Delivery> deliveries =
                deliveryRepository
                        .findByCustomer_IdAndDeliveryDateBetween(
                                customerId,
                                startDate,
                                endDate
                        );

        if (deliveries.isEmpty()) {

            throw new ResourceNotFoundException(
                    "No deliveries found for this month"
            );
        }

        return deliveries.stream()
                .map(this::convertToDto)
                .toList();
    }

    private DeliveryDto convertToDto(
            Delivery delivery) {

        return DeliveryDto.builder()

                .id(delivery.getId())

                .customerId(
                        delivery.getCustomer().getId()
                )

                .customerName(
                        delivery.getCustomer().getCustomerName()
                )

                .userId(
                        delivery.getUser().getId()
                )

                .milkmanName(
                        delivery.getUser().getFirstName()
                                + " "
                                + delivery.getUser().getLastName()
                )

                .deliveryDate(
                        delivery.getDeliveryDate()
                )

                .deliveryTime(
                        delivery.getDeliveryTime()
                )

                .milkQuantity(
                        delivery.getMilkQuantity()
                )

                .extraMilk(
                        delivery.getExtraMilk()
                )

                .totalMilk(
                        delivery.getTotalMilk()
                )

                .deliveryStatus(
                        delivery.getDeliveryStatus()
                )

                .createdAt(
                        delivery.getCreatedAt()
                )

                .build();
    }

    @Override
    public MonthlySummaryDto getMonthlySummary(
            Long customerId,
            int year,
            int month) {

        LocalDate startDate =
                LocalDate.of(year, month, 1);

        LocalDate endDate =
                startDate.withDayOfMonth(
                        startDate.lengthOfMonth()
                );

        List<Delivery> deliveries =
                deliveryRepository
                        .findByCustomer_IdAndDeliveryDateBetween(
                                customerId,
                                startDate,
                                endDate
                        );

        if (deliveries.isEmpty()) {

            throw new ResourceNotFoundException(
                    "No deliveries found for this month"
            );
        }

        Double totalMilk =
                deliveries.stream()
                        .mapToDouble(
                                Delivery::getTotalMilk
                        )
                        .sum();

        Double totalExtraMilk =
                deliveries.stream()
                        .mapToDouble(
                                Delivery::getExtraMilk
                        )
                        .sum();

        return MonthlySummaryDto.builder()
                .customerId(customerId)
                .customerName(
                        deliveries.get(0)
                                .getCustomer()
                                .getCustomerName()
                )
                .totalDeliveries(
                        deliveries.size()
                )
                .totalMilk(totalMilk)
                .totalExtraMilk(totalExtraMilk)
                .build();
    }
}