;(function($, window, document) {
    "use strict";
    $.extend({
        project: function(fnName, param1, param2) {
            var fn = $.extend({}, project),handler;
            if (fn[fnName] == undefined){
                console.log(fnName);
                return false;
            }
            handler = fn[fnName](param1, param2);
            return handler;
        }
    });

    var project = {
        /**
         * 确认添加项目
         *
         **/
        confirmAddProject:function() {
            
            var result = $.verify('checkFormValid','#dialogForm');
            if (false === result) {
                return false;
            }

            var param = {return_url:'/project/index','close_tip':true, 'loading':true, 'loading_txt':$.lang('get', 'CREATEING_PROJECT')};

            if (typeof($('#dialogForm').find('.show_name').html()) != 'undefined'){
                param = {return_value:true, 'close_tip':true, 'async':false};
            }
            
            var result = $.base('helloDialogPost', '/project/add/', param);
            if (false === result){
                return false;
            }
            if (false !== result && typeof($('#dialogForm').find('.show_name').html()) != 'undefined'){
                var project_name    = $('#dialogForm').find('#dialog_project_name').val();
                var project_id      = result.project_id;

                $('#'+$('#dialogForm').find('.show_name').html()).val(project_name);
                $('#project_id').val(project_id);
                $(".hello-autocomplete.project-menu ul").append("<li data-index='"+project_id+"'><span>"+project_name+"</span></li>");
                get_task_infos_by_project_id(1);
            }
            
        },
        /**
         * 确认修改项目
         *
         **/
        confirmEditProject:function() {
            var project_id = $('#dialog_project_id').val();
            if (typeof(project_id)== 'undefined' || isNaN(project_id)) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            }
            var result = $.verify('checkFormValid','#dialogForm');
            if (false === result) {
                return false;
            }

            var url = '/project/edit/project_id/'+project_id;
            var result = $.base('helloDialogPost', url, {reload:true,'close_tip':true, 'loading':true, 'loading_txt':$.lang('get', 'EDITING_PROJECT')});
            if (false === result){
                return false;
            }
        },
        /**
         * 删除项目
         *
         **/
         deleteProject:function(project_id) {
            if (typeof(project_id)== 'undefined' || isNaN(project_id)) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            };
            $.base('postData', '/project/delete_project', {project_id:project_id}, {reload:true,'close_tip':true, 'loading':true, 'loading_txt':$.lang('get', 'DELETEING_PROJECT')});
         },

        /**
         * 确认共享项目
         *
         **/
        confirmShareProject:function() {
            
            var project_id = $('#dialog_project_id').val();
            if (typeof(project_id)== 'undefined' || isNaN(project_id)) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            }
            var result = $.verify('checkFormValid', '#dialogForm');
            if (false === result) {
                return false;
            }
            var url = '/project/project_share/project_id/'+project_id;
            var result = $.base('helloDialogPost', url, {reload:true,'close_tip':true});
            if (false === result) {
                return false;
            }
        },
        /**
         * 取消共享项目
         *
         **/
        cancelProjectShare:function(project_share_id, project_id) {
            if (typeof(project_share_id) == 'undefined' || isNaN(project_share_id) || typeof(project_id) == 'undefined' || isNaN(project_id)) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            };
            
            $.base('postData', '/project/delete_project_share', {project_share_id:project_share_id,project_id:project_id}, {reload:true,'close_tip':true});
        },
        /**
         * 置顶项目
         *
         **/
         topProject:function(project_id) {
            if (typeof(project_id)== 'undefined' || isNaN(project_id)) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            };
            $.base('postData', '/project/top_project', {project_id:project_id}, {reload:true,'close_tip':true, 'loading':true, 'loading_txt':$.lang('get', 'TOPING_PROJECT')});
         },
        /**
         * 确认修改项目合同
         *
         **/
        confirmEditProjectContract:function() {
            var project_id = $('#dialog_project_id').val();
            if (typeof(project_id)== 'undefined' || isNaN(project_id)) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            }
            var result = $.verify('checkFormValid', '#dialogForm');
            if (false === result) {
                return false;
            };
            var result = $.base('helloDialogPost', '/project/contract_detail', {reload:true,'close_tip':true});

            if (false === result){
                return false;
            }
        },
        /**
         * 删除项目成员
         *
         **/
        deleteProjectMember:function(project_member_id, member_id){
            if (typeof(project_member_id)== 'undefined' || isNaN(project_member_id)) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            };
            var obj = {'close_tip':true};
            if(typeof(login_member_id) != 'undefined' && typeof(member_id) != 'undefined' && login_member_id == member_id){
                obj.return_url = '/project/index.html';
            }else{
                obj.reload = true;
            };
            $.base('postData', '/project/delete_project_member', {project_member_id:project_member_id}, obj);
        },
        /**
         * 拒绝或接受分享项目【会员中心用】 
         *
         **/
        operateProjectShare:function(project_share_id, status){
            $.base('postData', '/Project/edit_project_share_status.html', {project_share_id:project_share_id,status:status,is_disabled:true}, {reload:true, close_tip:true});           
        },
        /**
         * 一键接受分享项目【会员中心用】 
         *
         **/
        oneKeyOperateProjectShare:function(status){
            $.base('postData', '/Project/edit_bulk_project_share_status.html', {status:status,is_disabled:true}, {reload:true, close_tip:true});             
        },        

        /**
         * 修改项目成员权限 
         *
         **/
        updateProjectMember:function(project_member_id, privilege){
            $.base('postData', '/Project/edit_project_member.html', {project_member_id:project_member_id,privilege:privilege}, {reload:true, close_tip:true});
        }
        
    };
})(jQuery, window, document);

