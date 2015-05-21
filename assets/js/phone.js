var map = require('./map');

exports.init = function() {

  bindStopDemo();

  bindStartDemo();

};

exports.unavailableDemo = function() {

  var demoFull = document.querySelector('.phone .unavailable-demo');

  demoFull.classList.add('on');

};

exports.triggerBack = function() {

  var phone = document.querySelector('.phone iframe');

  phone.contentWindow.postMessage({ event: 'cheminot:back' }, window.location.origin);

};

function isDemoAvailable() {

  return !document.querySelector('.phone .unavailable-demo.on');

}

function bindStartDemo() {

  var startDemoBtn = document.querySelector('#demo .start-demo'),
      phone = document.querySelector('#demo .phone');

  phone.addEventListener('click', function() {

    if(isDemoAvailable()) {

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

    var mask = document.querySelector('.phone .mask');

    mask.classList.remove('off');

    document.body.classList.remove('playing');

    var startDemoBtn = document.querySelector('#demo .start-demo');

    startDemoBtn.classList.remove('hidden');

    map.disableControls();

    map.reset();
  });

};
