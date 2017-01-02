(function($) {
  Lumen.do_search_autofocus_fallback = function() {
    if (typeof Modernizer != "undefined") {
      if (Modernizr.autofocus) {
        return;
      }
    }

    $('input[autofocus]').focus();
  }

  Lumen.onLoad(function() {
    Lumen.do_search_autofocus_fallback();
  });
})(jQuery);