@api @cart
Feature: Shopping cart (Module 1)
  As a logged-in customer I want to keep products in a cart before checkout.

  Background:
    Given a new customer is registered and logged in

  @smoke
  Scenario: Add a product to an empty cart
    When the customer adds 2 of product "ELEC-MOUSE-001" to the cart
    Then the response status should be 201
    And the cart should have 1 line
    And the cart line for "ELEC-MOUSE-001" should have quantity 2

  Scenario: Adding the same product again merges the quantity
    Given the customer has 2 of product "ELEC-MOUSE-001" in the cart
    When the customer adds 3 of product "ELEC-MOUSE-001" to the cart
    Then the response status should be 201
    And the cart should have 1 line
    And the cart line for "ELEC-MOUSE-001" should have quantity 5

  Scenario: Change quantity of a cart line
    Given the customer has 1 of product "ELEC-KEYB-002" in the cart
    When the customer changes the quantity of the first cart line to 4
    Then the response status should be 200
    And the cart line for "ELEC-KEYB-002" should have quantity 4

  Scenario: Cannot add more than the available stock
    # ELEC-HEAD-005 is seeded with stock 3
    When the customer adds 4 of product "ELEC-HEAD-005" to the cart
    Then the response status should be 409
    And the error message should contain "Insufficient stock"

  Scenario Outline: Quantity outside 1..10 is rejected
    When the customer adds <qty> of product "ELEC-MOUSE-001" to the cart
    Then the response status should be 400

    Examples:
      | qty |
      | 0   |
      | 11  |

  Scenario: Empty the cart
    Given the customer has 1 of product "ELEC-MOUSE-001" in the cart
    When the customer empties the cart
    Then the response status should be 204
    And the cart should have 0 lines

  @security
  Scenario: Anonymous user cannot see a cart
    Given the customer is not logged in
    When the customer opens the cart
    Then the response status should be 401
