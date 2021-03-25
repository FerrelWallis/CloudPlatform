function creatSelector(id, optionname, option, selected) {
    $.each(optionname,function (i,v) {
        var sel = "<option value="+option[i]+" selected>"+v+"</option>";
        var nosel= "<option value="+option[i]+">"+v+"</option>";
        if(v == selected) $("#"+id).append(sel);
        else $("#"+id).append(nosel);
    });
}

function checkupdate(updatetime, subtime){
    $(".update").text(updatetime);
    if(subtime.split(" ")[0]<=updatetime) {
        $(".update_panel").show();
        $(".redraw_panel").hide();
    } else {
        $(".redraw_panel").show();
        $(".update_panel").hide();
    }
}

function downloadpics(status,taskname,picname,suffix){
    console.log(status);
    if(status!=="已完成"){
        swal({
            title: "无法下载",
            text: "任务未完成，无法下载！",
            type: "warning",
            confirmButtonText: "确认"
        });
    }else{
        window.open("/CloudPlatform/SoftTool/download?taskname="+taskname+"&picname="+picname+"&suffix="+suffix+"&num="+Math.random());
    }
}

function addFormValidation(id, fields,excluded) {
    $('#' + id).formValidation({
        framework: 'bootstrap',
        icon: {
            valid: 'glyphicon glyphicon-ok',
            invalid: 'glyphicon glyphicon-remove',
            validating: 'glyphicon glyphicon-refresh'
        },
        excluded: excluded,  //默认excluded: [':disabled', ':hidden', ':not(:visible)'] 现在需要hidden的元素也检查
        fields: fields
    })
}

function FieldTaskname(field) {
    field["taskname"] = {
        validators: {
            notEmpty: {
                message: '任务编号不能为空!'
            },
            remote:{
                type: 'POST',
                url: '/CloudPlatform/MyTasks/checkTaskname',
                message: "任务编号重复，请重新输入",
                delay: 1000
            }
        }
    };
    return field;
}

function FieldText(field, fields) {
    fields.forEach(x => {
        field[x] = {
            validators: {
                notEmpty: {
                    message: '不能为空！'
                }
            }
        }
    });
    return field;
}

function FieldNumber(field, fields) {
    fields.forEach(x => {
        field[x] = {
            validators: {
                notEmpty: {
                    message: '不能为空！'
                },
                numeric: {
                    message: "必须为数字"
                }
            }
        }
    });
    return field;
}

function FieldNumberonly(field, fields) {
    fields.forEach(x => {
        field[x] = {
            validators: {
                numeric: {
                    message: "必须为数字"
                }
            }
        }
    });
    return field;
}

function FieldFile(field, fields) {
    fields.forEach(x => {
        field[x] = {
            validators: {
                notEmpty: {
                    message: '不能为空！'
                }
                // ,
                // file: {
                //     // extension: 'txt',
                //     // message: '请上传txt格式的文件',
                //     maxSize: 5*1024*1024,
                //     message: '文件格式必须为txt，且不得大于5M！'
                // }
            }
        }
    });
    return field;
}

function FieldColor(field,fields) {
    fields.forEach(x => {
        field[x] = {
            validators: {
                notEmpty: {
                    message: '不能为空!'
                },
                color: {
                    type: ['hex','keyword'],  // The default value for type
                    message: '错误色值！支持hex或keyword色值'
                }
            }
        }
    });
    return field;
}

function FieldName(field,fields) {
    fields.forEach(x => {
        field[x] = {
            validators: {
                callback:{
                    message: '标题不能有汉字!',
                    callback: function (value, validator, $field) {
                        var rule = /[\u4e00-\u9fa5]+/;
                        return !(rule.test(value));
                    }
                }
            }
        }
    });
    return field;
}

function gapfocus(para) {
    $("#"+para).show();
}

function gapblur(para) {
    $("#"+para).hide();
}