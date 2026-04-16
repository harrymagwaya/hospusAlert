package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.AddressRequest;
import com.shanalert.hospitalalert.entity.Address;
import com.shanalert.hospitalalert.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public Address createAddress(@Valid @RequestBody AddressRequest request) {
        return addressService.saveAddress(request);
    }

    @GetMapping("/{id}")
    public Address getAddress(@PathVariable UUID id) {
        return addressService.findById(id);
    }

    @PatchMapping("/{id}")
    public Address updateAddress(@PathVariable UUID id, @RequestBody AddressRequest request) {
        return addressService.updateAddress(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteAddress(@PathVariable UUID id) {
        addressService.deleteAddress(id);
    }

    @GetMapping
    public List<Address> getAllAddresses() {
        return addressService.findAll();
    }
}