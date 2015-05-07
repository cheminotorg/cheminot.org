var tracesLayers = {},
    tripsLayers = [],
    map,
    lasttrip,
    lasttdsp;

if(window.L) {

  L.mapbox.accessToken = 'pk.eyJ1Ijoic3JlbmF1bHQiLCJhIjoiNGRHRzgxWSJ9.pawb4Qw10gD_8dbE-_Qrvw';

  map = L.mapbox.map('map', 'srenault.ljcc52c6', { zoomControl: false })
                .setView([46.822616668804926, 2.4884033203125], 7);
}

exports.displayTrace = function (trace) {

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

  if(lasttdsp && lasttdsp != tdsp) {

    exports.clearTrace(lasttdsp);

  }

  var featureLayer = L.mapbox.featureLayer(geojson);

  featureLayer.addTo(map);

  if(!tracesLayers[tdsp]) tracesLayers[tdsp] = [];

  tracesLayers[tdsp].push(featureLayer);

  lasttdsp = tdsp;
};

exports.displayTrip = function(trip) {

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

  if(lasttdsp && lasttrip != tripId) {

    exports.clearTrips(tripId);

  }

  var featureLayer = L.mapbox.featureLayer(geojson);

  featureLayer.addTo(map);

  tripsLayers.push(featureLayer);

  lasttdsp = tripId;

};

exports.clearTrace = function(tdsp) {

  var layersByTdsp = [];

  if(tdsp) {

    layersByTdsp = tracesLayers[tdsp];

  } else {

    layersByTdsp = Object.keys(tracesLayers).reduce(function(acc, key) {

      return acc.concat(tracesLayers[key]);

    }, []);

  }

  layersByTdsp.forEach(function(layer) {

    map.removeLayer(layer);

  });
};

exports.clearTrips = function() {

  tripsLayers.forEach(function(layer) {

    map.removeLayer(layer);

  });
};
