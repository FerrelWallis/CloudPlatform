$(function () {
    let resize = $(".handler")[0];
    let left = $(".mysoft-content-left")[0];
    let right = $(".mysoft-content-right")[0];
    let box = $(".mysoft-content")[0];

    function mousedown_fun(e) {
        var startX = e.clientX;
        resize.left = resize.offsetLeft;
        document.onmousemove = function (e) {
            var endX = e.clientX;
            var moveLen = resize.left + (endX - startX);
            adjustWidth(moveLen);
        };
        document.onmouseup = function (evt) {
            document.onmousemove = null;
            document.onmouseup = null;
            resize.releaseCapture && resize.releaseCapture();
        };
        resize.setCapture && resize.setCapture();
        return false;
    }

    function touchstart_fun(e) {
        e = e.touches[0];
        let startX = e.clientX;
        resize.left = resize.offsetLeft;
        $("*").css("touch-action", "pan-y");
        document.ontouchmove = function (e) {
            $("*").css("touch-action", "pan-y");
            e = e.touches[0];
            let endX = e.clientX;
            let moveLen = resize.left + (endX - startX);
            adjustWidth(moveLen);
        };
        document.ontouchend = function (evt) {
            $("*").css("touch-action", "auto");
            document.ontouchmove = null;
            document.ontouchend = null;
            resize.releaseCapture && resize.releaseCapture();
        };
        resize.setCapture && resize.setCapture();
        return false;
    }

    function adjustWidth(moveLen) {
        let minLeftWidth = 530;//左边最小宽度
        let minRightWidth = 450;//左边最小宽度
        if (moveLen < minLeftWidth) moveLen = minLeftWidth;
        let maxT = box.clientWidth - resize.offsetWidth; //可用宽度
        if (moveLen < minLeftWidth) moveLen = minLeftWidth;
        if (maxT - moveLen < minRightWidth) moveLen = maxT - minRightWidth;
        resize.style.left = moveLen;
        left.style.width = moveLen + "px";
        right.style.width = (box.clientWidth - moveLen - 35) + "px";
    }

    let moveLen2 = left.clientWidth;
    let minLeftWidth = getlongestWidth() + 80;
    if (moveLen2 < minLeftWidth) moveLen2 = minLeftWidth;
    adjustWidth(moveLen2);
    $(window).resize(function () {
        adjustWidth(left.clientWidth);
    });
    resize.addEventListener('mousedown', mousedown_fun);
    resize.addEventListener('touchstart', touchstart_fun);
});

function getlongestWidth() {
    let longestWidth = 450;
    $(".mysoft-content-left .one-line").each(function () {
        let oneLineWidth = 0;
        $(this).children().each(function () {
            oneLineWidth += $(this).outerWidth(true);
        });
        if (oneLineWidth > longestWidth) {
            longestWidth = oneLineWidth;
        }
    });
    return longestWidth;
}

// $(function () {
//     let resize = $(".handler")[0];
//     let left = $(".mysoft-content-left")[0];
//     let right = $(".mysoft-content-right")[0];
//     let box = $(".mysoft-content")[0];
//
//     function mousedown_fun(e) {
//         var startX = e.clientX;
//         resize.left = resize.offsetLeft;
//         document.onmousemove = function (e) {
//             var endX = e.clientX;
//             var moveLen = resize.left + (endX - startX);
//             adjustWidth(moveLen);
//         };
//         document.onmouseup = function (evt) {
//             document.onmousemove = null;
//             document.onmouseup = null;
//             resize.releaseCapture && resize.releaseCapture();
//         };
//         resize.setCapture && resize.setCapture();
//         return false;
//     }
//
//     function touchstart_fun(e) {
//         e = e.touches[0];
//         let startX = e.clientX;
//         resize.left = resize.offsetLeft;
//         $("*").css("touch-action", "pan-y");
//         document.ontouchmove = function (e) {
//             $("*").css("touch-action", "pan-y");
//             e = e.touches[0];
//             let endX = e.clientX;
//             let moveLen = resize.left + (endX - startX);
//             adjustWidth(moveLen);
//         };
//         document.ontouchend = function (evt) {
//             $("*").css("touch-action", "auto");
//             document.ontouchmove = null;
//             document.ontouchend = null;
//             resize.releaseCapture && resize.releaseCapture();
//         };
//         resize.setCapture && resize.setCapture();
//         return false;
//     }
//
//     function adjustWidth(moveLen) {
//         let minLeftWidth = 450;//左边最小宽度
//         let minRightWidth = 300;//左边最小宽度
//         if (moveLen < minLeftWidth) moveLen = minLeftWidth;
//         let maxT = box.clientWidth - resize.offsetWidth; //可用宽度
//         if (moveLen < minLeftWidth) moveLen = minLeftWidth;
//         if (maxT - moveLen < minRightWidth) moveLen = maxT - minRightWidth;
//         resize.style.left = moveLen;
//         left.style.width = moveLen + "px";
//         right.style.width = (box.clientWidth - moveLen - 35) + "px";
//     }
//
//     let moveLen2 = left.clientWidth;
//     let minLeftWidth = getlongestWidth() + 80;
//     if (moveLen2 < minLeftWidth) moveLen2 = minLeftWidth;
//     adjustWidth(moveLen2);
//     $(window).resize(function () {
//         adjustWidth(left.clientWidth);
//     });
//     resize.addEventListener('mousedown', mousedown_fun);
//     resize.addEventListener('touchstart', touchstart_fun);
// });
//
// function getlongestWidth() {
//     let longestWidth = 450;
//     $(".mysoft-content-left .one-line").each(function () {
//         let oneLineWidth = 0;
//         $(this).children().each(function () {
//             oneLineWidth += $(this).outerWidth(true);
//         });
//         if (oneLineWidth > longestWidth) {
//             longestWidth = oneLineWidth;
//         }
//     });
//     return longestWidth;
// }