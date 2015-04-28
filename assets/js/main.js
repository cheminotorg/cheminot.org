var qstart = require('qstart');

qstart.then(function() {

  (function BackButton() {

    var backBtn = document.querySelector('.phone .back');

    backBtn.addEventListener('click', function() {

      var phone = document.querySelector('.phone iframe');

      phone.contentWindow.postMessage('cheminotm.backbutton', window.location.origin);

    });

  })();

  (function StartDemo() {

    var startDemoBtn = document.querySelector('.phone .start-demo');

    startDemoBtn.addEventListener('click', function() {

      var phone = document.querySelector('.phone iframe');

      phone.contentWindow.location.href = "/cheminotm/app/index.html";

      var mask = document.querySelector('.phone .mask');

      mask.classList.add('off');

      startDemoBtn.remove();

    });

  })();

  (function Map() {

    if(window.L) {

      L.mapbox.accessToken = 'pk.eyJ1Ijoic3JlbmF1bHQiLCJhIjoiNGRHRzgxWSJ9.pawb4Qw10gD_8dbE-_Qrvw';

      var map = L.mapbox.map('map', 'srenault.ljcc52c6', { zoomControl: false }).setView([46.822616668804926, 2.4884033203125], 7);
    }

    var baseURL = 'http://localhost:9000';

    var endpoint = baseURL + '/cheminotm/trace';

    var stream = new EventSource(baseURL + '/cheminotm/trace');

    var layers = {};

    var lasttdsp;

    stream.onmessage = function(msg) {

      var data = JSON.parse(msg.data);

      console.log(data);

      if(data && window.L) {

        var tdsp;

        var features = data.map(function(vertice) {

          tdsp = vertice.tdsp;

          return {
            type: "Feature",
            geometry: {
              type: "Point",
              coordinates: [vertice.lng, vertice.lat]
            },
            properties: {
              title: vertice.name,
              'marker-color': '#548cba',
              "marker-shape": "pin"
            }
          };
        });

        var geojson = {
          type: "FeatureCollection",
          features: features
        };

        if(lasttdsp && lasttdsp != tdsp) {

          layers[lasttdsp].forEach(function(layer) {

            map.removeLayer(layer);

          });
        }

        var featureLayer = L.mapbox.featureLayer(geojson);

        featureLayer.addTo(map);

        if(!layers[tdsp]) layers[tdsp] = [];

        layers[tdsp].push(featureLayer);

        lasttdsp = tdsp;
      }
    };

    stream.onerror = function(event) {
      console.log(event);
    };

  })();

});
