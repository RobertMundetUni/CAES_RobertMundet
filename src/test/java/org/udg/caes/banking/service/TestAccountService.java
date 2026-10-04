package org.udg.caes.banking.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.udg.caes.banking.entity.Account;
import org.udg.caes.banking.entity.CreditCard;
import org.udg.caes.banking.exceptions.AccountActive;
import org.udg.caes.banking.exceptions.AccountNotFound;
import org.udg.caes.banking.exceptions.EntityNotFound;
import org.udg.caes.banking.exceptions.NotEnoughBalance;
import org.udg.caes.banking.exceptions.PersistenceException;
import org.udg.caes.banking.manager.EntityManager;

@ExtendWith(MockitoExtension.class)
public class TestAccountService {

  @Mock
  private EntityManager em;

  @Mock
  private CreditCardService ccs;

  @InjectMocks
  private AccountService service;

  @Test
  void testGetAccountReturnsAccount() throws AccountNotFound, EntityNotFound {
    Account account = new Account("acc-1", 300L);
    when(em.get("acc-1", Account.class)).thenReturn(account);

    Account result = service.getAccount("acc-1");

    assertEquals(account, result);
  }

  @Test
  void testGetAccountThrowsAccountNotFoundWhenEntityMissing() throws EntityNotFound {
    when(em.get("missing", Account.class)).thenThrow(new EntityNotFound());

    assertThrows(AccountNotFound.class, () -> service.getAccount("missing"));
  }

  @Test
  void testTransferMovesBalanceBetweenAccounts() throws Exception {
    Account from = new Account("from", 100L);
    Account to = new Account("to", 40L);
    when(em.get("from", Account.class)).thenReturn(from);
    when(em.get("to", Account.class)).thenReturn(to);

    service.transfer("from", "to", 30L);

    assertEquals(70L, from.getBalance());
    assertEquals(70L, to.getBalance());
    verify(em).persist(from);
    verify(em).persist(to);
  }

  @Test
  void testTransferWithInsufficientFundsThrowsNotEnoughBalance() throws Exception {
    Account from = new Account("from", 20L);
    Account to = new Account("to", 50L);
    when(em.get("from", Account.class)).thenReturn(from);
    when(em.get("to", Account.class)).thenReturn(to);

    assertThrows(NotEnoughBalance.class, () -> service.transfer("from", "to", 30L));
    assertEquals(20L, from.getBalance());
    assertEquals(50L, to.getBalance());
    verify(em, never()).persist(any());
  }

  @Test
  void testDeleteByIdRemovesAccountWithoutCards() throws Exception {
    Account account = new Account("acc-1", 0L);
    when(em.get("acc-1", Account.class)).thenReturn(account);
    when(em.getCreditCards(account)).thenReturn(List.of());

    service.delete("acc-1");

    verify(em).delete(account);
  }

  @Test
  void testDeleteByIdThrowsAccountActiveWhenAccountHasPositiveBalance() throws Exception {
    Account account = new Account("acc-1", 10L);
    when(em.get("acc-1", Account.class)).thenReturn(account);

    assertThrows(AccountActive.class, () -> service.delete("acc-1"));
    verify(em, never()).delete(any());
  }

  @Test
  void testDeleteByIdDeletesAssociatedZeroBalanceCard() throws Exception {
    Account account = new Account("acc-1", 0L);
    CreditCard creditCard = new CreditCard("cc-1");
    when(em.get("acc-1", Account.class)).thenReturn(account);
    when(em.getCreditCards(account)).thenReturn(List.of(creditCard));

    service.delete("acc-1");

    verify(ccs).delete(creditCard);
    verify(em).delete(account);
  }

  @Test
  void testDeleteByIdThrowsAccountActiveWhenCardStillHasCredit() throws Exception {
    Account account = new Account("acc-1", 0L);
    CreditCard creditCard = new CreditCard("cc-1");
    creditCard.credit(5L);
    when(em.get("acc-1", Account.class)).thenReturn(account);
    when(em.getCreditCards(account)).thenReturn(List.of(creditCard));

    assertThrows(AccountActive.class, () -> service.delete("acc-1"));
    verify(ccs, never()).delete(any());
  }

  @Test
  void testTransferWithMissingSenderStopsBeforeLoadingRecipient() throws Exception {
    when(em.get("missing", Account.class)).thenThrow(new EntityNotFound());

    assertThrows(AccountNotFound.class, () -> service.transfer("missing", "to", 30L));

    verify(em, never()).get("to", Account.class);
    verify(em, never()).persist(any());
  }

  @Test
  void testTransferWithMissingRecipientKeepsSenderBalance() throws Exception {
    Account from = new Account("from", 100L);
    when(em.get("from", Account.class)).thenReturn(from);
    when(em.get("missing", Account.class)).thenThrow(new EntityNotFound());

    assertThrows(AccountNotFound.class, () -> service.transfer("from", "missing", 30L));

    assertEquals(100L, from.getBalance());
    verify(em, never()).persist(any());
  }

  @Test
  void testDeleteMissingAccountDoesNotDeleteAnything() throws Exception {
    when(em.get("missing", Account.class)).thenThrow(new EntityNotFound());

    assertThrows(AccountNotFound.class, () -> service.delete("missing"));

    verify(em, never()).delete(any());
    verify(ccs, never()).delete(any());
  }

  @Test
  void testTransferPropagatesPersistenceFailureAndStopsPersisting() throws Exception {
    Account from = new Account("from", 100L);
    Account to = new Account("to", 40L);
    when(em.get("from", Account.class)).thenReturn(from);
    when(em.get("to", Account.class)).thenReturn(to);
    PersistenceException failure = new PersistenceException();
    doThrow(failure).when(em).persist(from);

    PersistenceException thrown = assertThrows(PersistenceException.class,
        () -> service.transfer("from", "to", 30L));

    assertSame(failure, thrown);
    verify(em, never()).persist(to);
  }

  @Test
  void testDeletePropagatesPersistenceFailure() throws Exception {
    Account account = new Account("acc-1", 0L);
    when(em.getCreditCards(account)).thenReturn(List.of());
    PersistenceException failure = new PersistenceException();
    doThrow(failure).when(em).delete(account);

    PersistenceException thrown = assertThrows(PersistenceException.class,
        () -> service.delete(account));

    assertSame(failure, thrown);
  }

  @Test
  void testDeleteStopsWhenAssociatedCardCannotBeCharged() throws Exception {
    Account account = new Account("acc-1", 0L);
    CreditCard card = new CreditCard("cc-1");
    when(em.getCreditCards(account)).thenReturn(List.of(card));
    NotEnoughBalance failure = new NotEnoughBalance();
    doThrow(failure).when(ccs).delete(card);

    NotEnoughBalance thrown = assertThrows(NotEnoughBalance.class,
        () -> service.delete(account));

    assertSame(failure, thrown);
    verify(em, never()).delete(account);
  }

}
