$("input[type='text']").keydown(function () {
    var theEvent = window.event || e;
    var code = theEvent.keyCode || theEvent.which;
    if (code == 13) {
        return false;
    }
});

function loadVideo(url,pic) {
    var id=md5(url);
    new DPlayer({
        container: document.getElementById('dplayer'),
        autoplay: false,
        loop: false,
        lang: 'zh-cn',
        screenshot: true,
        hotkey: true,
        preload: 'auto',
        volume: 0.7,
        mutex: true,
        airplay:false,
        video: {
            url: url,
            pic: pic,
            thumbnails: pic,
            type: 'auto',
        },
        danmaku: {
            id: id,
            api: 'https://dplayer.alone88.cn/',
        }
    });
}



function getTaskName(formid,prename) {
    let date = new Date();
    let month = date.getMonth() + 1;
    let day = date.getDate();
    let h = date.getHours();
    let m = date.getMinutes();
    let s = date.getSeconds();
    let name = prename+month + day + h + m + s ;
    $("#taskname").val(name);
    $("#"+formid).formValidation("revalidateField", "taskname");
}

$("#taskname").keyup(function () {
    checktaskname();
});

function checktaskname() {
    var val = $("#taskname").val();
    var rule = /[()（）@@#$%^&*~`!|\/\\?;,.，。:"]/;
    if (rule.test(val)) {
        alert('任务编号里不能有特殊字符，请重新填写!');
        return false;
    }
    if(val.toString().indexOf(" ")>0){
        alert('任务编号里不能有空格!');
        return false;
    }
    if(val.toString().length>20){
        alert('任务编号最长20位!');
        return false;
    }
    return true;
}

var running=false;

var cur=true;

var timmer=null;

function refresh(abbre) {
    if(cur){
        cur=false;
        removeul();
        loadHistory(abbre);
        var count=5;
        $("#btn-refresh").text("00:"+count);
        var tim=setInterval(function () {
            if(count==0){
                cur=true;
                $("#btn-refresh").text("刷新状态");
                clearInterval(tim);
            }else {
                count=count-1;
                $("#btn-refresh").text("00:"+count);
            }
        },1000);
    }else return;
}

function loadHistory(abbre){
     if(timmer!=null) {
         clearInterval(timmer);
         timmer=null;
         cur=true;
     }
    var html="";
    $.ajax({
        url: "/CloudPlatform/MyTasks/getDutysByType?sabbrename="+abbre,
        type: "post",
        success: function (data) {
            if(data.run==true) running=true;
            else running=false;
            $.each(data.rows,function (i,v) {
                html += "<li class='hislist'><div class='his_time'><span>"+v.taskname+"</span></div><div class='his_time'><span>"+v.finitime+"</span></div><div class='his_name'>"+v.status+"</div><hr></li>";
            });
            $("#hislist").after(html);
            if(running==true){
                    cur=false;
                    var count=5;
                    $("#btn-refresh").text("00:"+count);
                    timmer=setInterval(function () {
                        if(count==0){
                            $("#btn-refresh").text("刷新状态");
                            removeul();
                            loadHistory(abbre);
                            if(running==false) {
                                cur=true;
                                clearInterval(timmer);
                                timmer=null;
                            } else {
                                checkrunning(abbre);
                            }
                            count=5;
                        }else {
                            count=count-1;
                            $("#btn-refresh").text("00:"+count);
                        }
                    },1000);
            }
        }
    });

}

function checkrunning(abbre) {
    $.ajax({
        url: "/CloudPlatform/MyTasks/getDutysByType?sabbrename="+abbre,
        type: "post",
        success: function (data) {
            if(data.run==true) running=true;
            else running=false;
        }
    });
    return running;
}

function removeul(){
    $("#hisul li").remove();
    $("#hisul").append("<li id=\"hislist\" class=\"hislist\">\n" +
        "                                    <div class=\"his_time\"><span>任务编号</span></div>\n" +
        "                                    <div class=\"his_time\"><span style='margin-left: -20px;'>结束时间</span></div>\n" +
        "                                    <div class=\"his_name\"><span style='margin-left: -23px;'>状态</span></div>\n" +
        "                                    <hr>\n" +
        "                                </li>");

}

$(".file").fileinput({
    showPreview: false,
    showUpload:false,
    showRemove:true,
    browseLabel: "选择文件"
});

function switchRunningTab() {
    $("#his_tab1").addClass("active");
    $("#out_tab1").removeClass("active");
    $("#ins_tab1").removeClass("active");
    $("#faq_tab1").removeClass("active");
    $("#history").addClass("active").addClass("in");
    $("#example").removeClass("active");
    $("#instruction").removeClass("active");
    $("#faq").removeClass("active");
}