var tracesLayers = {},
    tripsLayers = [],
    markers = {},
    lasttdsp,
    zoomControl,
    map;

if(window.L) {

  L.mapbox.accessToken = 'pk.eyJ1Ijoic3JlbmF1bHQiLCJhIjoiNGRHRzgxWSJ9.pawb4Qw10gD_8dbE-_Qrvw';

  map = L.mapbox.map('map', 'srenault.ljcc52c6', { zoomControl: false })

  reset();
}

function reset() {
  map.setView([46.822616668804926, 2.4884033203125], 7);
}

exports.displayTrace = function(trace) {

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
};

exports.displayTrip = function(trip, tdsp) {

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
};

exports.clearTraces = function() {

  var layersByTdsp = [];

  layersByTdsp = Object.keys(tracesLayers).reduce(function(acc, key) {

    return acc.concat(tracesLayers[key]);

  }, []);

  layersByTdsp.forEach(function(layer) {

    map.removeLayer(layer);

  });
};

exports.clearTrips = function() {

  tripsLayers.forEach(function(layer) {

    map.removeLayer(layer);

  });
};

exports.enableZoomControl = function() {

  if(window.L && !zoomControl) {

    zoomControl = L.control.zoom();

    map.addControl(zoomControl);

  }
}

exports.disableZoomControl = function() {

  if(zoomControl) {

    map.removeControl(zoomControl);

  }
}

exports.addMarker = function(stop) {

  var marker = L.marker([stop.lat, stop.lng], { title: stop.name });

  marker.addTo(map);

  markers[stop.id] = marker;
}

exports.removeMarker = function(stopId) {

  var marker = markers[stopId];

  if(marker) {

    map.removeLayer(marker);

    delete markers[stopId];

    if(!Object.keys(markers).length) reset();

  }
}

exports.fitMarkers = function() {

  var pins = Object.keys(markers).map(function(stopId) {

    return markers[stopId];

  });

  if(pins.length) {

    var group = new L.featureGroup(pins);

    map.fitBounds(group.getBounds(), { maxZoom: 10 });

  }
}
