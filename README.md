# Scadashboard

Simple framework for building Business or Developer dashboard.

## Widgets available

 * Ping
 * CloudWatch metrics
 * Weather
 * Github issues
 * Github pull-requests
 * Codeship

## Communication protocol

All comunications are send through a websocket.

### Start a new widget

```json
{
  "action": "start",
  "data": {
    "widget": "Ping",
    "config": {
      "url": "http://www.google.fr",
      "interval": 10
    }
  }
}
```

Returns a `started` event with the unique widget name generated

```json
{ "event": "started", "data": { "name": "Ping:1" } }
```

The widgets will regulary send their updates.

```
{ "event": "update", "data": { "widget-name": { widget-data } } }
```
### Stop a widget

```json
{ "action": "stop", "data": "Ping:1" }
```

```
{ "event": "stopped", "data": { "name": "Ping:1" } }
```

### Stop all widgets

```json
{ "event": "stop-all" }
```

### List active widgets

```json
{ "event":  "status" }
```

```
{ "event": "status", "data": ["widget:1", "widget:2"] }
```

## Widgets

TODO

**Ping**

**Weather**

**Cloudwatch metrics**

**Github Pullrequests**

**Github Issues**

**Codeship build status**
