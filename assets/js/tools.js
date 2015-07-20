exports.isMobile = function() {
  return /Android/i.test(navigator.userAgent) ||
    /iPhone/i.test(navigator.userAgent) ||
    /iPad/i.test(navigator.userAgent) ||
    /iPod/i.test(navigator.userAgent) ||
    /webOS/i.test(navigator.userAgent) ||
    /BlackBerry/i.test(navigator.userAgent) ||
    /Windows Phone/i.test(navigator.userAgent);
}

exports.isIE = function() {
  return /MSIE/i.test(navigator.userAgent);
}

exports.isSafari = function() {
  return /Safari/i.test(navigator.userAgent);
}

exports.isChrome = function() {
  return /Chrome/i.test(navigator.userAgent);
}
