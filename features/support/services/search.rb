module KnowsAboutSearchServiceRequests
  def search_for_books(query)
    query = url_encode(query)
    query += "&#{@params.to_query}" if @params
    http_get :search, "/books?q=#{query}"
    @response = parse_response_data
  end

  def url_encode(string)
    URI.encode_www_form_component(string)
  end
end

World(KnowsAboutSearchServiceRequests)
