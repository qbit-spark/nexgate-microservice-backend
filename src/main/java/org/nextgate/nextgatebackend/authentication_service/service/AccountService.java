package org.nextgate.nextgatebackend.authentication_service.service;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.payloads.AccountLoginRequest;
import org.nextgate.nextgatebackend.authentication_service.payloads.CreateAccountRequest;
import org.nextgate.nextgatebackend.authentication_service.payloads.RefreshTokenResponse;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.TokenInvalidException;

import java.util.List;
import java.util.UUID;

public interface AccountService {

    String registerAccount(CreateAccountRequest createAccountRequest) throws Exception;

    String loginAccount(AccountLoginRequest accountLoginRequest) throws Exception;

    RefreshTokenResponse refreshToken(String refreshToken) throws TokenInvalidException;

    List<AccountEntity> getAllAccounts();

    AccountEntity getAccountByID(UUID uuid) throws ItemNotFoundException;

}
