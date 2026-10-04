package org.udg.caes.banking.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TestCreditCard {

  @Test
  void testConstructAndGetters() {
    CreditCard creditCard = new CreditCard("cc-1");

    assertEquals("cc-1", creditCard.getId());
    assertEquals(0L, creditCard.getCredit());
    assertEquals(0L, creditCard.getMaxCredit());
  }

  @Test
  void testCreditAddsAmount() {
    CreditCard creditCard = new CreditCard("cc-1");

    creditCard.credit(30L);

    assertEquals(30L, creditCard.getCredit());
  }

  @Test
  void testResetClearsBalance() {
    CreditCard creditCard = new CreditCard("cc-1");
    creditCard.credit(45L);

    creditCard.reset();

    assertEquals(0L, creditCard.getCredit());
  }
}
