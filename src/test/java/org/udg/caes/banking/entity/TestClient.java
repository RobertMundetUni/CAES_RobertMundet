package org.udg.caes.banking.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class TestClient {

  @Test
  void testConstructAndGetter() {
    Client client = new Client("client-1");

    assertEquals("client-1", client.getId());
  }
}
