package com.example.bankcards.util.validator;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.StatusCardException;
import com.example.bankcards.repository.CardStatusRepository;
import com.example.bankcards.util.properties.CardProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
@RequiredArgsConstructor

public class CardServiceValidator {

    private final CardStatusRepository cardStatusRepository;
    private final CardProperties cardStatus;

    public void validateCardOwnership(Card fromCard, Card toCard, User currentUser) {
        if (!fromCard.getOwner().getId().equals(currentUser.getId()) ||
                !toCard.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("Access denied: Cards must belong to user");
        }
    }

    public void validateCardOwnership(Card card, User currentUser) {
        if (!card.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("Access denied: Card does not belong to user");
        }
    }

    public void validateCardIsActive(Card card) {
        CardStatus activeStatus = requireStatus(cardStatus.getStatus().getActive());
        if (!card.getStatus().equals(activeStatus)) {
            throw new CardNotFoundException("Card is already blocked or expired");
        }
    }

    public void validateCardStatus(Card fromCard, Card toCard) {
        CardStatus activeStatus = requireStatus(cardStatus.getStatus().getActive());
        if (!fromCard.getStatus().equals(activeStatus) || !toCard.getStatus().equals(activeStatus)) {
            throw new CardException("Both cards must be active for transfer");
        }
    }

    public void validateSufficientBalance(Card fromCard, BigDecimal amount) {
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new CardException("Insufficient balance on source card");
        }
    }

    public void validateCardAccess(Card card, User currentUser) {
        if (!hasAccessToCard(card, currentUser)) {
            throw new SecurityException("Access denied: Card does not belong to user");
        }
    }

    public boolean hasAccessToCard(Card card, User currentUser) {
        return currentUser.getRole() == Role.ADMIN ||
                card.getOwner().getId().equals(currentUser.getId());
    }

    public CardStatus requireStatus(String statusName) {
        return cardStatusRepository.findByName(statusName)
                .orElseThrow(() -> new StatusCardException("Status not found: " + statusName));
    }

    public CardStatus determineCardStatus(Date expirationDate) {
        return isCardExpired(expirationDate)
                ? requireStatus(cardStatus.getStatus().getExpired())
                : requireStatus(cardStatus.getStatus().getActive());
    }

    public boolean isCardExpired(Date expirationDate) {
        return expirationDate.before(new Date());
    }
}
