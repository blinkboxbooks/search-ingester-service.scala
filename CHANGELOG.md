# Change log

## 0.1.3 ([#4](https://git.mobcastdev.com/Agora/search-ingester/pull/4) 2014-09-26 17:11:16)

Fixed Akka logging

### Bugfix

- Akka log messages now go to Graylog ([CP-1879](http://jira.blinkbox.local/jira/browse/CP-1879))

## 0.1.2 ([#3](https://git.mobcastdev.com/Agora/search-ingester/pull/3) 2014-09-09 14:38:40)

Copy the smoke tests over from the Mule SI to the Scala implementation

Patch

## 0.1.1 ([#2](https://git.mobcastdev.com/Agora/search-ingester/pull/2) 2014-09-09 09:01:54)

Rename to avoid clash with old Mule version of service.

### Improvements.

- Renamed service (hence RPM file) from `search-ingester` to `search-ingester-service`.


## 0.1.0 ([#1](https://git.mobcastdev.com/Agora/search-ingester/pull/1) 2014-09-08 14:17:51)

Initial version

### New features:

- Process book metadata, book undistribute and price update messages.
- Retry on temporary failure (only unrecoverable errors cause messages to be written to DLQ).
- Standard configuration and logging.


