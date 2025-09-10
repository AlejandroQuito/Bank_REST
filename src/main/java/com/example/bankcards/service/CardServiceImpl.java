package com.example.bankcards.service;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransferDTO;
import com.example.bankcards.dto.responce.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.util.CardEncryptor;
import com.example.bankcards.util.CardSpecifications;
import com.example.bankcards.util.mapper.CardMapper;
import com.example.bankcards.util.properties.CardProperties;
import com.example.bankcards.util.validator.CardServiceValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardProperties cardStatus;
    private final CardRepository cardRepository;
    private final UserService userService;
    private final CardServiceValidator validator;
    private final TransferRepository transferRepository;
    private final CardEncryptor cardEncryptor;
    private final CardMapper cardMapper;

    @Transactional
    public CardResponse createCard(@Valid CardDTO cardDTO) {
        log.info("Creating card for ownerId: {}", cardDTO.ownerId());

        Card card = Card.builder()
                .number(cardEncryptor.encrypt(cardDTO.number()))
                .owner(userService.requireUserById(cardDTO.ownerId()))
                .expiration(cardDTO.expiration())
                .balance(cardDTO.balance())
                .build();

        card.setStatus(validator.determineCardStatus(cardDTO.expiration()));
        Card savedCard = cardRepository.save(card);

        return cardMapper.toResponseWithMask(savedCard);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> getCards(String status,
                                       String owner,
                                       Pageable pageable,
                                       String username) {
        log.info("Fetching cards for user: {}, status: {}, owner: {}", username, status, owner);

        User currentUser = userService.requireUserByUsername(username);
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        CardStatus filterStatus = status != null ? validator.requireStatus(status) : null;
        User filterOwner = null;

        if (isAdmin && owner != null) {
            filterOwner = userService.requireUserByUsername(owner);
        } else if (!isAdmin) {
            filterOwner = currentUser;
        }

        Specification<Card> spec = Specification.allOf(
                CardSpecifications.hasStatus(filterStatus),
                CardSpecifications.hasOwner(filterOwner)
        );

        return cardRepository.findAll(spec, pageable)
                .map(cardMapper::toResponseWithMask);
    }

    @Transactional(readOnly = true)
    public CardResponse getCardById(Long id, String username) {
        Card card = requireCardById(id);
        User currentUser = userService.requireUserByUsername(username);

        validator.validateCardAccess(card, currentUser);

        return cardMapper.toResponseWithMask(card);
    }

    @Transactional
    public CardResponse updateCard(Long id, @Valid CardDTO cardDTO) {
        Card card = requireCardById(id);

        card.setNumber(cardEncryptor.encrypt(cardDTO.number()));
        card.setOwner(userService.requireUserById(cardDTO.ownerId()));
        card.setExpiration(cardDTO.expiration());
        card.setBalance(cardDTO.balance());

        updateCardStatusBasedOnExpiration(card, cardDTO.expiration());

        return cardMapper.toResponse(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(Long id) {
        cardRepository.delete(requireCardById(id));
    }

    @Transactional
    public void transfer(@Valid TransferDTO transferDTO, String username) {
        User currentUser = userService.requireUserByUsername(username);

        Card fromCard = requireCardById(transferDTO.fromCardId());
        Card toCard = requireCardById(transferDTO.toCardId());

        validator.validateCardOwnership(fromCard, toCard, currentUser);
        validator.validateCardStatus(fromCard, toCard);
        validator.validateSufficientBalance(fromCard, transferDTO.amount());

        performTransfer(fromCard, toCard, transferDTO.amount());
        createTransferRecord(fromCard, toCard, transferDTO.amount());
    }

    @Transactional
    public void requestBlock(Long id, String username) {
        Card card = requireCardById(id);
        User currentUser = userService.requireUserByUsername(username);

        validator.validateCardOwnership(card, currentUser);
        validator.validateCardIsActive(card);

        card.setStatus(validator.requireStatus(cardStatus.getStatus().getBlocked()));
        cardRepository.save(card);
    }

    @Transactional
    public void blockCardAdmin(Long id) {
        Card card = requireCardById(id);
        card.setStatus(validator.requireStatus(cardStatus.getStatus().getBlocked()));

        cardRepository.save(card);
    }

    @Transactional
    public void activateCardAdmin(Long id) {
        Card card = requireCardById(id);
        card.setStatus(validator.requireStatus(cardStatus.getStatus().getActive()));

        cardRepository.save(card);
    }

    private void performTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);
    }

    private void updateCardStatusBasedOnExpiration(Card card, Date expirationDate) {
        if (validator.isCardExpired(expirationDate)) {
            card.setStatus(validator.requireStatus(cardStatus.getStatus().getExpired()));
        }
    }

    private void createTransferRecord(Card fromCard, Card toCard, BigDecimal amount) {
        Transfer transfer = Transfer.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(amount)
                .build();

        transferRepository.save(transfer);
    }

    private Card requireCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardId));
    }
}
