$(document).ready(function() {
    // Facets stuff
//    if ($(".facet-container").length > 0) {
//        // hide() or show() the toggle containers on load
//        $(".facet-container").hide();
//
//        //Switch the "Open" and "Close" state per click then slide up/down (depending on open/close state)
//        $("h4.trigger").click(function() {
//            $(this).toggleClass("active").next().slideToggle("slow");
//            return false; //Prevent the browser jump to the link anchor
//        });
//
//        //Check to see if there are any active facets that need to be toggled to open
//        var toggles = $(document).find("h4.trigger");
//        $.each(toggles, function() {
//            if ($(this).hasClass("active")) {
//                $(this).next().css("display", "block").delay(2500).hide('slow');
//                $(this).removeClass("active");
//
//            }
//        })
//    }
//
//    if ($(".facet-container input[type=checkbox]").length > 0) {
//        $(".facet-container input[type=checkbox]").click(function() {
//            window.location.href = $(this).val();
//        })
//    }
//
//    // Scroll to first checked term
//    $(".facet-container").each(
//        function(i) {
//            $this = $(this);
//            $checked = $this.find("input:checked");
//            if ($checked.length) {
//                $this.animate({
//                    scrollTop: $checked.offset().top - $this.offset().top - $this.height() / 2
//                }, 1);
//            }
//    });

    $(".ic_container").capslide({
        caption_color    : 'white',
        caption_bgcolor  : 'black',
        overlay_bgcolor  : 'black',
        border           : '',
        showcaption      : true
    });
});