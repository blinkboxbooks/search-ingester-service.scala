When(/^a message marking this book as (\w+)d is received$/) do |action|
  @message = send("#{action}_message".to_sym, @book)

  # Posting directly into Search-Ingester's queue
  channel  = Blinkbox::MQ.connection.create_channel
  queue    = channel.queue("Distribution.Book.Search", :durable => true)
  exchange = channel.default_exchange

  exchange.publish(@message.to_xml, :routing_key => queue.name)
end

When(/^a message (upda|set)ting the price of a book to £(\d+\.\d+) is received$/) do |method, price|
  @message = price_message(@book['isbn'], price)

  channel  = Blinkbox::MQ.connection.create_channel
  queue    = channel.queue("Price.Notification.Search", :durable => true)
  exchange = channel.fanout("Price.Notification", :durable => true)

  exchange.publish(@message, :routing_key => queue.name)

  @updated_book = {"isbn" => @book['isbn']} if method == "upda"
end

When(/^I process the \w+ information$/) do
  sleep(5) # Technically a no-op whilst magic happens and s-i consumes messages
  commit_solr_index
end
