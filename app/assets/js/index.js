$(function() {

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
    })
        .css({visibility: "visible"})
        .knob({
            max: 1000,
            readOnly: true,
            angleOffset: -125,
            angleArc: 250
        });

    // ----------------------------------------
    // Widgets: SES notifications
    // ----------------------------------------
    widgets.ses = function(payload) {
        if (payload.delivered) {
            noty({text: '<span style="font-size: 24px">✉ Email délivré à <i>'+payload.destination+'</i></span>', layout: 'center', type: 'success', theme: 'relax', timeout: 10000});
        } else {
            noty({text: '<span style="font-size: 24px">✉ Email délivré à <i>'+payload.destination+'</i></span>', layout: 'center', type: 'error', theme: 'relax', timeout: 10000});
        }
    };

    // ----------

    Chart.defaults.global.animation = true;
    Chart.defaults.global.responsive = false;

    var ctx = document.getElementById("chart-03").getContext("2d");

    var data = {
        labels: ["1434313260000", "1434315300000", "1434313080000", "1434315060000", "1434313200000", "1434312780000", "1434312120000", "1434313500000", "1434314040000", "1434315360000", "1434314280000", "1434313380000", "1434312060000", "1434313140000", "1434314340000", "1434314400000", "1434314460000", "1434312600000", "1434315240000", "1434312900000", "1434314220000", "1434314760000", "1434313800000", "1434315120000", "1434313980000", "1434313440000", "1434312720000", "1434312960000", "1434314940000", "1434314640000", "1434314520000", "1434313020000", "1434313860000", "1434311880000", "1434312000000", "1434312660000", "1434314580000", "1434314100000", "1434312360000", "1434312540000", "1434312480000", "1434314820000", "1434312840000", "1434313740000", "1434312420000", "1434315180000", "1434314160000", "1434313920000", "1434314880000", "1434313320000", "1434312180000", "1434313560000", "1434314700000", "1434311940000", "1434315000000", "1434312240000", "1434313680000", "1434312300000", "1434313620000"],
        datasets: [
            {
                label: "My First dataset",
                fillColor: "rgba(220,220,220,0.2)",
                strokeColor: "rgba(220,220,220,1)",
                pointColor: "rgba(220,220,220,1)",
                pointStrokeColor: "#fff",
                pointHighlightFill: "#fff",
                pointHighlightStroke: "rgba(220,220,220,1)",
                data: [268, 276, 270, 273, 276, 262, 268, 274, 272, 273, 274, 273, 267, 273, 271, 274, 276, 268, 273, 276, 278, 275, 274, 277, 271, 270, 267, 270, 272, 275, 277, 277, 274, 261, 261, 265, 274, 275, 260, 266, 267, 273, 263, 271, 259, 272, 271, 276, 276, 274, 270, 272, 276, 264, 277, 265, 272, 265, 275]
            }
        ]
    };

    var myLineChart = new Chart(ctx).Line(data, {

        ///Boolean - Whether grid lines are shown across the chart
        scaleShowGridLines : true,

        //String - Colour of the grid lines
        scaleGridLineColor : "rgba(0,0,0,.05)",

        //Number - Width of the grid lines
        scaleGridLineWidth : 1,

        //Boolean - Whether to show horizontal lines (except X axis)
        scaleShowHorizontalLines: true,

        //Boolean - Whether to show vertical lines (except Y axis)
        scaleShowVerticalLines: true,

        //Boolean - Whether the line is curved between points
        bezierCurve : true,

        //Number - Tension of the bezier curve between points
        bezierCurveTension : 0.4,

        //Boolean - Whether to show a dot for each point
        pointDot : false,

        //Number - Radius of each point dot in pixels
        pointDotRadius : 4,

        //Number - Pixel width of point dot stroke
        pointDotStrokeWidth : 1,

        //Number - amount extra to add to the radius to cater for hit detection outside the drawn point
        pointHitDetectionRadius : 20,

        //Boolean - Whether to show a stroke for datasets
        datasetStroke : true,

        //Number - Pixel width of dataset stroke
        datasetStrokeWidth : 2,

        //Boolean - Whether to fill the dataset with a colour
        datasetFill : true,

        //String - A legend template
        legendTemplate : "<ul class=\"<%=name.toLowerCase()%>-legend\"><% for (var i=0; i<datasets.length; i++){%><li><span style=\"background-color:<%=datasets[i].strokeColor%>\"></span><%if(datasets[i].label){%><%=datasets[i].label%><%}%></li><%}%></ul>"

    });


});
