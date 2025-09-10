package com.example.bankcards.repository;

import com.example.bankcards.entity.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardStatusRepository  extends JpaRepository<CardStatus, Long> {

    Optional<CardStatus> findByName(String name);
}
