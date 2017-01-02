
/* 
  The ajax_modal plugin can display some interactions inside a Bootstrap
  modal window, including some multi-page interactions. 

  It supports unobtrusive Javascript, where a link or form that would have caused
  a new page load is changed to display it's results inside a modal dialog,
  by this plugin.  The plugin assumes there is a Bootstrap modal div
  on the page with id #ajax-modal to use as the modal -- the standard Lumen
  layout provides this. 

  To make a link or form have their results display inside a modal, add
  `data-ajax-modal="trigger"` to the link or form. (Note, form itself not submit input)
  With Rails link_to helper, you'd do that like:

      link_to something, link, :data => {:ajax_modal => "trigger"}

  The results of the link href or form submit will be displayed inside
  a modal -- they should include the proper HTML markup for a bootstrap modal's
  contents. Also, you ordinarily won't want the Rails template with wrapping
  navigational elements to be used.  The Rails controller could suppress
  the layout when a JS AJAX request is detected, OR the response
  can include a `<div data-ajax-modal="container">` -- only the contents
  of the container will be placed inside the modal, the rest of the
  page will be ignored. 

  If you'd like to have a link or button that closes the modal,
  you can just add a `data-dismiss="modal"` to the link,
  standard Bootstrap convention. But you can also have
  an href on this link for non-JS contexts, we'll make sure
  inside the modal it closes the modal and the link is NOT followed. 

  Link or forms inside the modal will ordinarily cause page loads
  when they are triggered. However, if you'd like their results
  to stay within the modal, just add `data-ajax-modal="preserve"`
  to the link or form. 

  Here's an example of what might be returned, demonstrating most of the devices available:

    <div data-ajax-modal="container">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
        <h3 class="modal-title">Request Placed</h3>
      </div>
  
      <div class="modal-body">
        <p>Some message</p>
        <%= link_to "This result will still be within modal", some_link, :data => {:ajax_modal => "preserve"} %>
      </div>


      <div class="modal-footer">
        <%= link_to "Close the modal", request_done_path, :class => "submit button dialog-close", :data => {:dismiss => "modal"} %>
      </div>
    </div>


  One additional feature. If the content returned from the AJAX modal load
  has an element with `data-ajax-modal=close`, that will trigger the modal
  to be closed. And if this element includes a node with class "flash_messages",
  the flash-messages node will be added to the main page inside #main-flahses. 

  == Events

  We'll send out an event 'loaded.Lumen.ajax-modal' with the #ajax-modal
  dialog as the target, right after content is loaded into the modal but before
  it is shown (if not already a shown modal).  In an event handler, you can 
  inspect loaded content by looking inside $(this).  If you call event.preventDefault(),
  we won't 'show' the dialog (although it may already have been shown, you may want to
  $(this).modal("hide") if you want to ensure hidden/closed. 

  The data-ajax-modal=close behavior is implemented with this event, see for example. 
*/

// We keep all our data in Lumen.ajaxModal object.
// Create lazily if someone else created first. 
if (Lumen.ajaxModal === undefined) {
  Lumen.ajaxModal = {};
}


// a Bootstrap modal div that should be already on the page hidden
Lumen.ajaxModal.modalSelector = "#ajax-modal";

// Trigger selectors identify forms or hyperlinks that should open
// inside a modal dialog.
Lumen.ajaxModal.triggerLinkSelector  = "a[data-ajax-modal~=trigger], a.lightboxLink,a.more_facets_link,.ajax_modal_launch";
Lumen.ajaxModal.triggerFormSelector  = "form[data-ajax-modal~=trigger], form.ajax_form";

// preserve selectors identify forms or hyperlinks that, if activated already
// inside a modal dialog, should have destinations remain inside the modal -- but
// won't trigger a modal if not already in one.
//
// No need to repeat selectors from trigger selectors, those will already
// be preserved. MUST be manually prefixed with the modal selector,
// so they only apply to things inside a modal.
Lumen.ajaxModal.preserveLinkSelector = Lumen.ajaxModal.modalSelector + ' a[data-ajax-modal~=preserve]';
Lumen.ajaxModal.preserveFormSelector = Lumen.ajaxModal.modalSelector + ' form[data-ajax-modal~=preserve]'

Lumen.ajaxModal.containerSelector    = "[data-ajax-modal~=container]";

Lumen.ajaxModal.modalCloseSelector   = "[data-ajax-modal~=close], span.ajax-close-modal";

