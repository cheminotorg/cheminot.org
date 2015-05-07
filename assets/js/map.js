var layers = {},
    map,
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

    exports.clear(lasttdsp);

  }

  var featureLayer = L.mapbox.featureLayer(geojson);

  featureLayer.addTo(map);

  if(!layers[tdsp]) layers[tdsp] = [];

  layers[tdsp].push(featureLayer);

  lasttdsp = tdsp;
};

exports.displayTrip = function(trip) {

};

exports.clear = function(tdsp) {

  var layersByTdsp = [];

  if(tdsp) {

    layersByTdsp = layers[tdsp];

  } else {

    layersByTdsp = Object.keys(layers).reduce(function(acc, key) {

      return acc.concat(layers[key]);

    }, []);

  }

  layersByTdsp.forEach(function(layer) {

    map.removeLayer(layer);

  });
};
