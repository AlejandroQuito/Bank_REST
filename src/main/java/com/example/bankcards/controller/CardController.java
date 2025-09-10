package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransferDTO;
import com.example.bankcards.dto.responce.CardResponse;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Validated
@RequestMapping("/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Operations with cards")
public class CardController {

    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@Valid @RequestBody CardDTO cardDTO) {
        return cardService.createCard(cardDTO);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Page<CardResponse> read(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String owner,
            Principal principal) {

        Pageable pageable = PageRequest.of(page, size);
        return cardService.getCards(status, owner, pageable, principal.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public CardResponse getById(@PathVariable Long id,
                                Principal principal) {
        return cardService.getCardById(id, principal.getName());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CardResponse update(@PathVariable Long id,
                               @Valid @RequestBody CardDTO cardDTO) {
        return cardService.updateCard(id, cardDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cardService.deleteCard(id);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public void transfer(@Valid @RequestBody TransferDTO transferDTO,
                         Principal principal) {
        cardService.transfer(transferDTO, principal.getName());
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('USER')")
    public void requestBlock(@PathVariable Long id,
                             Principal principal) {
        cardService.requestBlock(id, principal.getName());
    }

    @PostMapping("/{id}/block-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public void blockByAdmin(@PathVariable Long id) {
        cardService.blockCardAdmin(id);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public void activateByAdmin(@PathVariable Long id) {
        cardService.activateCardAdmin(id);
    }
}
