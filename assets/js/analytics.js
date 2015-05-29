var Q = require('q');

function triggerEvent(event) {

  var deferred = Q.defer();

  window.setTimeout(function() {

    deferred.resolve();

  }, 1000);

  ga('send', 'event', event, 'successful', {

    'hitCallback': function() {

      deferred.resolve();

    }
  });

  return deferred.promise;
}

exports.trackStartDemo = function() {

  return triggerEvent('start-demo');

};

exports.trackDownloadBeta = function() {

  return triggerEvent('download-beta');

};

exports.trackGooglePlus = function() {

  return triggerEvent('googleplus');

};

exports.trackGithub = function() {

  return triggerEvent('github');

};

exports.trackTwitter = function() {

  return triggerEvent('twitter');

};
