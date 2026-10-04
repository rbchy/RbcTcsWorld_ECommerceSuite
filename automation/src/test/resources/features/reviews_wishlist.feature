@api @reviews
Feature: Reviews, ratings and wishlist (Module 5)
  Only customers who received a product may review it. The product's average rating
  always reflects the published reviews. The wishlist is private and idempotent.

  Background:
    Given a new customer is registered and logged in
    And a product priced "25.00" with stock 10 exists

  @smoke
  Scenario: A verified buyer's review updates the product rating
    Given the customer has received that product
    And other verified buyers rated that product "4, 4"
    When the customer reviews that product with 5 stars and title "Excellent"
    Then the response status should be 201
    And the response field "reviewer" should be "qa***"
    And the product rating should be "4.3" from 3 reviews

  Scenario: A customer who never received the product cannot review it
    When the customer reviews that product with 5 stars
    Then the response status should be 403
    And the error message should contain "received"

  Scenario: Only one review per product
    Given the customer has received that product
    And the customer reviews that product with 5 stars
    When the customer reviews that product with 1 stars
    Then the response status should be 409

  Scenario Outline: Rating must be between 1 and 5
    Given the customer has received that product
    When the customer reviews that product with <stars> stars
    Then the response status should be <status>

    Examples:
      | stars | status |
      | 0     | 400    |
      | 1     | 201    |
      | 5     | 201    |
      | 6     | 400    |

  Scenario: Moderation removes a hidden review from the average
    Given the customer has received that product
    And other verified buyers rated that product "5"
    And the customer reviews that product with 1 stars and title "spam"
    And the product rating should be "3.0" from 2 reviews
    When an admin hides the customer's review
    Then the response status should be 200
    And the product rating should be "5.0" from 1 review

  @wishlist
  Scenario: Wishlist add is idempotent and move-to-cart empties it
    When the customer adds that product to the wishlist
    Then the response status should be 201
    When the customer adds that product to the wishlist
    Then the response status should be 200
    And the wishlist should contain 1 product
    When the customer moves that product from the wishlist to the cart
    Then the response status should be 200
    And the wishlist should contain 0 products
    And the cart should have 1 line
