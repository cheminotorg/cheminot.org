var Qajax = require('qajax');

exports.getStop = function(id) {

  return Qajax('/cheminotm/stop/' + id)
    .then(Qajax.filterSuccess)
    .then(function(response) {
      return JSON.parse(response.responseText);
    });
}
