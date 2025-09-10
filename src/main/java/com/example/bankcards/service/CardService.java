package com.example.bankcards.service;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransferDTO;
import com.example.bankcards.dto.responce.CardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CardService {

    CardResponse createCard(CardDTO cardDTO);

    Page<CardResponse> getCards(String status, String owner, Pageable pageable, String username);

    CardResponse getCardById(Long id, String username);

    CardResponse updateCard(Long id, CardDTO cardDTO);

    void deleteCard(Long id);

    void transfer(TransferDTO transferDTO, String username);

    void requestBlock(Long id, String username);

    void blockCardAdmin(Long id);

    void activateCardAdmin(Long id);
}
