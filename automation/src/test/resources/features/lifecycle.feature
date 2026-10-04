@api @lifecycle
Feature: Complete order lifecycle (Module 4)
  The flagship end-to-end journey of the platform, from registration to refund,
  checked through the public API at every step.

  Background:
    Given a new customer is registered and logged in
    And a product priced "30.00" with stock 10 exists

  @smoke @e2e @flagship
  Scenario: Register, buy with a coupon, pay, ship, deliver, track, return and get refunded
    Given the customer has 2 of that product in the cart
    When the customer asks for a price quote with coupon "WELCOME10"
    Then the quote should show discount "6.00", shipping "0.00", tax "3.24" and total "57.24"
    When the customer places the order with coupon "WELCOME10"
    Then the response status should be 201
    And the order total should be "57.24"
    And the product stock should be 8
    When the customer pays with card "4242424242424242"
    Then the response status should be 200
    When the warehouse ships the order with "UPS"
    Then anyone can track the parcel without logging in and sees "SHIPPED"
    When the order is delivered
    Then the tracking timeline should be "PLACED, PAID, SHIPPED, DELIVERED"
    When the customer requests a return because "WRONG_ITEM"
    Then the response status should be 201
    When customer service approves the return
    Then the response status should be 200
    And the refund should be "57.24" and restocked should be "true"
    And the order should now be "RETURNED"
    And the product stock should be 10
    And the tracking timeline should be "PLACED, PAID, SHIPPED, DELIVERED, RETURN_REQUESTED, RETURNED"

  Scenario Outline: Return decision table - refund and restock depend on the reason
    # 1 x 30.00 = 30.00 + 5.99 shipping + 1.80 tax = 37.79
    Given the customer has 1 of that product in the cart
    And the customer has placed the order
    And the customer pays with card "4242424242424242"
    And the warehouse ships the order with "FEDEX"
    And the order is delivered
    When the customer requests a return because "<reason>"
    And customer service approves the return
    Then the refund should be "<refund>" and restocked should be "<restocked>"
    And the product stock should be <stock>

    Examples:
      | reason           | refund | restocked | stock |
      | DAMAGED          | 37.79  | false     | 9     |
      | WRONG_ITEM       | 37.79  | true      | 10    |
      | NOT_AS_DESCRIBED | 37.79  | true      | 10    |
      | NO_LONGER_NEEDED | 31.80  | true      | 10    |

  Scenario: A rejected return sends the order back to DELIVERED
    Given the customer has 1 of that product in the cart
    And the customer has placed the order
    And the customer pays with card "4242424242424242"
    And the warehouse ships the order with "USPS"
    And the order is delivered
    And the customer requests a return because "NO_LONGER_NEEDED"
    When customer service rejects the return
    Then the response status should be 200
    And the order should now be "DELIVERED"

  Scenario: A shipped order can no longer be cancelled
    Given the customer has 1 of that product in the cart
    And the customer has placed the order
    And the customer pays with card "4242424242424242"
    And the warehouse ships the order with "UPS"
    When the customer cancels the order
    Then the response status should be 409
    And the error message should contain "SHIPPED"
