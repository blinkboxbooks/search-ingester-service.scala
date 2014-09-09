Then(/^the book should (not )?be available on the search endpoint$/) do |availability|
  search_for_books(@search_keyword)

  if availability == "not "
    expect(@response['numberOfResults']).to eq(0)
  else
    expect(@response['numberOfResults']).to be > 0
  end
end

Then(/^the books should be available on the search endpoint ordered by price$/) do
  @params = {:order => "PRICE"}
  search_for_books(@search_keyword)
end

Then(/^the updated book should appear above the cheaper item$/) do
  expect(@response['books'].first['id']).to eql(@updated_book['isbn'])
end