$(function() {
    /**
     * 添加项目
     *
     **/
    $(".add_project").on('click',function(){
        var url = '/Project/add.html';
        if (typeof($(this).attr('show_name')) != 'undefined') {
            var project_name = $(this).attr('project_name');
            url = '/Project/add/show_name/'+$(this).attr('show_name')+'/project_name/'+project_name+'.html';
        }
         $.dialog({
            type:'loader',
            title:$.lang('get', 'NEW_PROJECT'),
            width:700,
            height:'auto',
            draggable:true,
            msg:$.lang('get', 'NEW_PROJECT'),
            url:url,
            async:true,    
            method:'get',
            loadBeforeText:$.lang('get', 'LOADING'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            confirmBtnTxt:$.lang('get', 'OK'),
            confirmFn:function(){
                return $.project('confirmAddProject');
            }
            /*,
            cancelFn:function(){
                alert("点击取消");
            }*/
        });
    });

    /**
     * 修改项目
     *
     **/
    $(".edit_project").click(function(){
        var project_id = $(this).attr('project_id');
        var url        = '/project/edit/project_id/'+project_id+'.html';
        if (typeof(project_id)== 'undefined' || isNaN(project_id)) {
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        }
        
        $.dialog({
            type:'loader',
            title:$.lang('get', 'EDIT_PROJECT'),
            width:700,
            height:'auto',
            draggable:true,
            msg:$.lang('get', 'EDIT_PROJECT'),
            url:url,
            async:true,    
            method:'get',
            loadBeforeText:$.lang('get', 'LOADING'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            confirmBtnTxt:$.lang('get', 'OK'),
            confirmFn:function(){
                return $.project('confirmEditProject');
            }
            /*,
            cancelFn:function(){
                alert("点击取消");
            }*/
        });
    });

    /**
     * 删除项目
     *
     **/
    $(".delete_project").click(function(){
        if (!confirm($.lang('get', 'CONFIRM_DELETE_PROJECT'))){
            return false;
        }
        var project_id = $(this).attr('project_id');
        if (typeof(project_id)== 'undefined' || isNaN(project_id)) {
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        }
        
        $.project('deleteProject', project_id);
    });

    /**
     * 共享项目
     *
     **/
    $(".share_project").click(function(){
        var project_id = $(this).attr('project_id');
        var url        = '/project/project_share/project_id/'+project_id+'.html';
        if (typeof(project_id)== 'undefined' || isNaN(project_id)) {
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        }
        $.dialog({
            type:'loader',
            title:$.lang('get', 'SHARE_PROJECT'),
            width:500,
            height:'auto',
            loadBeforeText:$.lang('get', 'LOADING'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            confirmBtnTxt:$.lang('get', 'OK'),
            url:url,
            confirmFn:function(){
                return $.project('confirmShareProject');
            }
        });
    });
    
    /**
     * 置顶项目
     *
     **/
    $(".top_project").click(function(){
        var project_id = $(this).attr('project_id');
        var url        = '/project/project_top/project_id/'+project_id+'.html';
        if (typeof(project_id)== 'undefined' || isNaN(project_id)) {
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        }
        $.project('topProject', project_id);
    });

    /**
     * 取消共享项目
     *
     **/
    $(".cancel_project_share").on('click',function(){

        if (!confirm($.lang('get', 'CONFIRM_CANCEL_SHARE'))){
            return false;
        }
        var project_share_id    = $(this).attr('project_share_id');
        var project_id          = $(this).attr('project_id');
        if (typeof(project_share_id) == 'undefined' || isNaN(project_share_id) || typeof(project_id) == 'undefined' || isNaN(project_id)) {
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        }
        
        $.project('cancelProjectShare', project_share_id, project_id);

    });

    /**
     * 合同维护
     *
     **/
    $(".edit_project_contract").click(function(){
        var project_id = $(this).attr('project_id');
        if (typeof(project_id)== 'undefined' || isNaN(project_id)) {
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        }

        var member_type = $(this).attr('member_type');
        if (member_type != '1'){
            layer.msg($.lang('get', 'NO_PRIVILEGE'),{icon:7});
            return false;
        }

        var url = '/project/contract_detail/project_id/'+project_id+'.html';
        $.dialog({
            type:'loader',
            title:$.lang('get', 'CONTRACT_CONTENT'),
            width:700,
            height:'auto',
            draggable:true,
            msg:$.lang('get', 'CONTRACT_CONTENT'),
            url:url,
            async:true,    
            method:'get',
            loadBeforeText:$.lang('get', 'LOADING'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            confirmBtnTxt:$.lang('get', 'OK'),
            confirmFn:function(){
                return $.project('confirmEditProjectContract');
            }
            /*,
            cancelFn:function(){
                alert("点击取消");
            }*/
        });
    });

    /**
     * 删除项目会员
     *
     **/
    $(".delete_member").on('click',function(){

        if (!confirm($.lang('get', 'CONFIRM_OPERATE'))){
            return false;
        }

        var project_member_id   = $(this).attr('project_member_id');
        var member_id           = $(this).attr('member_id');

        if (typeof(project_member_id)== 'undefined' || isNaN(project_member_id)) {
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        }
        $.project('deleteProjectMember', project_member_id, member_id);

    });

    /**
     * 拒绝或接受分享项目【会员中心用】 
     *
     **/
    $('.operate_project_share').click(function(){
        var project_share_id    = $(this).attr('project_share_id');
        var status              = $(this).attr('status');
        var status_array        = [1,2];
        if(typeof(project_share_id) == 'undefined' || project_share_id.length < 1 || typeof(status) == 'undefined' || status.length < 1 || ($.inArray(parseInt(status),status_array) < 0)) {
            $.tip({width:300, position:'center', type:'warning',msg:$.lang('get', 'WRONG_PARAMETER'), keep:2});
            return false;
        }
        if(status == '2') {
            if(!confirm($.lang('get', 'CONFIRM_REFUSE_INVITE'))){return false};
        }
        $(this).attr('disabled',true);
        $(this).addClass('disabled');
        $.when($.project('operateProjectShare', project_share_id, status)).done(function(data){
            console.log(data);return false;
        });
    });
    
    /**
     * 一键接受分享项目【会员中心用】 
     *
     **/
    $('.one_key_operate_project_share').click(function(){
        var status              = $(this).attr('status');
        var status_num = $(this).attr('status_num');
        if(typeof(status_num) == 'undefined' || status_num < 1){
        	$.tip({width:300, position:'center', type:'warning',msg:$.lang('get', 'NO_SHARE_PROJECT'), keep:2});
            return false;
        }
        if(typeof(status) == 'undefined' || status.length < 1) {
            $.tip({width:300, position:'center', type:'warning',msg:$.lang('get', 'WRONG_PARAMETER'), keep:2});
            return false;
        }
        $(this).attr('disabled',true);
        $(this).addClass('disabled');
        $.when($.project('oneKeyOperateProjectShare', status)).done(function(data){
            console.log(data);
            setTimeout(function(){  //使用  setTimeout（）方法设定定时2000毫秒
                location.reload();
        	},2000);
        });
    });



    /**
     * 修改项目成员权限 
     *
     **/
    $('.manage_project_member').click(function(){
        var project_member_id   = $(this).attr('project_member_id');
        var privilege           = $(this).attr('privilege');

        if(typeof(project_member_id) == 'undefined' || project_member_id.length < 1 || typeof(privilege) == 'undefined' || privilege.length < 1 ) {
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        };

        $.project('updateProjectMember', project_member_id, privilege);
    });
    
    //参数说明黑色气泡
    $(".explain").mouseover(function(e){
        var w = $(window).width();
        if(w -150 < e.clientX){
            $(this).addClass('right');
        }else if(e.clientX <  350){
            $(this).addClass('left');
        }else{
            $(this).removeClass('right');
            $(this).removeClass('left');
        }
    });    
    
    
});
