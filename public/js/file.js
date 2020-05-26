;(function($, window, document) {
    if (window.file) {
        return;
    }
    $.extend({
        file: function(fnName, param1, param2) {
            var fn = $.extend({}, file),handler;
            if (fn[fnName] == undefined){
                console.log(fnName);
                return false;
            }
            handler = fn[fnName](param1, param2);
            return handler;
        }
    });

    window.file = {
        //提交创建文件夹
        confirmAddFileDir:function () {
            var name        = $('#dialog_dir_name').val();
            var result = $.verify('fileDirRelegx', name);
            if (false === result) {
                return false;
            }
            var result = $.verify('checkFormValid','#dialogForm');
            if (false === result) {
                return false;
            }
            var url     = '/file/add_file_dir.html';
            var param   = {'reload':true,'async':false};

            var result  = $.base('helloDialogPost', url, param);
            if (false === result){
                return false;
            }
        },
        /**
         * 提交复制文件
         * jeff
         */
        confirmCopyFile:function() {
            var url = '/file/copy_file.html';
            var param = {};
            var return_url  = '/file/index';
            var parent_hash = $('#dialogForm #parent_hash').val();
            var origin_hashes = $('#dialogForm #origin_hashes').val();
            if (typeof(parent_hash) == 'undefined' || parent_hash.length < 1) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            }
            if (typeof(origin_hashes) == 'undefined' || origin_hashes.length < 1) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            }

            if (typeof(parent_hash) != 'undefined' && parent_hash.length > 0) {
                return_url += '/parent_hash/'+parent_hash;
            }
            var project_id  = $('#project_id').val();
            if (typeof(project_id) != 'undefined' && project_id.length > 0) {
                return_url += '/project_id/'+project_id;
            }
            return_url      += '.html';
            //param.return_url = return_url;
            param.reload = true;
            param.loading = true;
            param.loading_txt = $.lang('get', 'COPYING_FILE');
            var result = $.base('helloDialogPost', url, param);
            if (false === result){
                return false;
            }
        },
        /**
         * 移动文件
         * jeff
         */
        confirmMoveFile:function(obj) {
            var url = '/file/move_file.html';
            var param = {};
            var return_url  = '/file/index';
            var parent_hash = $('#dialogForm #parent_hash').val();
            var origin_hashes = $('#dialogForm #origin_hashes').val();
            if (typeof(parent_hash) == 'undefined' || parent_hash.length < 1) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            }
            if (typeof(origin_hashes) == 'undefined' || origin_hashes.length < 1) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            }

            if (typeof(parent_hash) != 'undefined' && parent_hash.length > 0) {
                return_url += '/parent_hash/'+parent_hash;
            }
            var project_id  = $('#project_id').val();
            if (typeof(project_id) != 'undefined' && project_id.length > 0) {
                return_url += '/project_id/'+project_id;
            }
            return_url      += '.html';
            //param.return_url = return_url;
            param.reload = true;
            param.loading = true;
            param.loading_txt = $.lang('get', 'MOVING_FILE');
            var result = $.base('helloDialogPost', url, param);
            if (false === result){
                return false;
            }
        },

        /**
         * 修改文件/文件夹
         * zy
         */
        confirmEditFileDir:function() {
            var unique_hash = $('#dialog_unique_hash').val();
            var name        = $('#dialog_name').val();

            if (typeof(unique_hash) == 'undefined' || unique_hash.length < 1) {
                layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
                return false;
            }

            var result = $.verify('fileDirRelegx', name);
            if (false === result) {
                return false;
            }

            var result = $.verify('checkFormValid','#dialogForm');
            if (false === result) {
                return false;
            }

            var url = '/file/edit_file_dir.html';

            var obj = {'reload':true,'need_tip':true, 'async':false};

            var result = $.base('helloDialogPost', url, obj);
            if (false === result){
                return false;
            }



        },
        /**
         * 选择文件
         * jeff
         */
        confirmChooseFile:function(obj) {
            return false;
        },
    };
})(jQuery, window, document);

