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
        labels: [0, 6, 12, 18, 24, 30, 36, 42, 48, 54, 60, 66, 72, 78, 84, 90, 96, 102, 108, 114, 120, 126, 132, 138, 144, 150, 156, 162, 168, 174, 180, 186, 192, 198, 204, 210, 216, 222, 228, 234, 240, 246, 252, 258, 264, 270, 276, 282, 288, 294, 300, 306, 312, 318, 324, 330, 336, 342, 348],
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
