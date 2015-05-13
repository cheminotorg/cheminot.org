var qstart = require('qstart');
var map = require('./map');
var cheminotm = require('./cheminotm');

var stream;

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

        map.enableZoomControl();

        document.body.classList.add('playing');

        if(!stream) {

          stream = Stream();
        }
      }

      if(message.data.event == 'cheminot:init') {

        if(message.data.error == 'full') {

          var demoFull = document.querySelector('.phone .demo-full');

          demoFull.classList.add('on');

        }

      }

      if(message.data.event == 'cheminot:selecttrip') {

        map.displayTrip(message.data.trip, message.data.tdsp);

      }

      if(message.data.event == 'cheminot:selectstop') {

        cheminotm.getStop(message.data.stopId).then(function(stop) {

          map.addMarker(stop);

          map.fitMarkers();

        });

      }

      if(message.data.event == 'cheminot:resetstop') {

        map.removeMarker(message.data.stopId);

        map.fitMarkers();

      }
    }

  });

  (function StartDemo() {

    var startDemoBtn = document.querySelector('#demo .start-demo');

    startDemoBtn.addEventListener('click', function() {

      var phone = document.querySelector('.phone iframe');

      phone.contentWindow.location.href = "/cheminotm/app/index.html";

      var mask = document.querySelector('.phone .mask');

      mask.classList.add('off');

      startDemoBtn.classList.add('hidden');
    });

  })();

  (function StopDemo() {

    var stopDemoBtn = document.querySelector('.stop-demo');

    stopDemoBtn.addEventListener('click', function() {

      var mask = document.querySelector('.phone .mask');

      mask.classList.remove('off');

      document.body.classList.remove('playing');

      var startDemoBtn = document.querySelector('#demo .start-demo');

      startDemoBtn.classList.remove('hidden');

      map.disableZoomControl();
    });

  })();

  (function() {

    var todoList = document.querySelectorAll('input[type=checkbox]');

    for (var i = 0; i < todoList.length; i++) {

      var todo = todoList.item(i);

      todo.addEventListener('click', function(e) {

        e.preventDefault();

      });
    }

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

    return stream;
  };
});
