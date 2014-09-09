Given(/^a new book has been ingested into the shop database$/) do
  @book = { "isbn" => random_isbn, "title" => "Some test data - #{@search_keyword}" }
end

Given(/^(a|\d+) books? ha(?:s|ve) been successfully ingested to the search database$/) do |number|
  count = number == "a" ? 1 : number.to_i

  count.times do
    step("a new book has been ingested into the shop database")
    step("a message marking this book as distributed is received")
    step("a message setting the price of a book to £0.99 is received")
  end

  step("I process the book information")
  step("the book should be available on the search endpoint")
end
