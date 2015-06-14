var client = new Keen({
    projectId: "5368fa5436bf5a5623000000",
    readKey: "3f324dcb5636316d6865ab0ebbbbc725224c7f8f3e8899c7733439965d6d4a2c7f13bf7765458790bd50ec76b4361687f51cf626314585dc246bb51aeb455c0a1dd6ce77a993d9c953c5fc554d1d3530ca5d17bdc6d1333ef3d8146a990c79435bb2c7d936f259a22647a75407921056"
});

Keen.ready(function(){

    // -----------------------------
    // Ugly Websocket handler
    // -----------------------------
    var CONNECTING = 0;
    var OPEN = 1;
    var CLOSING = 2;
    var CLOSED = 3;

    var websocket = new WebSocket(WS_URL);
    var queue = [];
    websocket.onopen = onOpen;
    websocket.onclose = onClose;
    websocket.onmessage = onMessage;
    websocket.onerror = onError;

    function onOpen(e) {
        console.info("Connected");
        flushQueue();
    }

    function onClose(e) {
        console.info("Disconnected");
    }

    function onMessage(e) {
        //console.log("<< " + e.data);
        var json = JSON.parse(e.data);

        var event = json.event;
        switch (event) {
            case "update":
                for (var key in json.data) {
                    update(key, json.data[key]);
                }
            break;
        }
    }

    function onError(e) {
        console.error(e);
    }

    function flushQueue() {
        for (var i in queue) send(queue[i]);
    }

    function send(obj) {
        if (websocket.readyState == CONNECTING) {
            queue.push(obj);
        } else if (websocket.readyState == OPEN) {
            var json = JSON.stringify(obj);
            websocket.send(json);
            console.log(">> " + json);
        } else {
            console.err("Cannot send message because websocket is in state " + ws.readyState);
        }
    }

    var widgets = {};
    var widgetIndex = 0;

    function registerWidget(name, config, callback) {
        var id = widgetIndex++;
        var wid = id + ":" + name;

        send({
            action: "start",
            data: {
                widget: name,
                id: wid,
                config: config
            }
        });

        widgets[wid] = callback;
    }

    function update(wid, payload) {
        var callback = widgets[wid];
        callback(payload);
    }

    // ----------------------------------------
    // Widgets: ping
    // ----------------------------------------
    $(".ping").each(function() {
        var that = $(this);
        var callback = (function($elem) {
            return function(payload) {
                var success = payload.success;
                var ping = payload.ping;

                var color = '#87CEEB';
                if (success && ping > 300) color = 'orange';
                else if (!success) color = '#FF0000';

                $elem.val(ping).trigger('change');
                $elem.trigger('configure', { fgColor: color, inputColor: color });
            };
        })(that);

        var config = {
            url: that.data("url"),
            interval: that.data("interval")
        };

        registerWidget("Ping", config, callback);
    }).css({visibility: "visible"})
      .knob({
        max: 1000,
        readOnly: true,
        angleOffset: -125,
        angleArc: 250
    });

    // ---------------------------------------
    // Chart line
    // ---------------------------------------

    /*var baseParams = {
        timeframe: {
            start: start.toISOString(),
            end: end.toISOString()
        },
        filters: geoFilter
    };
    var end = new Date();
    var start = new Date(end.getFullYear(), end.getMonth(), end.getDate(), end.getHours()-1);

    var hearts = new Keen.Query("median", {
        eventCollection: "user_action",
        interval: "daily",
        targetProperty: "bio_sensors.heart_rate",
        timeframe: {
            start: start, end: end
        }
    });
    var daily_median_heartrate = new Keen.Dataviz()
        .el(document.getElementById("chart-01"))
        .height(300)
        .colors([Keen.Dataviz.defaults.colors[1]])
        .library("google")
        .chartType("areachart")
        .chartOptions({
            chartArea: { top: "5%", height: "80%", left: "10%", width: "85%" },
            hAxis: { format: 'MMM dd', maxTextLines: 1 },
            legend: { position: "none" },
            tooltip: { trigger: 'none' }
        })
        .prepare();

    client.run(hearts, function(){
        daily_median_heartrate
            .parseRequest(this)
            .title(null)
            .render();
    });*/


    // ----------------------------------------
    // Pageviews Area Chart
    // ----------------------------------------
    var pageviews_timeline = new Keen.Query("count", {
        eventCollection: "pageviews",
        interval: "hourly",
        groupBy: "user.device_info.browser.family",
        timeframe: {
            start: "2014-05-04T00:00:00.000Z",
            end: "2014-05-05T00:00:00.000Z"
        }
    });
    console.log(pageviews_timeline);
    client.draw(pageviews_timeline, document.getElementById("chart-01"), {
        chartType: "areachart",
        title: false,
        height: 250,
        width: "auto",
        chartOptions: {
            chartArea: {
                height: "85%",
                left: "5%",
                top: "5%",
                width: "80%"
            },
            isStacked: true
        }
    });


    // ----------------------------------------
    // Pageviews Pie Chart
    // ----------------------------------------
    var pageviews_static = new Keen.Query("count", {
        eventCollection: "pageviews",
        groupBy: "user.device_info.browser.family",
        timeframe: {
            start: "2014-05-01T00:00:00.000Z",
            end: "2014-05-05T00:00:00.000Z"
        }
    });
    client.draw(pageviews_static, document.getElementById("chart-02"), {
        chartType: "piechart",
        title: false,
        height: 250,
        width: "auto",
        chartOptions: {
            chartArea: {
                height: "85%",
                left: "5%",
                top: "5%",
                width: "100%"
            },
            pieHole: 0.4
        }
    });


    // ----------------------------------------
    // Impressions timeline
    // ----------------------------------------
    var impressions_timeline = new Keen.Query("count", {
        eventCollection: "impressions",
        groupBy: "ad.advertiser",
        interval: "hourly",
        timeframe: {
            start: "2014-05-04T00:00:00.000Z",
            end: "2014-05-05T00:00:00.000Z"
        }
    });
    client.draw(impressions_timeline, document.getElementById("chart-03"), {
        chartType: "columnchart",
        title: false,
        height: 250,
        width: "auto",
        chartOptions: {
            chartArea: {
                height: "75%",
                left: "10%",
                top: "5%",
                width: "60%"
            },
            bar: {
                groupWidth: "85%"
            },
            isStacked: true
        }
    });


    // ----------------------------------------
    // Impressions timeline (device)
    // ----------------------------------------
    var impressions_timeline_by_device = new Keen.Query("count", {
        eventCollection: "impressions",
        groupBy: "user.device_info.device.family",
        interval: "hourly",
        timeframe: {
            start: "2014-05-04T00:00:00.000Z",
            end: "2014-05-05T00:00:00.000Z"
        }
    });
    client.draw(impressions_timeline_by_device, document.getElementById("chart-04"), {
        chartType: "columnchart",
        title: false,
        height: 250,
        width: "auto",
        chartOptions: {
            chartArea: {
                height: "75%",
                left: "10%",
                top: "5%",
                width: "60%"
            },
            bar: {
                groupWidth: "85%"
            },
            isStacked: true
        }
    });


    // ----------------------------------------
    // Impressions timeline (country)
    // ----------------------------------------
    var impressions_timeline_by_country = new Keen.Query("count", {
        eventCollection: "impressions",
        groupBy: "user.geo_info.country",
        interval: "hourly",
        timeframe: {
            start: "2014-05-04T00:00:00.000Z",
            end: "2014-05-05T00:00:00.000Z"
        }
    });
    client.draw(impressions_timeline_by_country, document.getElementById("chart-05"), {
        chartType: "columnchart",
        title: false,
        height: 250,
        width: "auto",
        chartOptions: {
            chartArea: {
                height: "75%",
                left: "10%",
                top: "5%",
                width: "60%"
            },
            bar: {
                groupWidth: "85%"
            },
            isStacked: true
        }
    });


});
