package org.udg.caes.banking.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.udg.caes.banking.exceptions.NotEnoughBalance;

public class TestAccount {

  @Test
  void testConstructAndGetters() {
    Account account = new Account("acc-1", 250L);

    assertEquals("acc-1", account.getId());
    assertEquals(250L, account.getBalance());
  }

  @Test
  void testCreditAddsAmount() {
    Account account = new Account("acc-1", 100L);

    account.credit(25L);

    assertEquals(125L, account.getBalance());
  }

  @Test
  void testDebitWithEnoughBalance() throws NotEnoughBalance {
    Account account = new Account("acc-1", 100L);

    account.debit(30L);

    assertEquals(70L, account.getBalance());
  }

  @Test
  void testDebitWithInsufficientBalanceThrowsAndKeepsBalance() {
    Account account = new Account("acc-1", 100L);

    assertThrows(NotEnoughBalance.class, () -> account.debit(150L));
    assertEquals(100L, account.getBalance());
  }
}
