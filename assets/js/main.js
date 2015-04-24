var qstart = require('qstart');

qstart.then(function() {

  (function BackButton() {

    var backBtn = document.querySelector('.phone .back');

    backBtn.addEventListener('click', function() {

      var phone = document.querySelector('.phone iframe');

      phone.contentWindow.postMessage('onbackbutton', window.location.href);
    });

  })();

  (function Map() {

    var baseURL = 'http://localhost:9000';

    L.mapbox.accessToken = 'pk.eyJ1Ijoic3JlbmF1bHQiLCJhIjoiNGRHRzgxWSJ9.pawb4Qw10gD_8dbE-_Qrvw';

    var map = L.mapbox.map('map', 'srenault.ljcc52c6').setView([48.858859, 2.3470599], 9);

    var endpoint = baseURL + '/cheminotm/trace';

    var stream = new EventSource(baseURL + '/cheminotm/trace');

    stream.onmessage = function(msg) {
      var data = JSON.parse(msg.data);

      data.forEach(function(vertice) {

        var marker = L.marker([vertice.lat, vertice.lng], {

          title: vertice.name,

          riseOnHover: true

        });

        marker.addTo(map);
      });
    };

    stream.onerror = function(event) {
      console.log(event);
    };

  })();

});
