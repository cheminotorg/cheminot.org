var Q = require('q');

function triggerEvent(event) {

  var deferred = Q.defer();

  if(window.ga) {

    window.setTimeout(function() {

      deferred.resolve();

    }, 1000);

    ga('send', 'event', event, 'successful', {

      'hitCallback': function() {

        deferred.resolve();

      }
    });

  } else {

    deferred.resolve();

  }

  return deferred.promise;
}

exports.trackStartDemo = function() {

  return triggerEvent('start-demo');

};

exports.trackAndroidStore = function() {

  return triggerEvent('android-store');

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
