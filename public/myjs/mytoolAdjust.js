const leftWidth = 350;  // 左边部分宽度
const rightToLeftGap = 10;  // 右边部分与左边部分的距离
const lineWidth = 8; // 分隔条宽度
const splitMinLeft = 350; // 分隔条左边部分最小宽度
const splitMaxLeft = 650; // 分隔条左边部分最大宽度

var oRoot = document.getElementById('ta_root'),
    oLeft = document.getElementById('ta_left'),
    oRight = document.getElementById('ta_right'),
    oLine = document.getElementById('ta_line');

window.onload = function () {
    oLine.onmousedown = handleLineMouseDown;
};

// 分隔条操作
function handleLineMouseDown(e) {
    // 记录下初始位置的值
    let disX = e.clientX;
    oLine.left = oLine.offsetLeft;

    document.onmousemove = function (e) {
        let moveX = e.clientX - disX;   // 鼠标拖动的偏移距离
        let iT = oLine.left + moveX,    // 分隔条相对父级定位的 left 值
            maxT = oRoot.clientWidth - oLine.offsetWidth;

        iT < 0 && (iT = 0);
        iT > maxT && (iT = maxT);

        if (iT <= splitMinLeft || iT >= splitMaxLeft) return false;

        let leftLineGap = splitMinLeft - leftWidth; // 分隔条距左边部分的距离
        let oLeftWidth = iT - leftLineGap;
        let oRightMarginLeft = oLeftWidth + lineWidth + rightToLeftGap;

        oLine.style.left = `${iT}px`;
        oLeft.style.width = `${oLeftWidth}px`;
        oRight.style.marginLeft = `${oRightMarginLeft}px`;
        return false;
    };

    // 鼠标放开的时候取消操作
    document.onmouseup = function () {
        document.onmousemove = null;
        document.onmouseup = null;
    };
}