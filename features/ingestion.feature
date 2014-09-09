Feature: Search Ingestion
  As a platform
  I want to be able to ingest information about books hitting the shop
  So that users can search for books to buy

  @smoke
  Scenario: Search ingestion messages are stored in the search index
    Given a new book has been ingested into the shop database
    When a message marking this book as distributed is received
    And a message setting the price of a book to £0.99 is received
    And I process the book information
    Then the book should be available on the search endpoint

  @smoke
  Scenario: Undistribute message
    Given a book has been successfully ingested to the search database
    When a message marking this book as undistributed is received
    And I process the book information
    Then the book should not be available on the search endpoint

  @smoke
  Scenario: Price notification messages
    Given 2 books have been successfully ingested to the search database
    When a message updating the price of a book to £1.99 is received
    And I process the price information
    Then the books should be available on the search endpoint ordered by price
    And the updated book should appear above the cheaper item
