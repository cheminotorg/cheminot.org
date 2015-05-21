var Qajax = require('qajax');

exports.getStop = function(id) {

  var url = Router.controllers.Cheminotm.stop(id).absoluteURL()

  return Qajax(url)
    .then(Qajax.filterSuccess)
    .then(function(response) {
      return JSON.parse(response.responseText);
    });
}

exports.abort = function() {

  var url = Router.controllers.Cheminotm.abort().absoluteURL()

  return Qajax({
    url: url,
    method: 'POST'
  }).then(Qajax.filterSuccess)
    .then(function(response) {
      return JSON.parse(response.responseText);
    });
}

exports.signout = function() {

  var url = Router.controllers.Cheminotm.signout().absoluteURL()

  return Qajax({
    url: url,
    method: 'POST'
  }).then(Qajax.filterSuccess)
    .then(function(response) {
      return JSON.parse(response.responseText);
    });
}
