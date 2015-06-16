
var ticking = false;

function update() {
  var height = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
  var width = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);
  var wsize = (width * 100) / 1438;
  var hsize = (height * 100) / 802;
  var html = document.querySelector('html');
  html.style.fontSize = (wsize < hsize ? wsize : hsize) + '%';
  ticking = false;
}

function requestTick() {
  if(!ticking) {
    requestAnimationFrame(update);
  }
  ticking = true;
}

function onResize() {
  requestTick();
}

exports.init = function() {

  window.addEventListener('resize', onResize, false);

  onResize();

};
