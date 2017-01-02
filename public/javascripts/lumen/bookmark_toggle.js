(function($) {
//change form submit toggle to checkbox
    Lumen.do_bookmark_toggle_behavior = function() {
      $(Lumen.do_bookmark_toggle_behavior.selector).bl_checkbox_submit({
         //css_class is added to elements added, plus used for id base
         css_class: "toggle_bookmark",
         success: function(checked, response) {
           if (response.bookmarks) {
             $('[data-role=bookmark-counter]').text(response.bookmarks.count);
           }
         }
      });
    };
    Lumen.do_bookmark_toggle_behavior.selector = "form.bookmark_toggle";

Lumen.onLoad(function() {
  Lumen.do_bookmark_toggle_behavior();
});
  

})(jQuery);
