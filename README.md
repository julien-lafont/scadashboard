# Scadashboard

Simple framework for building Business or Developer dashboard.

## Widgets available

 * Ping
 * Amazon CloudWatch metrics
 * Amazon EC2 instances
 * Weather
 * Github issues
 * Github pull-requests
 * Codeship
 * Amazon SES events (push-notifications from Amazon SNS) **always active**

## Communication protocol

All comunications are send through a web-socket.

### Start a new widget

```json
{
  "action": "start",
  "data": {
    "widget": "Ping",
    "id": "1:ping",
    "config": {
      "url": "http://www.google.fr",
      "interval": 10
    }
  }
}
```

The `id` must be unique for all your widgets. You can use `$index:$name` for example.

Returns a `started` event to confirm the creation, or an `error`.

```json
{ "event": "started", "data": { "id": "1:ping" } }
```

The widgets will regulary send their updates.

```
{ "event": "update", "data": { "<widget-id<": <widget-data> } }
```
### Stop a widget

```json
{ "action": "stop", "data": "1:ping" }
```

```
{ "event": "stopped", "data": { "id": "1:ping" } }
```

### Stop all widgets

```json
{ "action": "stop-all" }
```

### List active widgets

```json
{ "action":  "status" }
```

```json
{ "event": "status", "data": ["1:ping", "2:weather"] }
```

## Widgets

**Ping**

Ping an URL, return status and ping.

```json
{"action": "start", "data": {"widget": "Ping", "id": "1:ping", "config": {"url": "http://www.google.fr", "fetchContent": true, "interval": 1}}}
```

Config : 
 * url (required)
 * interval (required) in seconds
 * fetchContent (optional, default=true) Do a GET or just a HEAD?

```
{"event":"update","data":{"1:ping":{"url":"http://www.google.fr","success":true,"ping":98}}}
```

**Weather**

Return the weather for the specified city.

