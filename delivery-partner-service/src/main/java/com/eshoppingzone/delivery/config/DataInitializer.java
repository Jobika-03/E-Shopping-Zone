package com.eshoppingzone.delivery.config;

import com.eshoppingzone.delivery.entity.DeliveryPartner;
import com.eshoppingzone.delivery.repository.DeliveryPartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DeliveryPartnerRepository partnerRepository;

    @Override
    public void run(String... args) throws Exception {
        if (partnerRepository.count() == 0) {
            List<DeliveryPartner> defaultPartners = Arrays.asList(
                    new DeliveryPartner(0, "Alex Rivera (Express Bike)", "alex.rider@fastcourier.com", "+91 98765 11001", "partner123", "Bike", "TN-33-EQ-4521 (Express Bike)", "Central Metro", "AVAILABLE", 4.9, 128),
                    new DeliveryPartner(0, "BlueDart Express Logistics", "support@bluedart.delivery.com", "+91 98765 22002", "partner123", "Fleet Van", "TN-01-BD-2202 (Fleet Van)", "All Zones (National)", "AVAILABLE", 4.9, 340),
                    new DeliveryPartner(0, "Priya Sharma (City Fleet)", "priya.logistics@swiftfleet.com", "+91 98765 33003", "partner123", "Electric Van", "TN-33-EV-3003 (Electric Van)", "East & South Hubs", "AVAILABLE", 4.8, 95),
                    new DeliveryPartner(0, "ShadowFax Delivery Network", "dispatch@shadowfax.com", "+91 98765 44004", "partner123", "Hyperlocal Bike", "TN-38-SF-4404 (Hyperlocal Bike)", "West Zone", "AVAILABLE", 4.7, 210),
                    new DeliveryPartner(0, "Vikram Singh (Heavy Transit)", "vikram.cargo@transit.com", "+91 98765 55005", "partner123", "Cargo Truck", "TN-28-TR-5505 (Cargo Truck)", "North Region Interstate", "AVAILABLE", 4.8, 82)
            );
            partnerRepository.saveAll(defaultPartners);
            System.out.println(">>> Seeded " + defaultPartners.size() + " default Delivery Partners into delivery-partner-service");
        }
    }
}