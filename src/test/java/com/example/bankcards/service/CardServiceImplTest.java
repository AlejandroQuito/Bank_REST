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
import com.example.bankcards.util.mapper.CardMapper;
import com.example.bankcards.util.properties.CardProperties;
import com.example.bankcards.util.validator.CardServiceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardProperties cardProperties;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserService userService;

    @Mock
    private CardServiceValidator validator;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private CardEncryptor cardEncryptor;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardServiceImpl cardService;

    private static final Long CARD_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";
    private static final String CARD_NUMBER = "1234567812345678";
    private static final String ENCRYPTED_NUMBER = "encrypted12345678";
    private static final BigDecimal BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("100.00");

    private Card testCard;
    private User testUser;
    private CardStatus activeStatus;
    private CardStatus blockedStatus;
    private CardStatus expiredStatus;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUsername(USERNAME);
        testUser.setRole(Role.USER);

        activeStatus = new CardStatus();
        activeStatus.setName("ACTIVE");

        blockedStatus = new CardStatus();
        blockedStatus.setName("BLOCKED");

        expiredStatus = new CardStatus();
        expiredStatus.setName("EXPIRED");

        testCard = new Card();
        testCard.setId(CARD_ID);
        testCard.setNumber(ENCRYPTED_NUMBER);
        testCard.setOwner(testUser);
        testCard.setBalance(BALANCE);
        testCard.setStatus(activeStatus);
        testCard.setExpiration(new Date(System.currentTimeMillis() + 86400000));

        pageable = Pageable.ofSize(10).withPage(0);

        CardProperties.Status statusProperties = mock(CardProperties.Status.class);
        lenient().when(statusProperties.getActive()).thenReturn("ACTIVE");
        lenient().when(statusProperties.getBlocked()).thenReturn("BLOCKED");
        lenient().when(statusProperties.getExpired()).thenReturn("EXPIRED");

        lenient().when(cardProperties.getStatus()).thenReturn(statusProperties);
    }

    @Test
    void createCard_ShouldCreateCard_WhenValidData() {
        CardDTO cardDTO = new CardDTO(CARD_NUMBER, USER_ID, new Date(), BALANCE);
        CardResponse cardResponse = new CardResponse(
                CARD_ID, "1234********5678", USERNAME, new Date(), "ACTIVE", BALANCE);

        when(cardEncryptor.encrypt(CARD_NUMBER)).thenReturn(ENCRYPTED_NUMBER);
        when(userService.requireUserById(USER_ID)).thenReturn(testUser);
        when(validator.determineCardStatus(any())).thenReturn(activeStatus);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardMapper.toResponseWithMask(testCard)).thenReturn(cardResponse);

        CardResponse response = cardService.createCard(cardDTO);

        assertNotNull(response);
        verify(cardEncryptor).encrypt(CARD_NUMBER);
        verify(userService).requireUserById(USER_ID);
        verify(validator).determineCardStatus(any());
        verify(cardRepository).save(any(Card.class));
        verify(cardMapper).toResponseWithMask(testCard);
    }

    @Test
    void getCards_ShouldReturnAllCardsForAdmin_WhenNoFilters() {
        testUser.setRole(Role.ADMIN);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));
        CardResponse cardResponse = new CardResponse(
                CARD_ID, "1234********5678", USERNAME, new Date(), "ACTIVE", BALANCE);

        when(userService.requireUserByUsername(USERNAME)).thenReturn(testUser);
        when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(cardPage);
        when(cardMapper.toResponseWithMask(testCard)).thenReturn(cardResponse);

        Page<CardResponse> result = cardService.getCards(null, null, pageable, USERNAME);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userService).requireUserByUsername(USERNAME);
        verify(cardRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getCards_ShouldReturnFilteredCardsForUser() {
        Page<Card> cardPage = new PageImpl<>(List.of(testCard));
        CardResponse cardResponse = new CardResponse(
                CARD_ID, "1234********5678", USERNAME, new Date(), "ACTIVE", BALANCE);

        when(userService.requireUserByUsername(USERNAME)).thenReturn(testUser);
        when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(cardPage);
        when(cardMapper.toResponseWithMask(testCard)).thenReturn(cardResponse);

        Page<CardResponse> result = cardService.getCards("ACTIVE", null, pageable, USERNAME);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userService).requireUserByUsername(USERNAME);
        verify(cardRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getCardById_ShouldReturnCard_WhenUserHasAccess() {
        CardResponse cardResponse = new CardResponse(
                CARD_ID, "1234********5678", USERNAME, new Date(), "ACTIVE", BALANCE);

        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
        when(userService.requireUserByUsername(USERNAME)).thenReturn(testUser);
        doNothing().when(validator).validateCardAccess(testCard, testUser);
        when(cardMapper.toResponseWithMask(testCard)).thenReturn(cardResponse);

        CardResponse response = cardService.getCardById(CARD_ID, USERNAME);

        assertNotNull(response);
        verify(cardRepository).findById(CARD_ID);
        verify(userService).requireUserByUsername(USERNAME);
        verify(validator).validateCardAccess(testCard, testUser);
        verify(cardMapper).toResponseWithMask(testCard);
    }

    @Test
    void getCardById_ShouldThrowException_WhenCardNotFound() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.getCardById(CARD_ID, USERNAME));
        verify(cardRepository).findById(CARD_ID);
        verify(userService, never()).requireUserByUsername(anyString());
    }

    @Test
    void updateCard_ShouldUpdateCard_WhenValidData() {
        CardDTO cardDTO = new CardDTO("8765432187654321", USER_ID, new Date(), new BigDecimal("2000.00"));
        CardResponse cardResponse = new CardResponse(
                CARD_ID, "1234********5678", USERNAME, new Date(), "ACTIVE", new BigDecimal("2000.00"));

        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
        when(cardEncryptor.encrypt("8765432187654321")).thenReturn("encrypted87654321");
        when(userService.requireUserById(USER_ID)).thenReturn(testUser);
        when(cardRepository.save(testCard)).thenReturn(testCard);
        when(cardMapper.toResponse(testCard)).thenReturn(cardResponse);

        CardResponse response = cardService.updateCard(CARD_ID, cardDTO);

        assertNotNull(response);
        verify(cardRepository).findById(CARD_ID);
        verify(cardEncryptor).encrypt("8765432187654321");
        verify(userService).requireUserById(USER_ID);
        verify(cardRepository).save(testCard);
        verify(cardMapper).toResponse(testCard);
    }

    @Test
    void deleteCard_ShouldDeleteCard_WhenCardExists() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
        doNothing().when(cardRepository).delete(testCard);

        cardService.deleteCard(CARD_ID);

        verify(cardRepository).findById(CARD_ID);
        verify(cardRepository).delete(testCard);
    }

    @Test
    void deleteCard_ShouldThrowException_WhenCardNotFound() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.deleteCard(CARD_ID));
        verify(cardRepository).findById(CARD_ID);
        verify(cardRepository, never()).delete((Card) any());
    }

    @Test
    void transfer_ShouldTransferMoney_WhenValidTransfer() {
        TransferDTO transferDTO = new TransferDTO(CARD_ID, 2L, TRANSFER_AMOUNT);
        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setOwner(testUser);
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setStatus(activeStatus);

        when(userService.requireUserByUsername(USERNAME)).thenReturn(testUser);
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));
        doNothing().when(validator).validateCardOwnership(testCard, toCard, testUser);
        doNothing().when(validator).validateCardStatus(testCard, toCard);
        doNothing().when(validator).validateSufficientBalance(testCard, TRANSFER_AMOUNT);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transferRepository.save(any(Transfer.class))).thenReturn(new Transfer());

        cardService.transfer(transferDTO, USERNAME);

        assertEquals(new BigDecimal("900.00"), testCard.getBalance());
        assertEquals(new BigDecimal("600.00"), toCard.getBalance());
        verify(userService).requireUserByUsername(USERNAME);
        verify(cardRepository, times(2)).findById(any());
        verify(validator).validateCardOwnership(testCard, toCard, testUser);
        verify(validator).validateCardStatus(testCard, toCard);
        verify(validator).validateSufficientBalance(testCard, TRANSFER_AMOUNT);
        verify(cardRepository, times(2)).save(any(Card.class));
        verify(transferRepository).save(any(Transfer.class));
    }

    @Test
    void transfer_ShouldThrowException_WhenFromCardNotFound() {
        TransferDTO transferDTO = new TransferDTO(CARD_ID, 2L, TRANSFER_AMOUNT);
        when(userService.requireUserByUsername(USERNAME)).thenReturn(testUser);
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.transfer(transferDTO, USERNAME));
        verify(userService).requireUserByUsername(USERNAME);
        verify(cardRepository).findById(CARD_ID);
        verify(cardRepository, never()).findById(2L);
        verify(validator, never()).validateCardOwnership(any(), any(), any());
    }

    @Test
    void requestBlock_ShouldBlockCard_WhenUserIsOwnerAndCardActive() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
        when(userService.requireUserByUsername(USERNAME)).thenReturn(testUser);
        doNothing().when(validator).validateCardOwnership(testCard, testUser);
        doNothing().when(validator).validateCardIsActive(testCard);
        when(validator.requireStatus("BLOCKED")).thenReturn(blockedStatus);
        when(cardRepository.save(testCard)).thenReturn(testCard);

        cardService.requestBlock(CARD_ID, USERNAME);

        assertEquals(blockedStatus, testCard.getStatus());
        verify(cardRepository).findById(CARD_ID);
        verify(userService).requireUserByUsername(USERNAME);
        verify(validator).validateCardOwnership(testCard, testUser);
        verify(validator).validateCardIsActive(testCard);
        verify(validator).requireStatus("BLOCKED");
        verify(cardRepository).save(testCard);
    }

    @Test
    void blockCardAdmin_ShouldBlockCard() {
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
        when(validator.requireStatus("BLOCKED")).thenReturn(blockedStatus);
        when(cardRepository.save(testCard)).thenReturn(testCard);

        cardService.blockCardAdmin(CARD_ID);

        assertEquals(blockedStatus, testCard.getStatus());
        verify(cardRepository).findById(CARD_ID);
        verify(validator).requireStatus("BLOCKED");
        verify(cardRepository).save(testCard);
    }

    @Test
    void activateCardAdmin_ShouldActivateCard() {
        testCard.setStatus(blockedStatus);
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
        when(validator.requireStatus("ACTIVE")).thenReturn(activeStatus);
        when(cardRepository.save(testCard)).thenReturn(testCard);

        cardService.activateCardAdmin(CARD_ID);

        assertEquals(activeStatus, testCard.getStatus());
        verify(cardRepository).findById(CARD_ID);
        verify(validator).requireStatus("ACTIVE");
        verify(cardRepository).save(testCard);
    }

    @Test
    void transfer_ShouldThrowException_WhenInsufficientBalance() {
        TransferDTO transferDTO = new TransferDTO(CARD_ID, 2L, new BigDecimal("2000.00"));
        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setOwner(testUser);
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setStatus(activeStatus);

        when(userService.requireUserByUsername(USERNAME)).thenReturn(testUser);
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));
        doNothing().when(validator).validateCardOwnership(testCard, toCard, testUser);
        doNothing().when(validator).validateCardStatus(testCard, toCard);
        doThrow(new RuntimeException("Insufficient balance"))
                .when(validator).validateSufficientBalance(testCard, new BigDecimal("2000.00"));

        assertThrows(RuntimeException.class, () -> cardService.transfer(transferDTO, USERNAME));
        verify(validator).validateSufficientBalance(testCard, new BigDecimal("2000.00"));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void requestBlock_ShouldThrowException_WhenCardNotActive() {
        testCard.setStatus(blockedStatus);
        when(cardRepository.findById(CARD_ID)).thenReturn(Optional.of(testCard));
        when(userService.requireUserByUsername(USERNAME)).thenReturn(testUser);
        doNothing().when(validator).validateCardOwnership(testCard, testUser);
        doThrow(new RuntimeException("Card is not active"))
                .when(validator).validateCardIsActive(testCard);

        assertThrows(RuntimeException.class, () -> cardService.requestBlock(CARD_ID, USERNAME));
        verify(validator).validateCardIsActive(testCard);
        verify(cardRepository, never()).save(any(Card.class));
    }
}
