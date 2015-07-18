var phone = require('./phone');

var tracesLayers = {},
    tripsLayers = [],
    markers = {},
    zoomControl;

var lasttdsp,
    selectedtdsp;

var map,
    mapId = 'srenault.ljcc52c6',
    accessToken = 'pk.eyJ1Ijoic3JlbmF1bHQiLCJhIjoiNGRHRzgxWSJ9.pawb4Qw10gD_8dbE-_Qrvw';

exports.init = function() {

  if(window.L) {

    L.mapbox.accessToken = accessToken;

    map = L.mapbox.map('map', mapId, { zoomControl: false });

    resetPosition();

  } else {

    staticMap();

  }

  bindLegend();
};

exports.reset = function() {

  if(map) {

    exports.clearTrips();

    exports.clearTraces();

    exports.clearMarkers();

    resetPosition();

  }
};

function showTrace() {

  var layers = (tracesLayers[selectedtdsp] || []).reduce(function(acc, layers) {

    return acc.concat(layers);

  }, []);

  layers.forEach(function(layer) {

    layer.addTo(map);

  });

}

exports.displayTrace = function(trace) {

  if(map) {

    var tdsp;

    var features = trace.map(function(vertice) {

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
          'marker-shape': "pin",
          'marker-size': "small",
          'marker-symbol': "embassy",
          "stroke-width": 1
        }
      };
    });

    var geojson = {
      type: "FeatureCollection",
      features: features
    };

    if(lasttdsp != tdsp) {

      exports.clearTraces();

    }

    var featureLayer = L.mapbox.featureLayer(geojson);

    if(isDisplayTrace() && !selectedtdsp) featureLayer.addTo(map);

    if(!tracesLayers[tdsp]) tracesLayers[tdsp] = [];

    tracesLayers[tdsp].push(featureLayer);

    lasttdsp = tdsp;
  }
};

function showTrip() {

  tripsLayers.forEach(function(layer) {

    layer.addTo(map);

  });
}

exports.displayTrip = function(trip, tdsp) {

  if(map) {

    var checkbox = document.querySelector('#map-legend .best-trip input[type=checkbox]');

    checkbox.checked = true;

    exports.clearTrips();

    exports.clearTraces();

    var tripId;

    var features = trip.map(function(stopTime) {

      tripId = stopTime.tripId;

      return {
        type: "Feature",
        geometry: {
          type: "Point",
          coordinates: [stopTime.lng, stopTime.lat]
        },
        properties: {
          title: stopTime.stopName,
          'marker-color': '#e9683e',
          'marker-shape': "pin",
          'marker-size': "medium",
          'marker-symbol': "rail-light",
          "stroke-width": 1
        }
      };

    });

    var geojson = {
      type: "FeatureCollection",
      features: features
    };

    var coordinates = trip.reduce(function(acc, stopTime) {
      acc.push(L.latLng(stopTime.lat, stopTime.lng));
      return acc;
    }, []);


    var tracesLayersByTdsp = tracesLayers[tdsp] || [];

    if(isDisplayTrace()) {

      tracesLayersByTdsp.forEach(function(layer) {

        layer.addTo(map);

      });

    }

    var line = L.polyline(coordinates, { color: '#e9683e' });

    if(isDisplayTrip()) {

      line.addTo(map);

    }

    var featureLayer = L.mapbox.featureLayer(geojson);

    if(isDisplayTrip()) featureLayer.addTo(map);

    tripsLayers.push(featureLayer);

    tripsLayers.push(line);

    selectedtdsp = tdsp;
  }
};

exports.clearTraces = function() {

  if(map) {

    var layersByTdsp = [];

    layersByTdsp = Object.keys(tracesLayers).reduce(function(acc, key) {

      return acc.concat(tracesLayers[key]);

    }, []);

    layersByTdsp.forEach(function(layer) {

      map.removeLayer(layer);

    });

  }
};

exports.clearTrips = function() {

  if(map) {

    tripsLayers.forEach(function(layer) {

      map.removeLayer(layer);

    });

  }
};

exports.enableControls = function() {

  if(map) {

    if(!zoomControl) {

      zoomControl = L.control.zoom();

      map.addControl(zoomControl);

    }

  }
};

exports.disableControls = function() {

  if(map) {

    if(zoomControl) map.removeControl(zoomControl);

  }
};

exports.addMarker = function(stop) {

  if(map) {

    var marker = L.marker([stop.lat, stop.lng], { title: stop.name });

    marker.addTo(map);

    markers[stop.id] = marker;

  }
};

exports.clearMarkers = function() {

  if(map) {

    Object.keys(markers).forEach(exports.removeMarker);
  }
};

exports.removeMarker = function(stopId) {

  if(map) {

    exports.clearTrips();

    exports.clearTraces();

    var marker = markers[stopId];

    if(marker) {

      map.removeLayer(marker);

      delete markers[stopId];

      if(!Object.keys(markers).length) resetPosition();

    }

  }
};

exports.fitMarkers = function() {

  if(map) {

    var pins = Object.keys(markers).map(function(stopId) {

      return markers[stopId];

    });

    if(pins.length) {

      var group = new L.featureGroup(pins);

      map.fitBounds(group.getBounds(), { maxZoom: 8 });

    }

  }
};

exports.disableTrace = function() {

  var checkbox = document.querySelector('#map-legend .algorithm input[type=checkbox]');

  checkbox.checked = false;

  exports.clearTraces();
};

exports.enableTrace = function() {

  selectedtdsp = null;

  var checkbox = document.querySelector('#map-legend .algorithm input[type=checkbox]');

  checkbox.checked = true;
};

function resetPosition() {

  if(map) {

    map.setView([46.822616668804926, 2.4884033203125], 7);

  }
}

function staticMap() {

  var baseURL = 'http://api.tiles.mapbox.com/v4/' + mapId;

  var position = '2.4884033203125,46.822616668804926,7';

  var height = document.body.scrollHeight > 1280 ?  1280 : document.body.scrollHeight;

  var width = document.body.scrollWidth > 1280 ?  1280 : document.body.scrollWidth;

  var size = width + 'x' + height + '.png';

  var url = [baseURL, position, size].join('/') + '?access_token=' + accessToken;

  var image = new Image();

  image.src = url;

  image.onload = function() {

    document.body.style.backgroundImage="url('" + url + "')";

  };

  image.onerror = function() {

    if(Settings.mode != "Dev") {

      phone.unavailableDemo();

    }

  };
};

function bindLegend() {

  var filters = document.querySelectorAll('#map-legend input[type=checkbox]');

  for (var i = 0; i < filters.length; i++) {

    var filter = filters.item(i);

    filter.addEventListener('click', function(e) {

      var checkbox = e.currentTarget;

      var li = checkbox.parentElement;

      if(checkbox.checked) {

        if(li.classList.contains('algorithm')) {

          showTrace();

        } else {

          showTrip();

        }
      } else {

        if(li.classList.contains('algorithm')) {

          exports.clearTraces();

        } else {

          exports.clearTrips();

        }

      }
    });
  }
}

function isDisplayTrip() {

  var filter = document.querySelector('#map-legend .best-trip input[type=checkbox]');

  return filter.checked;
}

function isDisplayTrace() {

  var filter = document.querySelector('#map-legend .algorithm input[type=checkbox]');

  return filter.checked;
}
