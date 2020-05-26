$('.form-gp input').on('focus', function() {
    $(this).parent('.form-gp').addClass('focused');
});
$('.form-gp input').on('focusout', function() {
    if ($(this).val().length === 0) {
        $(this).parent('.form-gp').removeClass('focused');
    }
});

var path = window.location.pathname;


var code ; //在全局定义验证码

function openSignup() {
    $(".close-reveal-modal").click();
    $(".signup").click();
    createCode();
}

function openSignin() {
    $(".close-reveal-modal").click();
    $(".signin").click();
    createCode();
}

function createCode() {
    code = "";
    var codeLength = 4;//验证码的长度
    var random = new Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);//随机数
    for (var i = 0; i < codeLength; i++) {//循环操作
        var index = Math.floor(Math.random() * 10);//取得随机数的索引（0~35）
        code += random[index];//根据索引取得随机数加到code上
    }
    $(".code").val(code);//把code值赋给验证码
}

function Signout() {
    window.location.href = "/CloudPlatform/User/signout?path=" + path;
}

//登录
function Signin() {
    var form = $("#signinForm");
    var fv = form.data("formValidation");
    fv.validate();
    if (fv.isValid()) {
        $.ajax({
            url: "",
            type: "post",
            dataType: "json",
            data: form.serialize(),
            success: function (data) {
                if (data.valid === "false") {
                    swal({
                        title: "错误",
                        text: "账号或密码错误",
                        type: "error",
                        confirmButtonText: "确认"
                    })
                } else {
                    window.location.href = "/CloudPlatform/User/signInSuccess?path=" + path + "&phone=" + data.phone;
                }
            }
        });
    }
}

//注册
function Signup() {
    var form = $("#signupForm");
    var fv = form.data("formValidation");
    fv.validate();
    if (fv.isValid()) {
        $.ajax({
            url: "@routes.UserController.addUser()",
            type: "post",
            dataType: "json",
            data: form.serialize(),
            success: function (data) {
                if (data.valid == "false") {
                    swal({
                        title: "错误!",
                        text: data.message,
                        type: "error",
                        confirmButtonText: "确定"
                    })
                } else {
                    $(".close-reveal-modal").click();
                    swal({
                        title: "注册成功",
                        text: "注册成功!欢迎使用云平台",
                        type: 'success',
                        confirmButtonText: "确定"
                    }, function () {

                        $(".signin").click();
                        createCode();
                    })
                }
            }
        });
    }

}

//修改密码
function changePassword() {
    var form = $("#passwordForm");
    var fv = form.data("formValidation");
    fv.validate();
    if (fv.isValid()) {
        $.ajax({
            url: "   ",
            type: "post",
            dataType: "json",
            data: form.serialize(),
            success: function (data) {
                if (data.valid == "false") {
                    swal({
                        title: "Error!",
                        text: data.message,
                        type: "error",
                        confirmButtonText: "OK"
                    })
                } else {
                    $(".close-reveal-modal").click();
                    swal({
                        title: "Success",
                        text: "Change password success!",
                        type: 'success',
                        confirmButtonText: "Click on the return!"
                    }, function () {
                        window.location.href = "/CloudPlatform/User/signout?path=" + path;
                    })
                }
            }
        });
    }

}

function formValidation() {
    $("#signinForm").formValidation({
        framework: 'bootstrap',
        icon: {
            valid: 'glyphicon glyphicon-ok',
            invalid: 'glyphicon glyphicon-remove',
            validating: 'glyphicon glyphicon-refresh'
        },
        fields: {
            user: {
                validators: {
                    notEmpty: {
                        message: '请输入账号！'
                    }
                }
            },
            password: {
                validators: {
                    notEmpty: {
                        message: '请输入密码！'
                    }
                }
            },
            validnumber: {
                validators: {
                    notEmpty: {
                        message: '请输入验证码！'
                    },
                    callback: {
                        message: '验证码错误！',
                        callback: function () {
                            return $('#validnumber').val() === code;
                        }
                    }
                }
            }
        }
    });
    $('#signupForm').formValidation({
        framework: 'bootstrap',
        icon: {
            valid: 'glyphicon glyphicon-ok',
            invalid: 'glyphicon glyphicon-remove',
            validating: 'glyphicon glyphicon-refresh'
        },
        fields: {
            user: {
                validators: {
                    notEmpty: {
                        message: '请输入账号！'
                    }
                }
            },
            password: {
                validators: {
                    notEmpty: {
                        message: '请输入密码！'
                    },
                    stringLength: {
                        min: 6,
                        message: '请输入至少六位密码！'
                    }
                }
            },
            password1: {
                validators: {
                    notEmpty: {
                        message: '请输入确认密码！'
                    },
                    identical: {
                        field: 'password',
                        message: '两次密码不一致!'
                    }
                }
            },
            validnumber: {
                validators: {
                    notEmpty: {
                        message: "请输入验证码！"
                    },
                    callback: {
                        message: '验证码错误！',
                        callback: function () {
                            return $('#validnumber1').val() === code;
                        }
                    }
                }
            }
        }
    });
    $('#passwordForm').formValidation({
        framework: 'bootstrap',
        icon: {
            valid: 'glyphicon glyphicon-ok',
            invalid: 'glyphicon glyphicon-remove',
            validating: 'glyphicon glyphicon-refresh'
        },
        fields: {
            password: {
                validators: {
                    notEmpty: {
                        message: '请输入旧密码！'
                    }
                }
            },
            password1: {
                validators: {
                    notEmpty: {
                        message: '请输入新密码！'
                    },
                    stringLength: {
                        min: 6,
                        message: '请输入至少六位密码！'
                    }
                }
            },
            password2: {
                validators: {
                    notEmpty: {
                        message: '请输入确认密码！'
                    },
                    identical: {
                        field: 'password1',
                        message: '两次密码不一致!'
                    }
                }
            },
            validnumber: {
                validators: {
                    notEmpty: {
                        message: "请输入验证码！"
                    },
                    callback: {
                        message: '验证码错误！',
                        callback: function () {
                            return $('#validnumber2').val() === code;
                        }
                    }
                }
            }
        }
    });
}