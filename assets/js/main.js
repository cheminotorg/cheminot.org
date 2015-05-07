var qstart = require('qstart');
var map = require('./map');

console.log(map);

qstart.then(function() {

  (function BackButton() {

    var backBtn = document.querySelector('.phone .back');

    backBtn.addEventListener('click', function() {

      var phone = document.querySelector('.phone iframe');

      phone.contentWindow.postMessage({ event: 'cheminot:back' }, window.location.origin);

    });

  })();

  window.addEventListener("message", function(message) {

    if(message.data && message.origin == window.location.origin) {

      if(message.data.event == 'cheminot:ready') {

        Stream();

        var screen = document.querySelector('.phone .screen');

        screen.classList.add('loaded');
      }

      if(message.data.event == 'cheminot:init') {

        if(message.data.error == 'full') {

          var demoFull = document.querySelector('.phone .demo-full');

          demoFull.classList.add('on');

        }

      }

      if(message.data.event == 'cheminot:selecttrip') {

        map.displayTrip(message.data.trip);

      }

    }

  });

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

  function Stream() {

    var baseURL = 'http://localhost:9000';

    var endpoint = baseURL + '/cheminotm/trace';

    var stream = new EventSource(baseURL + '/cheminotm/trace');

    stream.onmessage = function(msg) {

      var data = JSON.parse(msg.data);

      if(data && window.L) {

        map.displayTrace(data);

      }
    };

    stream.onerror = function(event) {

      console.log(event);

    };

  };

});
