package com.cepheus.sovcombank.deal.service;

import com.cepheus.sovcombank.account.model.Account;
import com.cepheus.sovcombank.account.repository.AccountRepository;
import com.cepheus.sovcombank.deal.dto.BalanceChangerDto;
import com.cepheus.sovcombank.deal.dto.ForDealDto;
import com.cepheus.sovcombank.deal.model.Deal;
import com.cepheus.sovcombank.deal.repository.DealRepository;
import com.cepheus.sovcombank.exception.BalanceException;
import com.cepheus.sovcombank.exception.NotFoundException;
import com.cepheus.sovcombank.exception.UserIsNotOwnerException;
import com.cepheus.sovcombank.account.model.Currency;
import com.cepheus.sovcombank.user.model.User;
import com.cepheus.sovcombank.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static java.lang.Math.abs;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DealServiceImpl implements DealService{
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final DealRepository dealRepository;

    @Transactional
    @Override
    public Deal make(ForDealDto forDealDto, String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not find"));
        Account account = accountRepository.findById(forDealDto.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account for buy not find"));
        Account accountRu = accountRepository.findByUserAndCurrency(user, Currency.RUB)
                .orElseThrow(() -> new NotFoundException("Счёт с валютой " + Currency.RUB + " Не найден"));
        if(!user.getId().equals(account.getId())){
            throw new UserIsNotOwnerException("User is not owner");
        }
        if(forDealDto.getSum() > 0) {
            if(accountRu.getBalance() - forDealDto.getSum() < 0 ){
                throw new BalanceException("You can't take off more than you have");
            }
            accountRu.setBalance(accountRu.getBalance() - forDealDto.getSum());
            account.setBalance(account.getBalance() + (account.getBalance() * forDealDto.getCurrencyRatio()));
        } else {
            if(account.getBalance() - (forDealDto.getSum() / forDealDto.getCurrencyRatio()) > 0){
                throw new BalanceException("You can't take off more than you have");
            }
            accountRu.setBalance(account.getBalance() - (forDealDto.getSum() / forDealDto.getCurrencyRatio()));
            account.setBalance(accountRu.getBalance() + (account.getBalance()));
        }
        Deal deal = Deal.builder()
                .timeStamp(LocalDateTime.now())
                .summary(-1 * forDealDto.getSum())
                .account(accountRu)
                .build();
        dealRepository.save(deal);
        accountRepository.save(account);
        accountRepository.save(accountRu);
        log.info("Покупка валюты {}, пользователем {}, на сумму {}. Прошла успешно",
                account.getCurrency(), user.getEmail(), forDealDto.getSum());
        return deal;
    }

    @Transactional
    @Override
    public Deal changeBalance(BalanceChangerDto balanceChangerDto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь " + email + " не найден"));
        Account account = accountRepository.findByUserAndCurrency(user,Currency.RUB)
                .orElseThrow(() -> new NotFoundException("Счёт с валютой " + Currency.RUB + " Не найден"));
        account.setBalance(account.getBalance() + balanceChangerDto.getSum());
        Deal deal = Deal.builder()
                .summary(balanceChangerDto.getSum())
                .timeStamp(LocalDateTime.now())
                .build();
        accountRepository.save(account);
        log.info("На баланс {} успешно пришло {}", user.getEmail(), balanceChangerDto.getSum());
        return dealRepository.save(deal);
    }
}