Documentation: [OpenWeatherMap](http://openweathermap.org/current)

```json
{"action": "start", "data": {"widget": "Weather", "id": "1:weather", "config": {"interval": 60, "city": "Montpellier", "country": "fr", "unit": "metric", "language": "fr"}}}
```

Config:

 * city (required)
 * country (optional)
 * unit (optional): imperial, metrics
 * language (optional): fr, en...
 * interval (optional) in seconds

```json
{
  "event":"update",
  "data":{
    "1:weather":{
      "coord":{"lon":3.88,"lat":43.61},
      "weather":[{"id":802,"main":"Clouds","description":"partiellement ensoleillé","icon":"03d"}],
      "base":"cmc stations",
      "main":{
        "temp":20.929,
        "temp_min":20.929,
        "temp_max":20.929,
        "pressure":982.6,
        "humidity":69
      },
      "wind":{"speed":1.26,"deg":280.005},
      "clouds":{"all":36}
    }
  }
}
```

**Amazon Cloudwatch metrics**

Return the statistics of a cloudwatch metrics

```json
{"action": "start", "data": {"widget": "CloudWatch", "id": "1:cloudwatch", "config": {"interval": 1, "namespace": "AWS/EC2", "metric": "NetworkIn", "instanceId": "i-7fc786d5", "period": 60, "since": 1 }}}
```

Config:
 * namespace (required) [aws-namespace](http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/aws-namespaces.html)
 * metric (required) [aws-metric](http://docs.aws.amazon.com/AmazonCloudWatch/latest/DeveloperGuide/ec2-metricscollected.html)
 * instanceId (requis)
 * period (required) Granularity (in s) of datapoints. Min 60, multiple of 60.
 * since (requis) Fetch the datapoints for the N last hours.

```json
{"event":"update","data":{"2:cloudwatch":{"1434304320000":288318461,"1434302040000":288558051,"1434303360000":288834271,"1434303540000":290506134,"1434304020000":287059788,"1434303960000":285087595,"1434303900000":288596710,"1434300900000":288018407,"1434303480000":293454792,"1434303420000":288138737,"1434303240000":289734447,"1434302460000":289559817,"1434301680000":293763600,"1434302640000":289669546,"1434303120000":289487857,"1434301440000":290510582,"1434303060000":292413611,"1434301860000":293872165,"1434301020000":288144432,"1434302940000":290112439,"1434301380000":293555723,"1434302580000":294265180,"1434301620000":287092116,"1434304140000":288919045,"1434304200000":293733757,"1434302340000":290091144,"1434301500000":288406488,"1434303840000":283264921,"1434302160000":288652301,"1434303300000":294559430,"1434302220000":289853774,"1434300780000":288505197,"1434302400000":295451706,"1434302700000":293270383,"1434302280000":295240917,"1434301140000":288049709,"1434301200000":288458455,"1434301740000":291084616,"1434304080000":290330646,"1434302100000":292291300,"1434303780000":299295933,"1434301320000":288738105,"1434302760000":288186256,"1434303720000":291789206,"1434302880000":294139651,"1434301080000":292955182,"1434300960000":292275011,"1434303000000":291427933,"1434301800000":287729249,"1434301920000":287522892,"1434301560000":295091110,"1434302820000":290415301,"1434302520000":288013478,"1434304260000":288334542,"1434303600000":295374975,"1434300840000":292344576,"1434303180000":294121771,"1434303660000":290316364,"1434301260000":293424137,"1434301980000":293376282}}}
```

**Amazon EC2 instances**

Return the list of all *running* EC2 instances

```json
{"action": "start", "data": {"widget": "EC2", "id": "1:ec2", "config": {"interval": 60 }}}
```

**Github pull-requests**

Return the pull-requests in the organization, or only in one repository.

```json
{"action": "start", "data": {"widget": "GitHubPullRequests", "id": "1:ghpr", "config": {"organization": "tabmo", "repository": "manager-front", "interval": 60}}}
```

Config:
 * Organization (required)
 * Repository (optional)

```json
{
  "event":"update",
  "data":{
    "1:githubpullrequests":[
      {
        "repository":"manager-front",
        "title":"Removing the use of ui-grid in the orders and lines view because of t…",
        "createdAt":"2015-06-12T14:49:15Z",
        "creator":"audrey-novak",
        "avatar":"https://avatars.githubusercontent.com/u/12810711?v=3"
      }
    ]
  }
}
```

**Github Issues**

Return the issues in the organization, or only in one repository.

```json
{"action": "start", "data": {"widget": "GitHubIssues", "id": "1:ghi", "config": {"organization": "tabmo", "repository": "manager-front", "interval": 60}}}
```
Config:
 * Organization (required)
 * Repository (optional)

```json
TODO
```

**Codeship build status**

Return the build status of a project.

```json
{"action": "start", "data": {"widget": "CodeShip", "id": "1:codeship", "config": {"projectId": "76XXX", "branch": "develop", "interval": 60}}}
```
Config:
 * projectId (required)
 * branch (optional)

```json
{
  "event":"update",
  "data":{
    "1:codeship":{
      "id":00000,
      "uuid":"XXXXXXX-ccab-0132-3442-7299564565fe",
      "repository_name":"orga/name",
      "repository_provider":"github",
      "builds":[
        {
          "id":6248647,
          "uuid":"XXXXXX-f30a-0132-f31e-0283dcdf0169",
          "project_id":00000,
          "status":"success",
          "github_username":"julienlafont",
          "commit_id":"f8449a0fd68480708a4e043865e66e73f404ecde",
          "message":"XXXXXXXXXX",
          "branch":"develop",
          "started_at":"2015-06-12T08:24:34.332Z",
          "finished_at":"2015-06-12T08:27:35.449Z"
        }
      ]
    }
  }
}
```

**Amazon SES**

Forward push-notifications send by Amazon SES though Amazon SNS.

```json
{
  "event":"update",
  "data":{
    "ses":{
      "source":"XXXX <yyyyy@tabmo.io>",
      "destination":"xxxx.yyyyy@hotmail.com",
      "timestamp":1434305346325,
      "notificationType":"Bounce",
      "delivered":false
    }
  }
}
```
