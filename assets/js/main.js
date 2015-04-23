var qstart = require('qstart');

qstart.then(function() {

  (function BackButton() {
    var backBtn = document.querySelector('.phone .back');
    backBtn.addEventListener('click', function() {
      var phone = document.querySelector('.phone iframe');
      if(phone.contentWindow.location.hash && phone.contentWindow.location.hash != '#/') {
        phone.contentWindow.history.back();
      }
    });
  })();
});
