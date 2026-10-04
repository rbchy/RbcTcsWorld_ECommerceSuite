@api @checkout
Feature: Checkout, coupons and payment (Module 3)
  Price = subtotal - coupon discount + shipping (5.99 below 50.00) + 6% tax.
  Payments go through a mock gateway with Stripe-style test cards.

  Background:
    Given a new customer is registered and logged in

  Scenario Outline: Price breakdown with and without coupons
    Given a product priced "<price>" with stock 10 exists
    And the customer has <qty> of that product in the cart
    When the customer asks for a price quote with coupon "<coupon>"
    Then the response status should be 200
    And the quote should show discount "<discount>", shipping "<shipping>", tax "<tax>" and total "<total>"

    Examples:
      | price | qty | coupon    | discount | shipping | tax  | total |
      | 49.99 | 1   |           | 0.00     | 5.99     | 3.00 | 58.98 |
      | 50.00 | 1   |           | 0.00     | 0.00     | 3.00 | 53.00 |
      | 30.00 | 2   | WELCOME10 | 6.00     | 0.00     | 3.24 | 57.24 |
      | 30.00 | 1   | SAVE5     | 5.00     | 5.99     | 1.50 | 32.49 |

  Scenario Outline: Coupons that must be rejected
    Given a product priced "30.00" with stock 10 exists
    And the customer has 1 of that product in the cart
    When the customer asks for a price quote with coupon "<coupon>"
    Then the response status should be 400
    And the error message should contain "<message>"

    Examples:
      | coupon    | message             |
      | EXPIRED20 | expired             |
      | FUTURE15  | not active yet      |
      | DISABLED  | Invalid coupon code |

  @smoke @e2e
  Scenario Outline: Paying for an order with different test cards
    Given a product priced "10.00" with stock 10 exists
    And the customer has 2 of that product in the cart
    And the customer has placed the order
    When the customer pays with card "<card>"
    Then the response status should be <status>
    And the order should now be "<orderStatus>"

    Examples:
      | card             | status | orderStatus | note               |
      | 4242424242424242 | 200    | PAID        | approved           |
      | 4000000000000002 | 402    | PLACED      | card declined      |
      | 4000000000009995 | 402    | PLACED      | insufficient funds |
      | 4242424242424241 | 400    | PLACED      | fails Luhn check   |

  Scenario: Cancelling a paid order refunds it
    Given a product priced "10.00" with stock 10 exists
    And the customer has 1 of that product in the cart
    And the customer has placed the order
    And the customer pays with card "4242424242424242"
    When the customer cancels the order
    Then the response status should be 200
    And the order status should be "CANCELLED"
    And the product stock should be 10
