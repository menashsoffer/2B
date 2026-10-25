# Rules & API Integration Guide

## Bank of Israel Exchange Rate (BOI)
- Endpoint: `https://edge.boi.gov.il/FusionEdgeServer/sdmx/v2/data/dataflow/BOI.STATISTICS/EXR/1.0/RER_USD_ILS`
- WorkManager Schedule: Enqueued as unique periodic work (`BoiSyncWorker`, policy `KEEP`) with `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()`.
- Schedule calculation: BOI representative rates are published on business days (Sunday–Thursday) at 16:00 Israel time. The periodic work triggers daily to fetch the latest rate after 16:00.
- Historical rate window: Queries up to 10 days back to locate the latest published representative rate on or before the requested date.

## Hebcal Integration
- Endpoint: `https://www.hebcal.com/hebcal?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=now&month=x&ss=on&mf=on&c=on&geo=geoname&geonameid={geonameid}`
- Daily Hebrew date is parsed from the corresponding calendar items category (e.g. `roshchodesh` or daily events with Hebrew representation).

## Real Recorded Samples Fetched
- Date fetched: October 25, 2026.
- Stored under `app/src/test/resources/`.
