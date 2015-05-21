var phone = require('./phone');

var tracesLayers = {},
    tripsLayers = [],
    markers = {},
    lasttdsp,
    zoomControl,
    map,
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
};

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

    featureLayer.addTo(map);

    if(!tracesLayers[tdsp]) tracesLayers[tdsp] = [];

    tracesLayers[tdsp].push(featureLayer);

    lasttdsp = tdsp;

  }
};

exports.displayTrip = function(trip, tdsp) {

  if(map) {

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

    exports.clearTrips();

    exports.clearTraces();

    var tracesLayersByTdsp = tracesLayers[tdsp] || [];

    tracesLayersByTdsp.forEach(function(layer) {

      layer.addTo(map);

    });

    var featureLayer = L.mapbox.featureLayer(geojson);

    featureLayer.addTo(map);

    tripsLayers.push(featureLayer);

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

exports.removeMarker = function(stopId) {

  if(map) {

    exports.clearTrips();

    exports.clearTraces();

    var marker = markers[stopId];

    if(marker) {

      map.removeLayer(marker);

      delete markers[stopId];

      if(!Object.keys(markers).length) reset();

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

      map.fitBounds(group.getBounds(), { maxZoom: 10 });

    }

  }
};

function resetPosition() {

  if(map) {

    map.setView([46.822616668804926, 2.4884033203125], 7);

  }
}

function staticMap() {

  var baseURL = 'http://api.tiles.mapbox.com/v4/' + mapId;

  var position = '2.4884033203125,46.822616668804926,7';

  var size = document.body.scrollWidth + 'x' + document.body.scrollHeight + '.png';

  var url = [baseURL, position, size].join('/') + '?access_token=' + accessToken;

  var image = new Image();

  image.src = url;

  image.onload = function() {

    document.body.style.backgroundImage="url('" + url + "')";

  };

  image.onerror = function() {

    phone.unavailableDemo();

  };
};
