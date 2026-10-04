@api @catalog
Feature: Product catalog
  As a shopper I want to browse and search products without logging in.

  @smoke
  Scenario: Customer opens catalog
    When I request the product catalog
    Then the response status should be 200
    And the catalog should contain at least 8 products

  Scenario: Search is case-insensitive
    When I search products for "KEYBOARD"
    Then the response status should be 200
    And every product name should contain "keyboard"

  Scenario: Unknown product returns 404 with a JSON error
    When I request product id 999999
    Then the response status should be 404
    And the error message should contain "Product not found"