$(function() {
    /**
     * 添加文件夹
     * jeff
     */
    $(".add_file_dir").on('click',function(){
        var url         = '/File/add_file_dir';
        var project_id  = $(this).attr('project_id');
        if(typeof(project_id) != 'undefined' && project_id.length > 0){
            url += '/project_id/'+project_id;
        }
        var parent_hash     = $(this).attr('parent_hash');
        if(typeof(parent_hash) != 'undefined' && parent_hash.length > 0){
            url += '/parent_hash/'+parent_hash;
        }
        url += '.html';

        $.dialog({
            type:'loader',
            title:$.lang('get', 'NEW_FOLDER'),
            width:600,
            height:200,
            draggable:true,
            msg:$.lang('get', 'NEW_FOLDER'),
            url:url,
            async:true,    
            method:'get',
            loadBeforeText:$.lang('get', 'LOADING'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            confirmBtnTxt:$.lang('get', 'OK'),
            confirmFn:function(){
                return $.file('confirmAddFileDir');
            }
        });
    });
    
    /**
     * 选择文件
     * jeff
     */
    $(document).on('click', ".select_file_dir", function(){
        var obj     = $(this);
        var act     = obj.attr('act');
        var host    = obj.attr('host');
        if(typeof(act) == 'undefined' || act.length < 1 || 'undefined' == typeof(json_select_file_act_arr[act])){
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        }
        var confirm_btn_txt = $.lang('get', 'OK');
        var parent_hash     = obj.attr('parent_hash');
        if(typeof(WWW_SG_COM) !='undefined' ){
            var url             = '/file/select_file_dir/en_name/anno_ref/act/'+act+'/task_id/'+obj.attr('task_id');
        }else{
            var url             = '/file/select_file_dir/act/'+act;
        }
        if (typeof(host) != 'undefined') {
            url = host + url;
        }
        var first_choose    = $(this).attr('first_choose');
        if ('choose_file' == act) {
            confirm_btn_txt = false;
            var en_name     = obj.parent().parent().attr('class');//页面提交选择使用
            var only_file   = obj.attr('only_file');//页面提交选择使用
            if (typeof(en_name) == 'undefined' ||  en_name == 'mod-select-file') {en_name = $('#dialogForm').find("input[name=en_name]").val()}//弹出页面点击使用
            if (typeof(only_file) == 'undefined') {only_file = $('#dialogForm').find("input[name=only_file]").val()}//弹出页面点击使用
            /*if(typeof(project_id) == 'undefined') {
                var project_id = $('#project_id').val();
            }*/
            var task_id     = $('#task_id').val();
            if(typeof(member_type) != 'undefined' && 3 == member_type){
                //医学
                if(typeof(en_name) != 'undefined') {
                    url += '/en_name/'+en_name;
                    if ('data_dir' != en_name) {
                        url += '/only_file/1';
                    }
                }
            }else{
                /*if (typeof(project_id) == 'undefined' || project_id < 1) {
                    $.tip({width:300, position:'center', type:'warning',msg:'请指定正确项目', keep:1,});
                    return false;
                }*/
                //目标目录 判断
                //url += '/project_id/'+project_id;
                if ('undefined' != typeof(only_file) && only_file) {
                    url += '/only_file/'+only_file;
                }
                if(typeof(en_name) != 'undefined') {
                    url += '/en_name/'+en_name;
                    if ('raw_sequence' == en_name || 'species_software' == en_name || 'species_info' == en_name||'geneset_upload'==en_name) {
                        url += '/only_file/1';
                    }
                }
            }
            if (parent_hash != '') {
                url += '/parent_hash/'+parent_hash;
            }
        }else{
            var checked_origin_hashes = $(".origin_hashes:checked");
            if (typeof(checked_origin_hashes) == 'undefined' || checked_origin_hashes.length == 0) {
                layer.msg($.lang('get', 'PLEASE_SELECT')+json_select_file_act_arr[act]['name'],{icon:7});
                return false;
            }
            //目标目录 判断
            /*if (typeof(parent_hash) == 'undefined' || parent_hash.length < 1) {
                $.tip({width:300, position:'center', type:'warning',msg:'目录不正确', keep:1,});
                return false;
            }*/
            //源文件
            var origin_hashes = new Array();
            $.each(checked_origin_hashes, function(i, data){
                origin_hashes.push($(data).val());
            });
            
            url += '/origin_hashes/'+origin_hashes;
            if (parent_hash != '') {
                url +='/parent_hash/'+parent_hash;
                $('.hello-dialog').find('.btng a:eq(0)').show();
                $('.hello-dialog').find('.btng span').remove();
            } else {
                $('.hello-dialog').find('.btng a:eq(0)').hide();
                if (!$('.hello-dialog').find('.btng span').hasClass('tip')){
                
                    $('.hello-dialog').find('.btng').append('<span class="tip" style="color:red">'+$.lang('get', 'CAN_NOT')+json_select_file_act_arr[act]['name']+$.lang('get', 'TO_ROOT_PATH')+'<span>');
                }
            }
            /*var project_id = $(this).attr('project_id');
            // if(privilege == 2) {
                //只能项目内移动、复制
                if(typeof(project_id) != 'undefined' && project_id.length > 0){
                    url += '/project_id/'+project_id;
                }
            // }
            */
        }
        url += ".html";
        //第一次
        if(typeof(first_choose) != 'undefined'){
                $.dialog({
                    type:'loader',
                    title:json_select_file_act_arr[act]['name'],
                    width:600,
                    height:'auto',
                    draggable:true,
                    msg:json_select_file_act_arr[act]['name'],
                    url:url,
                    async:true,    
                    method:'get',
                    loadBeforeText:$.lang('get', 'LOADING'),
                    cancelBtnTxt:$.lang('get', 'CLOSE'),
                    confirmBtnTxt:confirm_btn_txt,
                    confirmFn:function(){
                        var confirmFunction = 'confirm'+json_select_file_act_arr[act]['js_act'];
                        return $.file(confirmFunction);
                    }
                });
        }else{
            //第二次
            $.ajax({
                url:url,
                type:'get',
                success:function(res){
                    $(".hello-dialog .container").html(res);
                }
            });
        }

    });

    /**
     * 编辑文件
     * jeff
     */
    $(".edit_file_dir").on('click', function () {
        var url         = '/File/edit_file_dir';
        var unique_hash = $(this).parentsUntil('tr').parent().find('.origin_hashes').val();
        if (typeof(unique_hash) == 'undefined' || $.trim(unique_hash) == '') {
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        }

        $.dialog({
            type:'loader',
            title:$.lang('get', 'EDIT'),
            width:600,
            height:200,
            draggable:true,
            msg:$.lang('get', 'EDIT'),
            url:url,
            async:true,    
            method:'get',
            data:'unique_hash='+unique_hash,
            loadBeforeText:$.lang('get', 'LOADING'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            confirmBtnTxt:$.lang('get', 'OK'),
            confirmFn:function(){
                return $.file('confirmEditFileDir');
            }
        });
    });

    /**
     * 选中文件
     * jeff
     */
    $(document).on('click',".choose", function () {
        var file_dir_id     = $(this).attr('file_dir_id');
        var file_dir_type   = $(this).attr('file_dir_type');
        var file_dir_path   = $(this).attr('file_dir_path');
        if(typeof(WWW_SG_COM) !='undefined' && file_dir_type =='1' ){
            layer.msg($.lang('get', 'SELECT_FILE'),{icon:7});
            return false;
        }
        if(typeof(en_name) == 'undefined' || en_name.length < 1 || typeof(file_dir_id) == 'undefined' || file_dir_id.length < 1 || typeof(file_dir_type) == 'undefined' || file_dir_type.length < 1){
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        }
        
        if(typeof(member_type) != 'undefined' && member_type == 3){
            //医学帐户用
            var hidden = $("#"+en_name).find("input[type='hidden']").val(file_dir_id);                
            $("#"+en_name).find("input[type='text']").val(file_dir_path);

            var formParameter = $('#form_parameter');
            var flag          = 0;
            formParameter.find('input').each(function(index){
                if($(this).attr('is_must') == '1' && $(this).val() == ''){
                    //doing---
                    flag = 1; 
                }
            });

            if(1 !== flag){
                $('.run_parameter').addClass('btn-activate');
            }
        }else if(typeof(WWW_SG_COM) !='undefined'){
            $('input[name="hidden_file_dir_id"]').val(file_dir_id);
            $('input[name="genbank_file"]').val(file_dir_path);
        }else{
            var hidden = $("."+ en_name).find("input[type='hidden']");
            if(typeof(enterFileDirAndFilePath) == 'function'){
                return enterFileDirAndFilePath({'file_dir_id':file_dir_id,'file_dir_path':file_dir_path,'en_name':en_name});
            }
            if('1' == file_dir_type) {
                hidden.prev('select').val(394);
            }else{
                hidden.prev('select').val(393);
            }
            if(en_name=='in_fastq'){
                $('#sample_list').hide().find('tbody').empty();
            }
            $("."+en_name).find("input[name ^=parameter_ids]").val(file_dir_path).focus();
            hidden.val(file_dir_id);
            if(typeof(controllDisplay) == 'function' && (typeof(cmd_command) == 'undefined' || cmd_command != 'ref_rna.refrna')){
                controllDisplay(2, {en_name:en_name,value:file_dir_path});
            }
        }

        $('.hello-dialog').remove();
        $('.hello-dialogMark').remove();

        if(typeof(getFileContent) == 'function') {//bsa读取样本列表
            setTimeout(function() {
                getFileContent();
            }, 200);
        }
    });

    /**
     * 批量锁定
     * jeff
     */
    $(".lock_files").on('click',function(){
        var text = $.lang('get', 'LOCK');
        var url  = "/file/lock_files.html";
        if ($(".origin_hashes:checked").length < 1) {
            layer.msg($.lang('get', 'SELECT_FILE_LOCK'),{icon:7});
            return false;
        }

        var origin_hashes = new Array();
        $.each($(".origin_hashes:checked"), function(i, data){
            var origin_hash = $(data).val();
            origin_hashes.push(origin_hash);
        });
        $('a.lock_files:eq(0)').html($.lang('get', 'WAITING_LOCK'));
        var obj = {'reload':true,'need_tip':true};
        var result = $.base('postData', url, 'origin_hashes='+origin_hashes, obj);
        if (false === result || typeof(result) == 'undefined') {
            $('a.lock_files:eq(0)').html("<i class='iconcloud'>&#xe6b4;</i> "+text);
        }
    });

    /**
     * 批量解锁
     * jeff
     */
    $(".unlock_files").on('click',function(){
        var text = '解锁';
        var url  = "/file/unlock_files.html";
        if ($(".origin_hashes:checked").length < 1) {
            layer.msg($.lang('get', 'SELECT_FILE_UNLOCK'),{icon:7});
            return false;
        }

        var origin_hashes = new Array();
        $.each($(".origin_hashes:checked"), function(i, data){
            var origin_hash = $(data).val();
            origin_hashes.push(origin_hash);
        });
        $('a.unlock_files:eq(0)').html($.lang('get', 'WAITING_UNLOCK'));

        var obj = {'reload':true,'need_tip':true};
        var result = $.base('postData', url, 'origin_hashes='+origin_hashes, obj);
        if (false === result || typeof(result) == 'undefined') {
            $('a.unlock_files:eq(0)').html("<i class='iconcloud'>&#xe6d1;</i> "+text);
        }
    });

    /**
     * 单个加锁
     * jeff
     */
    $('.lock_file').on('click', function () {
        var url  = "/file/lock_files.html";
        var unique_hash = $(this).attr('unique_hash');
        if (typeof(unique_hash) == 'undefined' || unique_hash.length < 1) {
            layer.msg($.lang('get', 'SELECT_FILE_LOCK'),{icon:7});
            return false;
        }

        var origin_hashes = new Array();
        origin_hashes.push(unique_hash);

        var obj = {'reload':true,'need_tip':true};
        var result = $.base('postData', url, 'origin_hashes='+origin_hashes, obj);
    });

    /**
     * 单个解锁
     * jeff
     */
    $('.unlock_file').on('click', function () {
        var url  = "/file/unlock_files.html";
        var unique_hash = $(this).attr('unique_hash');
        if (typeof(unique_hash) == 'undefined' || unique_hash.length < 1) {
            layer.msg($.lang('get', 'SELECT_FILE_UNLOCK'),{icon:7});
            return false;
        }
        var origin_hashes = new Array();
        origin_hashes.push(unique_hash);

        var obj = {'reload':true,'need_tip':true};
        var result = $.base('postData', url, 'origin_hashes='+origin_hashes, obj);
    });

    /**
     * 上传文件
     * jeff
     */
    $(".upload_file_btn").on('click', function() {
        if(privilege == 1) {
            layer.msg($.lang('get', 'NO_PRIVILEGE'),{icon:7});
            return false;
        }
        
        var url         = "/file/upload_file_new";
        var project_id  = $(this).attr('project_id');
        if (typeof(project_id) != 'undefined' && project_id.length > 0) {
            url += "/project_id/"+project_id;
        }
        var parent_hash = $(this).attr('parent_hash');
        if (typeof(parent_hash) != 'undefined' && parent_hash.length > 0) {
            url += "/parent_hash/"+parent_hash;
        }
        url += ".html";

        $.dialog({
            type:'loader',
            title:$.lang('get', 'UPLOAD_FILE'),
            width:600,
            height:200,
            draggable:true,
            msg:$.lang('get', 'UPLOAD_FILE'),
            url:url,
            async:true,    
            method:'get',
            loadBeforeText:$.lang('get', 'LOADING'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            confirmBtnTxt:false,
            //showBtn:false,
            confirmFn:function(){
                return true;
            }
        });
    });

    /**
     * 显示文件
     * jeff
     */
    $(".file_detail").on('click',function(){
        var unique_hash = $(this).attr('unique_hash');
        if (typeof(unique_hash) == 'undefined' || unique_hash.length < 1) {
            layer.msg($.lang('get', 'WRONG_PARAMETER'),{icon:7});
            return false;
        }
        var url         = "/file/file_detail/unique_hash/"+unique_hash;
        $.dialog({
            type:'loader',
            title:$.lang('get', 'FILE_CONTENT'),
            width:800,
            height:420,
            draggable:true,
            msg:$.lang('get', 'FILE_CONTENT'),
            url:url,
            async:true,    
            method:'get',
            loadBeforeText:$.lang('get', 'LOADING'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            confirmBtnTxt:false,
        });
    });

    /**
     * 批量下载文件
     * jeff
     */
    $(".download_files").on('click',function(){
        var origin_hashes = [];
        if(member_type == '2') {
            //客户下载判断
            var has_lock = 0;
            $(".origin_hashes:checked").each(function(i, data){
                var status = $(this).closest('tr').find('td').find('i').hasClass('lock');
                if(status == true) {
                    has_lock = 1;
                }else{
                    origin_hashes.push($(data).val());
                }
            });
            
            if(has_lock && origin_hashes.length == 0) {
                layer.msg($.lang('get', 'LOCK_FILE_CAN_NOT_DOWNLOAD'),{icon:7});
                return false;
            }
            if(has_lock) {
                layer.msg($.lang('get', 'CHOOSE_DOWNLOAD_FILE'),{icon:7});
            }else if (origin_hashes.length == 0) {
                layer.msg($.lang('get', 'CHOOSE_DOWNLOAD_FILE'),{icon:7});
                return false;
            }
            
            $("#form_download").submit();
        }else{
            //生信人员
            $(".origin_hashes:checked").each(function(i, data){
                origin_hashes.push($(data).val());
            });
            if (origin_hashes.length == 0) {
                layer.msg($.lang('get', 'CHOOSE_DOWNLOAD_FILE'),{icon:7});
                return false;
            }
            
            $("#form_download").submit();
        }

    });

    // 全选/取消全选
    $(".checkAll-label").on("click",function(){
        if($(this).find("input").is(":checked")){
            $(this).parentsUntil(".table_simple_wrap").find("input[type='checkbox']").attr("checked",true);
        }else{
            $(this).parentsUntil(".table_simple_wrap").find("input[type='checkbox']").attr("checked",false);
        }
    });

    //自动刷新
    $(function(){
       $(".origin_hashes").removeAttr('checked');
       //$(":checkbox").removeAttr('checked');
       // $("#keyword").val('');
    });

    $(".ico_search").click(function(){
        var reg=new RegExp("[\.]","g");
        var keyword = $("#keyword").val();
        keyword     = keyword.replace(reg, '|');
        $("#form1").submit();
    });

    /**
     * 批量删除
     *
     **/
    $(".del_file").on('click',function(){
        
        if(!confirm($.lang('get', 'CONFIRM_DELETE_FILE'))) {
            return false;
        }
        var url        = "/file/delete_files.html";

        if ($(".origin_hashes:checked").length == 0) {
            layer.msg($.lang('get', 'CHOOSE_DELETE_FILE'),{icon:7});
            return false;
        }
        var origin_hashes = new Array();
        var is_false = false;
        $.each($(".origin_hashes:checked"), function(i, data){
            var is_lock = $(this).attr('is_lock');
             if(is_lock == '1' && member_type == '2') {
                layer.msg($.lang('get', 'LOCK_FILE_CAN_NOT_DELETE'),{icon:7});
                is_false = true;
                return false;
            }else{
                origin_hashes.push($(data).val());
            }
        });
        if(true === is_false)
            return false;
        $.base('postData', url, 'origin_hashes='+origin_hashes, {'need_tip':true,'reload':true});
    });

    /**
     * 获取上传码
     *
     * zhouyong
     **/
    $('.get_upload_code').on('click', function() {
        if ($.trim(unique_hash) == '') {
            layer.msg($.lang('get', 'CHOOSE_PROJECT_GET_UPCODE'),{icon:7});
            return false;
        }
        var data = {type:'upload', unique_hash:unique_hash};
        var url         = "/file/get_code.html";
        $.dialog({
            type:'loader',
            title:$.lang('get', 'GET_UPCODE'),
            width:350,
            height:80,
            draggable:true,
            msg:$.lang('get', 'FILE_CONTENT'),
            url:url,
            async:true,    
            method:'post',
            data:data,
            loadBeforeText:$.lang('get', 'LOADING'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            confirmBtnTxt:false,
        });
    });

    /**
     * 获取下载码
     *
     * zhouyong
     **/
    $('.get_download_code').on('click', function() {
        var unique_hash = $(this).parentsUntil('tr').parent().find('.origin_hashes').val();
        if ($.trim(unique_hash) == '') {
            layer.msg($.lang('get', 'DIR_CAN_NOT_GET_DOWNCODE'),{icon:7});
            return false;
        }
        var data = {type:'download', unique_hash:unique_hash};
        var url         = "/file/get_code.html";
        $.dialog({
            type:'loader',
            title:$.lang('get', 'GET_DOWNCODE'),
            width:350,
            height:80,
            draggable:true,
            msg:$.lang('get', 'FILE_CONTENT'),
            url:url,
            async:true,    
            method:'post',
            data:data,
            loadBeforeText:$.lang('get', 'LOADING'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            confirmBtnTxt:false,
        });
    });

    $('.search-bar button').on('click',function(){
        $.base('loading', 'open', $.lang('get', 'SEARCHING'));
    });
});