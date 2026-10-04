package org.udg.caes.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.udg.caes.banking.entity.Account;
import org.udg.caes.banking.entity.CreditCard;
import org.udg.caes.banking.exceptions.CreditExceeded;
import org.udg.caes.banking.exceptions.NotEnoughBalance;
import org.udg.caes.banking.exceptions.PersistenceException;
import org.udg.caes.banking.manager.EntityManager;

@ExtendWith(MockitoExtension.class)
public class TestCreditCardService {

  @Mock
  private EntityManager em;

  @InjectMocks
  private CreditCardService service;

  @Test
  void testCreditWithinLimitAddsAmountAndPersists() throws Exception {
    CreditCard creditCard = spy(new CreditCard("cc-1"));
    doReturn(200L).when(creditCard).getMaxCredit();

    service.credit(creditCard, 50L);

    assertEquals(50L, creditCard.getCredit());
    verify(em).persist(creditCard);
  }

  @Test
  void testCreditAboveLimitThrowsCreditExceeded() throws Exception {
    CreditCard creditCard = spy(new CreditCard("cc-1"));
    creditCard.credit(80L);
    doReturn(100L).when(creditCard).getMaxCredit();

    assertThrows(CreditExceeded.class, () -> service.credit(creditCard, 30L));
    assertEquals(80L, creditCard.getCredit());
    verify(em, never()).persist(any());
  }

  @Test
  void testChargeDeductsBalanceAndResetsCard() throws Exception {
    Account account = new Account("acc-1", 100L);
    CreditCard creditCard = new CreditCard("cc-1");
    creditCard.credit(30L);

    service.charge(creditCard, account);

    assertEquals(70L, account.getBalance());
    assertEquals(0L, creditCard.getCredit());
    verify(em).persist(creditCard);
    verify(em).persist(account);
  }

  @Test
  void testChargeWithNotEnoughBalanceThrowsAndKeepsState() throws Exception {
    Account account = new Account("acc-1", 50L);
    CreditCard creditCard = new CreditCard("cc-1");
    creditCard.credit(60L);

    assertThrows(NotEnoughBalance.class, () -> service.charge(creditCard, account));
    assertEquals(50L, account.getBalance());
    assertEquals(60L, creditCard.getCredit());
    verify(em, never()).persist(any());
  }

  @Test
  void testDeleteChargesCardAndDeletesIt() throws Exception {
    Account account = new Account("acc-1", 100L);
    CreditCard creditCard = new CreditCard("cc-1");
    creditCard.credit(20L);
    when(em.getAccountAssociated(creditCard)).thenReturn(account);

    service.delete(creditCard);

    assertEquals(80L, account.getBalance());
    assertEquals(0L, creditCard.getCredit());
    verify(em).delete(creditCard);
  }

  @Test
  void testInvalidateSetsCardInactive() throws Exception {
    CreditCard creditCard = spy(new CreditCard("cc-1"));
    creditCard.setActive(true);

    service.invalidate(creditCard);

    verify(creditCard).setActive(false);
    verify(em).persist(creditCard);
  }

  @Test
  void testDeleteWithInsufficientBalanceThrowsAndDoesNotDelete() throws Exception {
    Account account = new Account("acc-1", 10L);
    CreditCard card = new CreditCard("cc-1");
    card.credit(20L);
    when(em.getAccountAssociated(card)).thenReturn(account);

    assertThrows(NotEnoughBalance.class, () -> service.delete(card));

    verify(em, never()).delete(any());
  }

  @Test
  void testDeleteStopsWhenChargeCannotBePersisted() throws Exception {
    Account account = new Account("acc-1", 100L);
    CreditCard card = new CreditCard("cc-1");
    card.credit(20L);
    when(em.getAccountAssociated(card)).thenReturn(account);
    PersistenceException failure = new PersistenceException();
    doThrow(failure).when(em).persist(card);

    PersistenceException thrown = assertThrows(PersistenceException.class,
        () -> service.delete(card));

    assertSame(failure, thrown);
    verify(em, never()).persist(account);
    verify(em, never()).delete(any());
  }

  @Test
  void testDeletePropagatesDeletionPersistenceFailure() throws Exception {
    Account account = new Account("acc-1", 100L);
    CreditCard card = new CreditCard("cc-1");
    when(em.getAccountAssociated(card)).thenReturn(account);
    PersistenceException failure = new PersistenceException();
    doThrow(failure).when(em).delete(card);

    PersistenceException thrown = assertThrows(PersistenceException.class,
        () -> service.delete(card));

    assertSame(failure, thrown);
    verify(em).persist(card);
    verify(em).persist(account);
  }

}
