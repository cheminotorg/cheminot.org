var qstart = require('qstart');
var map = require('./map');
var cheminotm = require('./cheminotm');
var phone = require('./phone');

var stream;

sessionStorage.clear();

qstart.then(function() {

  phone.init();

  map.init();

  (function BackButton() {

    var backBtn = document.querySelector('.phone .back');

    backBtn.addEventListener('click', function() {

      phone.triggerBack();

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

          phone.unavailableDemo();

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

    var baseURL = 'http://' + Settings.domain;

    var endpoint = baseURL + '/cheminotm/trace';

    var stream = new EventSource(baseURL + '/cheminotm/trace');

    stream.onmessage = function(msg) {

      var data = JSON.parse(msg.data);

      if(data) {

        map.displayTrace(data);

      }
    };

    stream.onerror = function(event) {

      console.log(event);

    };

    return stream;
  };
});
