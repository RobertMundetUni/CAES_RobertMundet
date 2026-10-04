package org.udg.caes.banking.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.udg.caes.banking.entity.Account;
import org.udg.caes.banking.entity.Client;
import org.udg.caes.banking.entity.CreditCard;
import org.udg.caes.banking.exceptions.ClientNotFound;
import org.udg.caes.banking.exceptions.EntityNotFound;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import org.udg.caes.banking.exceptions.AccountActive;
import org.udg.caes.banking.exceptions.NotEnoughBalance;
import org.udg.caes.banking.exceptions.PersistenceException;
import org.udg.caes.banking.manager.EntityManager;

@ExtendWith(MockitoExtension.class)
public class TestClientService {

  @Mock
  private EntityManager em;

  @Mock
  private AccountService as;

  @InjectMocks
  private ClientService service;

  @Test
  void testGetClientReturnsClient() throws ClientNotFound, EntityNotFound {
    Client client = new Client("client-1");
    when(em.get("client-1", Client.class)).thenReturn(client);

    Client result = service.getClient("client-1");

    assertEquals(client, result);
  }

  @Test
  void testGetClientThrowsClientNotFoundWhenMissing() throws EntityNotFound {
    when(em.get("missing", Client.class)).thenThrow(new EntityNotFound());

    assertThrows(ClientNotFound.class, () -> service.getClient("missing"));
  }

  @Test
  void testDeleteCallsDeleteOnAllClientAccounts() throws Exception {
    Client client = new Client("client-1");
    Account account1 = new Account("acc-1", 0L);
    Account account2 = new Account("acc-2", 0L);
    when(em.get("client-1", Client.class)).thenReturn(client);
    when(em.getClientAccounts(client)).thenReturn(List.of(account1, account2));

    service.delete("client-1");

    verify(as).delete(account1);
    verify(as).delete(account2);
  }

  @Test
  void testGetBalanceSumsAccountBalances() {
    Client client = new Client("client-1");
    Account acc1 = new Account("acc-1", 100L);
    Account acc2 = new Account("acc-2", 60L);
    when(em.getClientAccounts(client)).thenReturn(List.of(acc1, acc2));

    long balance = service.getBalance(client);

    assertEquals(160L, balance);
  }

  @Test
  void testGetRealBalanceSubtractsCreditCardDebt() {
    Client client = new Client("client-1");
    Account acc1 = new Account("acc-1", 100L);
    Account acc2 = new Account("acc-2", 50L);
    CreditCard creditCard = new CreditCard("cc-1");
    creditCard.credit(30L);
    when(em.getClientAccounts(client)).thenReturn(List.of(acc1, acc2));
    when(em.getCreditCards(acc1)).thenReturn(List.of(creditCard));
    when(em.getCreditCards(acc2)).thenReturn(List.of());

    long balance = service.getRealBalance(client);

    assertEquals(120L, balance);
  }

  @Test
  void testDeleteMissingClientDoesNotDeleteAccounts() throws Exception {
    when(em.get("missing", Client.class)).thenThrow(new EntityNotFound());

    assertThrows(ClientNotFound.class, () -> service.delete("missing"));

    verify(as, never()).delete(any(Account.class));
  }

  @Test
  void testDeletePropagatesAccountActiveAndStopsDeletingAccounts() throws Exception {
    Client client = new Client("client-1");
    Account first = new Account("acc-1", 0L);
    Account second = new Account("acc-2", 0L);
    when(em.get("client-1", Client.class)).thenReturn(client);
    when(em.getClientAccounts(client)).thenReturn(List.of(first, second));
    AccountActive failure = new AccountActive();
    doThrow(failure).when(as).delete(first);

    AccountActive thrown = assertThrows(AccountActive.class, () -> service.delete("client-1"));

    assertSame(failure, thrown);
    verify(as, never()).delete(second);
  }

  @Test
  void testDeletePropagatesNotEnoughBalanceAndStopsDeletingAccounts() throws Exception {
    Client client = new Client("client-1");
    Account first = new Account("acc-1", 0L);
    Account second = new Account("acc-2", 0L);
    when(em.get("client-1", Client.class)).thenReturn(client);
    when(em.getClientAccounts(client)).thenReturn(List.of(first, second));
    NotEnoughBalance failure = new NotEnoughBalance();
    doThrow(failure).when(as).delete(first);

    NotEnoughBalance thrown = assertThrows(NotEnoughBalance.class, () -> service.delete("client-1"));

    assertSame(failure, thrown);
    verify(as, never()).delete(second);
  }

  @Test
  void testDeletePropagatesPersistenceExceptionAndStopsDeletingAccounts() throws Exception {
    Client client = new Client("client-1");
    Account first = new Account("acc-1", 0L);
    Account second = new Account("acc-2", 0L);
    when(em.get("client-1", Client.class)).thenReturn(client);
    when(em.getClientAccounts(client)).thenReturn(List.of(first, second));
    PersistenceException failure = new PersistenceException();
    doThrow(failure).when(as).delete(first);

    PersistenceException thrown = assertThrows(PersistenceException.class, () -> service.delete("client-1"));

    assertSame(failure, thrown);
    verify(as, never()).delete(second);
  }

}
