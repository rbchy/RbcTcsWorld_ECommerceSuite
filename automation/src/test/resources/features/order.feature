@api @order
Feature: Place and cancel orders (Module 2)
  As a customer I want to turn my cart into an order, and cancel it if I change my mind.
  Stock must always stay correct.

  Background:
    Given a new customer is registered and logged in
    And a product priced "12.50" with stock 5 exists

  @smoke @e2e
  Scenario: Order from cart reduces stock and empties the cart
    Given the customer has 2 of that product in the cart
    When the customer places the order
    Then the response status should be 201
    And the order status should be "PLACED"
    And the order subtotal should be "25.00"
    # 25.00 + 5.99 shipping (below 50.00) + 1.50 tax (6%)
    And the order total should be "32.49"
    And the product stock should be 3
    And the cart should have 0 lines

  Scenario: Cancelling an order gives the stock back
    Given the customer has 2 of that product in the cart
    And the customer has placed the order
    When the customer cancels the order
    Then the response status should be 200
    And the order status should be "CANCELLED"
    And the product stock should be 5

  Scenario: An order cannot be cancelled twice
    Given the customer has 1 of that product in the cart
    And the customer has placed the order
    And the customer cancels the order
    When the customer cancels the order
    Then the response status should be 409
    And the product stock should be 5

  Scenario: An empty cart cannot be ordered
    When the customer places the order
    Then the response status should be 400
    And the error message should contain "Cart is empty"
