module KnowsAboutSolrServiceRequests
  def commit_solr_index
    xml = "<commit/>"
    http_post :solr, "books/update/", xml, "Content-Type" => "application/xml"
  end
end

World(KnowsAboutSolrServiceRequests)