// Called on fatal failure of ajax load, function returns content
// to show to user in modal.  Right now called only for extreme
// network errors. 
Lumen.ajaxModal.onFailure = function(data) {
  var contents =  "<div class='modal-header'>" +
           "<button type='button' class='close' data-dismiss='modal' aria-hidden='true'>×</button>" +
           "Network Error</div>";
  $(Lumen.ajaxModal.modalSelector).find('.modal-content').html(contents);
  $(Lumen.ajaxModal.modalSelector).modal('show');
}

/**
  * jQuery seems to have changed so that the always() callback
  * receives the response body string, rather than a data structure
  */
Lumen.ajaxModal.receiveAjax = function (data) {
      if (!data) {
        // Network error, could not contact server. 
        Lumen.ajaxModal.onFailure(data)
      }
      else {
        var contents = data;

        // does it have a data- selector for container?
        // important we don't execute script tags, we shouldn't. 
        // code modelled off of JQuery ajax.load. https://github.com/jquery/jquery/blob/master/src/ajax/load.js?source=c#L62
        var container =  $("<div>").
          append( jQuery.parseHTML(contents) ).find( Lumen.ajaxModal.containerSelector ).first();
        if (container.length !== 0) {
          contents = container.html();
        }

        $(Lumen.ajaxModal.modalSelector).find('.modal-content').html(contents);

        // send custom event with the modal dialog div as the target
        var e    = $.Event('loaded.Lumen.ajax-modal')
        $(Lumen.ajaxModal.modalSelector).trigger(e);
        // if they did preventDefault, don't show the dialog
        if (e.isDefaultPrevented()) return;

        $(Lumen.ajaxModal.modalSelector).modal('show');
      }
};


Lumen.ajaxModal.modalAjaxLinkClick = function(e) {
  e.preventDefault();

  var jqxhr = $.ajax({
    url: $(this).attr('href'),
    dataType: 'html'
  });

  jqxhr.always( Lumen.ajaxModal.receiveAjax );
};

Lumen.ajaxModal.modalAjaxFormSubmit = function(e) {
  e.preventDefault();

  var jqxhr = $.ajax({
    url: $(this).attr('action'),
    data: $(this).serialize(),
    type: $(this).attr('method'), //POST',
    dataType: 'script'
 });

 jqxhr.always(Lumen.ajaxModal.receiveAjax);
}



Lumen.ajaxModal.setup_modal = function() {
	// Event indicating Lumen is setting up a modal link,
  // you can catch it and call e.preventDefault() to abort
  // setup. 
	var e = $.Event('setup.Lumen.ajax-modal');
	$("body").trigger(e);
	if (e.isDefaultPrevented()) return;

  // Register both trigger and preserve selectors in ONE event handler, combining
  // into one selector with a comma, so if something matches BOTH selectors, it
  // still only gets the event handler called once. 
  $("body").on("click", Lumen.ajaxModal.triggerLinkSelector  + ", " + Lumen.ajaxModal.preserveLinkSelector,
    Lumen.ajaxModal.modalAjaxLinkClick);
  $("body").on("submit", Lumen.ajaxModal.triggerFormSelector + ", " + Lumen.ajaxModal.preserveFormSelector,
    Lumen.ajaxModal.modalAjaxFormSubmit);

  // Catch our own custom loaded event to implement data-ajax-modal=closed
  $("body").on("loaded.Lumen.ajax-modal", Lumen.ajaxModal.check_close_ajax_modal);

  // we support doing data-dismiss=modal on a <a> with a href for non-ajax
  // use, we need to suppress following the a's href that's there for
  // non-JS contexts. 
  $("body ").on("click", Lumen.ajaxModal.modalSelector + " a[data-dismiss~=modal]", function (e) {
    e.preventDefault();
  });
};

// A function used as an event handler on loaded.Lumen.ajax-modal
// to catch contained data-ajax-modal=closed directions
Lumen.ajaxModal.check_close_ajax_modal = function(event) {
  if ($(event.target).find(Lumen.ajaxModal.modalCloseSelector).length) {
    modal_flashes = $(this).find('.flash_messages');

    $(event.target).modal("hide");
    event.preventDefault();

    main_flashes = $('#main-flashes');
    main_flashes.append(modal_flashes);
    modal_flashes.fadeIn(500);  
  }
}

Lumen.onLoad(function() {
  Lumen.ajaxModal.setup_modal();
});
