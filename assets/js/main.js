var qstart = require('qstart'),
    map = require('./map'),
    cheminotm = require('./cheminotm'),
    phone = require('./phone'),
    responsive = require('./responsive'),
    analytics = require('./analytics');

var stream;

responsive.init();

qstart.then(function() {

  var ua = window.navigator.userAgent,
      isSafari = ua.indexOf("Safari") >= 0,
      isChrome = ua.indexOf("Chrome") >= 0,
      isIE = ua.indexOf("MSIE") >= 0;

  map.init();

  if(!isIE && !(!isChrome && isSafari)) {

    sessionStorage.clear();

    phone.init();

    window.addEventListener("message", function(message) {

      if(message.data && message.origin == window.location.origin) {

        if(message.data.event == 'cheminot:ready') {

          map.enableControls();

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

          map.disableTrace();

          map.displayTrip(message.data.trip, message.data.tdsp);

        }

        if(message.data.event == 'cheminot:unselecttrip') {

          map.clearTrips();

          map.clearTraces();

          map.enableTrace();

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

        if(message.data.event == 'cheminot:abort') {

        }
      }

    });

    (function() {

      var androidStore = document.querySelectorAll('.android-store');

      for(var i=0; i<androidStore.length; i++) {

        var link = androidStore.item(i);

        link.addEventListener('click', function(e) {

          e.preventDefault();

          analytics.trackAndroidStore().fin(function() {

            window.location.href = link.getAttribute('href');

          });

        });
      }

    })();

    (function() {

      var todoList = document.querySelectorAll('.roadmap input[type=checkbox]');

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

  } else {

    phone.notSupportedBrowser();

  }
});
