var map = require('./map'),
    cheminotm = require('./cheminotm'),
    analytics = require('./analytics');

exports.init = function() {

  bindStopDemo();

  bindStartDemo();

  bindBackButton();

};

exports.unavailableDemo = function() {

  var demoFull = document.querySelector('.phone .unavailable-demo');

  demoFull.classList.add('on');

};

function isDemoAvailable() {

  return !document.querySelector('.phone .unavailable-demo.on');

}

function bindStartDemo() {

  var startDemoBtn = document.querySelector('#demo .start-demo'),
      phone = document.querySelector('#demo .phone');

  phone.addEventListener('click', function() {

    if(isDemoAvailable() && !document.body.classList.contains('playing')) {

      analytics.trackStartDemo();

      var iframe = document.querySelector('.phone iframe');

      iframe.contentWindow.location.href = "/cheminotm/app/index.html";

      var mask = document.querySelector('.phone .mask');

      mask.classList.add('off');

      startDemoBtn.classList.add('hidden');
    }
  });

};

function bindStopDemo() {

  var stopDemoBtn = document.querySelector('.stop-demo');

  stopDemoBtn.addEventListener('click', function() {

    cheminotm.abort();

    cheminotm.signout();

    var mask = document.querySelector('.phone .mask');

    mask.classList.remove('off');

    document.body.classList.remove('playing');

    var startDemoBtn = document.querySelector('#demo .start-demo');

    startDemoBtn.classList.remove('hidden');

    map.disableControls();

    map.reset();
  });

};

function bindBackButton() {

  var backBtn = document.querySelector('.phone .back');

  backBtn.addEventListener('click', function(e) {

    e.preventDefault();

    e.stopPropagation();

    var phone = document.querySelector('.phone iframe');

    phone.contentWindow.postMessage({ event: 'cheminot:back' }, window.location.origin);

  });
}
