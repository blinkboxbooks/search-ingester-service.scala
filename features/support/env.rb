require 'active_support/core_ext/hash'
require 'rspec'
require 'blinkbox/events/distribution/book'
require 'blinkbox/events/distribution/undistribute'
require 'blinkbox/events/messaging'

TEST_CONFIG = {}
TEST_CONFIG['server'] = ENV['SERVER'] || 'LOCAL'
TEST_CONFIG['proxy']  = ENV['PROXY_SERVER']
TEST_CONFIG['debug']  = ENV['DEBUG'] =~ /^on|true$/i unless ENV['DEBUG'].nil?

puts "TEST_CONFIG: #{TEST_CONFIG}" if TEST_CONFIG["debug"]

require 'cucumber/blinkbox/environment'
require 'cucumber/blinkbox/data_dependencies'
require 'cucumber/blinkbox/subjects'
require 'cucumber/blinkbox/requests'
require 'cucumber/blinkbox/responses'
require 'cucumber/blinkbox/response_validation'
require 'httpclient'
require 'httpclient/capture'

def random_isbn
  rand((8 * (10**12))..(9 * (10**12))).to_s
end

def distribute_message(hash)
  # Merge in default values that the search-ingester XSLT cares about
  hash.merge!(
    "contributors" => [
    {
      "role"            => "Author",
      "contributor_id"  => "0000000000000000000000000000000000000000",
      "display_name"    => "Testy McTesterson"
    }],
    "descriptions" => [
    {
      "format"  => "html",
      "type"    => "Short description/annotation",
      "content" => "Short stuff"
    },
    {
      "format"  => "html",
      "type"    => "Long description",
      "content" => "Longer stuff here"
    }],
    "dates" => {
      "publish" => Time.now.strftime('%Y-%m-%d')
    }
  )
  Blinkbox::Events::Distribution::Book.new(hash)
end

def undistribute_message(hash)
  # Merge in default values needed to construct an undistribute message
  hash.merge!(
    "reason_list" => [{
      "code" => "00",
      "description" => "testing",
      "authorized_by" => { "entity" => "Test Script", "name" => "Search Ingester Tests" }
    }],
    "effective_from" => Time.now - 3600
  )
  Blinkbox::Events::Distribution::Undistribute.new(hash)
end

def price_message(isbn, price)
  message = <<EOF
<?xml version="1.0"?>
<book-price xmlns="http://schemas.blinkbox.com/books/pricing" xmlns:r="http://schemas.blinkbox.com/books/routing" r:originator="ingestion" r:instance="" r:version="0.0.1">
  <isbn>#{isbn}</isbn>
  <price>#{price}</price>
  <currency>GBP</currency>
</book-price>
EOF
  message
end

Before do
  unless $mq_connected == true
    rabbit = test_env.rabbit
    amqp = "amqp://#{rabbit['user']}:#{rabbit['password']}@#{rabbit['host']}:#{rabbit['port']}"
    Blinkbox::MQ.connect(amqp)
    $mq_connected = true
  end

  # Assign a unique keyword as part of the book metadata for a given scenario
  @search_keyword = (0...10).map { ('a'..'z').to_a[rand(26)] }.join
end
