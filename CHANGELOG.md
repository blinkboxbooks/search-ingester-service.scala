# Change log

## 0.1.0 ([#1](https://git.mobcastdev.com/Agora/search-ingester/pull/1) 2014-09-08 14:17:51)

Initial version

### New features:

- Process book metadata, book undistribute and price update messages.
- Retry on temporary failure (only unrecoverable errors cause messages to be written to DLQ).
- Standard configuration and logging.


