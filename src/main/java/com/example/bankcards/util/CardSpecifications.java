package com.example.bankcards.util;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class CardSpecifications {

    public static Specification<Card> hasStatus(CardStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Card> hasOwner(User owner) {
        return (root, query, cb) -> owner == null ? null : cb.equal(root.get("owner"), owner);
    }
}
