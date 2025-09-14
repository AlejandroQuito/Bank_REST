package com.example.bankcards.util.mapper;

import com.example.bankcards.dto.responce.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.util.CardEncryptor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = CardEncryptor.class)
public interface CardMapper {

    @Mapping(source = "number", target = "cardNumber")
    @Mapping(source = "owner", target = "owner", qualifiedByName = "mapOwner")
    @Mapping(source = "expiration", target = "expiryDate")
    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatus")
    CardResponse toResponse(Card card);

    @Mapping(source = "number", target = "cardNumber", qualifiedByName = "maskCardNumber")
    @Mapping(source = "owner", target = "owner", qualifiedByName = "mapOwner")
    @Mapping(source = "expiration", target = "expiryDate")
    @Mapping(source = "status", target = "status", qualifiedByName = "mapStatus")
    CardResponse toResponseWithMask(Card card);

    @Named("mapOwner")
    default String mapOwner(User user) {
        return user != null ? user.getUsername() : null;
    }

    @Named("mapStatus")
    default String mapStatus(CardStatus status) {
        return status != null ? status.getName() : null;
    }
}
