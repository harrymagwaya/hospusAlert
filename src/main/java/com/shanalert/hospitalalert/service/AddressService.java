package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.entity.Address;
import com.shanalert.hospitalalert.dto.AddressRequest;
import com.shanalert.hospitalalert.repository.AddressRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;

    @Transactional
    public Address saveAddress(AddressRequest dto) {
        Address address = new Address();
        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setCountry(dto.getCountry());

        // Set the coordinates
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());

        return addressRepository.save(address);
    }

    @Transactional(readOnly = true)
    public Address findById(UUID id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + id));
    }

    @Transactional
    public void deleteAddress(UUID id) {
        if (!addressRepository.existsById(id)) {
            throw new EntityNotFoundException("Cannot delete: Address not found");
        }
        addressRepository.deleteById(id);
    }

    // 5. GET ALL: (Optional but useful for System Admin)
    public List<Address> findAll() {
        return addressRepository.findAll();
    }

    @Transactional
    public Address updateAddress(UUID id, AddressRequest dto) {
        Address address = findById(id);

        if (dto.getStreet() != null) address.setStreet(dto.getStreet());
        if (dto.getCity() != null) address.setCity(dto.getCity());
        if (dto.getDistrict() != null) address.setDistrict(dto.getDistrict());
        if (dto.getCountry() != null) address.setCountry(dto.getCountry());
        if (dto.getLatitude() != null) address.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) address.setLongitude(dto.getLongitude());

        log.info("Patched address ID: {}", id);
        return addressRepository.save(address);
    }
}