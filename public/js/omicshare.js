$(window).load(function () {

    "use strict";

    // Page Preloader
    $('#preloader').delay(350).fadeOut(function () {
        $('body').delay(350).css({'overflow': 'visible'});

    });
});
$('body').on('keyup', '.bar-search', function (event) {
    if (event.keyCode == "13") {
        $('.searchform i').click();
    }
});
$(".searchform i").click(function () {
    $(this).parent().submit();
});

$(document).ready(function () {

    "use strict";
// $(".mainpanel").css("min-height",document.body.offsetHeight);
    // Toggle Left Menu
    // $('.leftpanel .nav-parent > a').on('click', function () {
    //
    //     var parent = $(this).parent();
    //     var sub = parent.find('> ul');
    //
    //     // Dropdown works only when leftpanel is not collapsed
    //     if (!$('body').hasClass('leftpanel-collapsed')) {
    //         if (sub.is(':visible')) {
    //             sub.slideUp(200, function () {
    //                 parent.removeClass('nav-active');
    //                 $('.mainpanel').css({height: ''});
    //                 adjustmainpanelheight();
    //             });
    //         } else {
    //             closeVisibleSubMenu();
    //             parent.addClass('nav-active');
    //             sub.slideDown(200, function () {
    //                 adjustmainpanelheight();
    //             });
    //         }
    //     }
    //     return false;
    // });


    function closeVisibleSubMenu() {
        $('.leftpanel .nav-parent').each(function () {
            var t = $(this);
            if (t.hasClass('nav-active')) {
                t.find('> ul').slideUp(200, function () {
                    t.removeClass('nav-active');
                });
            }
        });
    }

    function adjustmainpanelheight() {
        // Adjust mainpanel height
        // var docHeight = $(document).height();
        // console.log(docHeight);
        // console.log($('.mainpanel').height());
        // if (docHeight > $('.mainpanel').height())
        //     $('.mainpanel').height(docHeight - 33);
        // console.log($('.mainpanel').height());
        // $('.mainpanel').height(1100)
        // console.log($('.mainpanel').height());
    }

    // adjustmainpanelheight();

    // Add class everytime a mouse pointer hover over it
    $('.nav-bracket > li').hover(function () {
        $(this).addClass('nav-hover');
    }, function () {
        $(this).removeClass('nav-hover');
    });

    // Menu Toggle
    $('.menutoggle').click(function () {

        var body = $('body');
        var bodypos = body.css('position');

        if (bodypos != 'relative') {

            if (!body.hasClass('leftpanel-collapsed')) {
                body.addClass('leftpanel-collapsed');
                $('.nav-bracket ul').attr('style', '');

                $(this).addClass('menu-collapsed');

            } else {
                body.removeClass('leftpanel-collapsed chat-view');
                $('.nav-bracket li.active ul').css({display: 'block'});

                $(this).removeClass('menu-collapsed');

            }
        } else {

            if (body.hasClass('leftpanel-show'))
                body.removeClass('leftpanel-show');
            else
                body.addClass('leftpanel-show');

            adjustmainpanelheight();
        }

    });
    $(".no-vip  ").hover(function () {
        $(".recharge-now").show();
    }, function () {
        $(".recharge-now").hide();
    });
    $(".head-hover-div").hover(function () {
        $(this).find(".head-menu-popup").show();

    }, function () {
        $(this).find(".head-menu-popup").hide();


    });
});