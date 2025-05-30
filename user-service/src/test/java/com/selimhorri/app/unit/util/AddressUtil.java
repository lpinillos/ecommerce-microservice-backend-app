package com.selimhorri.app.unit.util;

import com.selimhorri.app.dto.AddressDto;
import java.util.Set;
import java.util.HashSet;

public class AddressUtil {

    public static AddressDto getSampleAddressDto() {
        return AddressDto.builder()
                .addressId(1)
                .fullAddress("123 Main Street, Apt 4B")
                .postalCode("12345")
                .city("New York")
                .build();
    }

    public static Set<AddressDto> getSampleAddressDtoSet() {
        Set<AddressDto> addresses = new HashSet<>();
        addresses.add(getSampleAddressDto());
        addresses.add(AddressDto.builder()
                .addressId(2)
                .fullAddress("456 Oak Avenue")
                .postalCode("67890")
                .city("Los Angeles")
                .build());
        return addresses;
    }
}
