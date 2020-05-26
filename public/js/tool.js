;(function($, window, document) {
    "use strict";
    $.extend({
        tool: function(fnName, param1, param2) {
            var fn = $.extend({}, cmd),handler;
            if (fn[fnName] == undefined){
                console.log(fnName);
                return false;
            }
            handler = fn[fnName](param1, param2);
            return handler;
        }
    });

    var tool = {
        
    };
})(jQuery, window, document);


    $('.parameter_step .block').eq(0).find(".content").slideDown();
//    $('.parameter_step .block').eq(0).find(".title").find('i').attr('class','fold icon-angle-up');
    $('.parameter_step .block .title').click(function(){
        if($(this).next('.content').is(":visible")){
             $(this).next('.content').slideUp(200);
//           $(this).find('i').attr('class','fold icon-angle-down');
        }else{
             $(this).next('.content').slideDown(200).css({"overflow":"visible"});
             $(this).parent().siblings().find('.content').slideUp(200);
//           $(this).find('i').attr('class','fold icon-angle-up');
//           $(this).parent().siblings().find('.title').find('i').attr('class','fold icon-angle-down');
        }        
    });
    /**
     * 项目自动填充
     *
     **/
    $(".autocomplete_project_name").autoComplete({
        data:json_project_infos,
        number: 300,
        autoFocus: true,
        menu: '.project-menu',
        showBtmBar:true,
        callBack: function(data,self) {
            if(typeof(data.name) == 'undefined' || typeof(data.id) == 'undefined' || data.name.length < 1 || data.id.length < 1){
                return false;
            }
            $('#project_name').val(data.name);
            $('#project_id').val(data.id);

            $('.select_file_dir').attr('parent_hash', data.parent_hash);
            get_task_infos_by_project_id(1);
        }
    });

    /**
     * 新建项目
     *
     **/
    $(".project-menu .autocomplete-eventBtn").live("click",function(){
        var project_name = $('#project_name').val();
        url = '/Project/add/show_name/project_name';
        if(typeof(project_name) != 'undefined' && project_name.length > 0){
            url += '/project_name/'+encodeURIComponent(project_name);
        }
        url += '.html';

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

    $(".task-menu .autocomplete-eventBtn").live("click",function(){
        var project_id = $("#project_id").val();
        if (typeof(project_id)== 'undefined' || isNaN(project_id)) {
            $.tip({width:300, position:'center', type:'warning',msg:$.lang('get', 'WRONG_PARAMETER'), keep:1,});
            return false;
        }
        var url = '/task/add/project_id/'+project_id;

        var task_title = $("#task_title").val();
        if(typeof(task_title) != 'undefined' && task_title.length > 0){
            url += '/task_title/'+encodeURIComponent(task_title);
        }

        var return_type = $("#return_type").val();
        if(typeof(return_type) != 'undefined' && return_type.length > 0){
            url += '/return_type/'+encodeURIComponent(return_type);
        }
        
        url += '.html';
        $.dialog({
            type:'loader',
            title:$.lang('get', 'NEW_TASK'),
            width:500,
            height:140,
            draggable:true,
            msg:$.lang('get', 'NEW_TASK'),
            url:url,
            async:true,    
            method:'get',
            loadBeforeText:$.lang('get', 'LOADING'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            confirmBtnTxt:$.lang('get', 'OK'),
            confirmFn:function(){
                var return_data = $.task('confirmAddTask');
                if(!isNaN(parseInt(return_data))){
                    $('#task_id').val(return_data);
                    return true;
                }
                return return_data;
            }
            /*,
            cancelFn:function(){
                alert("点击取消");
            }*/
        });
    });

    

    /**
     * tab切换
     *
     **/
    $(".tool_tab li").click(function(){
        var index = $(this).index();
        $(this).addClass('current').siblings().removeClass('current');
        $('.iterm_wrap').hide();
        $('.iterm_wrap:eq('+index+')').show();
    });

    /**
     * 输入参数
     *
     **/
    $('.module_info').find(".parameter_ids").blur(function() {
        var attribute_type = $(this).attr('attribute_type');
        // console.log(attribute_type);
        if (attribute_type == 'float') {
            var value = $(this).val();
            var gt = $(this).attr('gt');
            var lt = $(this).attr('lt');
            var egt = $(this).attr('egt');
            var elt = $(this).attr('elt');
            if (!$.verify('isFloat',value)) {
                alert($.lang('get', 'TYPE_WRONG'));
                $(this).val($(this).attr('default_value')).focus();
                return false;
            }
            if (typeof(gt) != 'undefined' && typeof(lt) != 'undefined') {
                if (parseFloat(value) >= parseFloat(lt) || parseFloat(value) <= parseFloat(gt)) {
                    alert($.lang('get', 'EXCEED_RANGE'));
                    $(this).val($(this).attr('default_value')).focus();
                    return false;
                }
            } else if (typeof(gt) != 'undefined' && typeof(elt) != 'undefined') {
                if (parseFloat(value) > parseFloat(elt) || parseFloat(value) <= parseFloat(gt)) {
                    alert($.lang('get', 'EXCEED_RANGE'));
                    $(this).val($(this).attr('default_value')).focus();
                    return false;
                }
            } else if (typeof(egt) != 'undefined' && typeof(lt) != 'undefined') {
                if (parseFloat(value) >= parseFloat(lt) || parseFloat(value) < parseFloat(egt)) {
                    alert($.lang('get', 'EXCEED_RANGE'));
                    $(this).val($(this).attr('default_value')).focus();
                    return false;
                }
            } else if (typeof(egt) != 'undefined' && typeof(elt) != 'undefined') {
                if (parseFloat(value) > parseFloat(elt) || parseFloat(value) < parseFloat(egt)) {
                    alert($.lang('get', 'EXCEED_RANGE'));
                    $(this).val($(this).attr('default_value')).focus();
                    return false;
                }
            }
        } else if (attribute_type == 'int') {
            var value = $(this).val();
            var gt = $(this).attr('gt');
            var lt = $(this).attr('lt');
            var egt = $(this).attr('egt');
            var elt = $(this).attr('elt');
            if (!$.verify('isInt',value)) {
                alert($.lang('get', 'TYPE_WRONG'));
                $(this).focus();
                return false;
            }
            if (typeof(gt) != 'undefined' && typeof(lt) != 'undefined') {
                if (parseFloat(value) >= parseFloat(lt) || parseFloat(value) <= parseFloat(gt)) {
                    alert($.lang('get', 'EXCEED_RANGE'));
                    $(this).val($(this).attr('default_value')).focus();
                    return false;
                }
            } else if (typeof(gt) != 'undefined' && typeof(elt) != 'undefined') {
                if (parseInt(value) > parseInt(elt) || parseInt(value) <= parseInt(gt)) {
                    alert($.lang('get', 'EXCEED_RANGE'));
                    $(this).val($(this).attr('default_value')).focus();
                    return false;
                }
            } else if (typeof(egt) != 'undefined' && typeof(lt) != 'undefined') {
                if (parseInt(value) >= parseInt(lt) || parseInt(value) < parseInt(egt)) {
                    alert($.lang('get', 'EXCEED_RANGE'));
                    $(this).val($(this).attr('default_value')).focus();
                    return false;
                }
            } else if (typeof(egt) != 'undefined' && typeof(elt) != 'undefined') {
                if (parseInt(value) > parseInt(elt) || parseInt(value) < parseInt(egt)) {
                    alert($.lang('get', 'EXCEED_RANGE'));
                    $(this).val($(this).attr('default_value')).focus();
                    return false;
                }
            }
        }
    });

    /**
     * 显示参考示例
     *
     **/
    $(".show-sample").click(function(){
        var id  = $(this).attr("parameter-id");
        var url = '/file/show_sample';
        if(typeof(id) != 'undefined' && id.length > 0){
            url += '/parameter_id/'+id;
        }
        url += '.html';

        $.dialog({
            width:700,
            height:'auto',
            type:'loader',
            title:$.lang('get', 'DEMO'),
            cancelBtnTxt:$.lang('get', 'CLOSE'),
            url:url,
            confirmBtnTxt:false
        });
    });

    $('.module_info').find("input:checkbox").live('click', function() {
        controllDisplay(2, {en_name:$(this).parent().parent().parent().attr('class'),value:$(this).val()});
    });

       initalRawCleanData();
    initalRefDatabase(true);
    function initalRawCleanData()
    {
        var cmd_commands =['toolapps.humann_reads','toolapps.kraken','toolapps.sortmerna','toolapps.metaphlan','toolapps.centrifuge'];
        if( $.inArray(cmd_command, cmd_commands)!= -1){
            if($('.qc input:checked').val() =='true'){
                $('.in_fastq').show();
                $('.clean_fq').hide();
                $('.rm_host').show();
                $('.ref_database').show();
                $('.ref_undefined').show();
                $('.ref_undefined_name').show();
                if($('.ref_database').find('select[en_name="ref_database"]').find('option:selected').val()!='Custom'){
                    $('.ref_undefined').hide();
                    $('.ref_undefined_name').hide();
                }
            }else{
                $('.clean_fq').show();
                $('.in_fastq').hide();
                $('.rm_host').hide();
                $('.ref_database').hide();
                $('.second_ref').hide();
                $('.ref_undefined').hide();
                $('.ref_undefined_name').hide();
            }
        }
        
    }
    function initalRefDatabase(is_inital)
    {

        var cmd_commands =['toolapps.humann_reads','toolapps.kraken','toolapps.sortmerna','toolapps.metaphlan','toolapps.centrifuge'];
        if( $.inArray(cmd_command, cmd_commands)!= -1){
            $parent = $('.ref_database').find('select[en_name="ref_database"]').parent();
            $('.second_ref select').html('');
            var ref_database = $('.ref_database').find('select[en_name="ref_database"]').find('option:selected').val();
            //no 不等于Custom
            if($('.rm_host input:checked').val() =='false'){
                $('.second_ref').hide();
                $('.ref_database').hide();
                $('.second_ref select').html('');
                $('.ref_undefined').hide();
                $('.ref_undefined_name').hide();
            //yes 不等于Custom
            }else if(ref_database!='Custom' && $('.rm_host input:checked').val() =='true'){
        
                for(var i in json_ens_ref_metag[ref_database]){
                    var selected="";
                    if( !$.isEmptyObject(task_workflow_parameter)){
          
                        if(json_ens_ref_metag[ref_database][i]['category2'] ==task_workflow_parameter.second_ref.value &&is_inital !=undefined){
                            selected ='selected="selected"';
                        }
                    }
                    $('.second_ref select').append('<option '+selected+' value="'+json_ens_ref_metag[ref_database][i]['category2']+'">'+json_ens_ref_metag[ref_database][i]['category2']+'</option>');
                }
                $('.ref_database').show();
                $('.second_ref').show();
                $('.ref_undefined').hide();
                $('.ref_undefined_name').hide();
            //yes 等于Custom
            }else if($('.rm_host input:checked').val() =='true' && ref_database=='Custom'){
                $('.second_ref select').html('');
                $('.ref_undefined').show();
                $('.ref_database').show();
                $('.ref_undefined_name').show();
                $('.second_ref').hide();
            }else if($('.rm_host input:checked').val() =='false'){
                $('.second_ref select').html('');
                $('.ref_undefined').hide();
                $('.ref_database').hide();
                $('.second_ref').hide();
                $('.ref_undefined_name').hide();
            }
        }
    }
    function initalRmHostCleanData()
    {
        var cmd_commands =['toolapps.humann_reads','toolapps.kraken','toolapps.sortmerna','toolapps.metaphlan','toolapps.centrifuge'];
        if( $.inArray(cmd_command, cmd_commands)!= -1){
            if($('.rm_host input:checked').val() =='false'){
                $('.ref_database').hide();
                $('.ref_undefined').hide();
                $('.ref_undefined_name').hide();
            }else{
                $('.ref_database').show();
                $('.ref_undefined').show();
                $('.ref_undefined_name').show();
            }
        }
    }
    $('select[en_name="ref_database"]').live('change',function(){
        initalRefDatabase();
    });
    $('.qc input').on('click',function(){
        initalRawCleanData();
    });
    $('.rm_host input').on('click',function(){
        initalRefDatabase();
    });
    function initalDatabaseShow(database){
        if(database =='custom_mode'){
            $('.ref_fasta').show();
            $('.ref_taxon').show();
            $('.confidence').show();
        }else if(database =='nt'){
            $('.ref_fasta').hide();
            $('.ref_taxon').hide();
            $('.confidence').hide();
        }else{
            $('.ref_fasta').hide();
            $('.ref_taxon').hide();
            $('.confidence').show();
        }
    }
    function addAnalysis(){
        var task_id = $('#task_id').val();
        var cmd_id  = $('input[name="cmd_id"]').val();
        if(cmd_command =='meta.meta_base'){
            return true;
        }
        var data = [
                {name:'cmd_id',value:cmd_id},
                {name:'task_id',value:task_id},
            ];
            var checkeds =[];
            $('input[name="category_ids[]"]:checked').each(function(){
                checkeds.push($(this).val());
            });
            var not_checkeds =[];
            $('input[name="category_ids[]"]').each(function(){
                if(!$(this).is(":checked")){
                    not_checkeds.push($(this).val());
                }
                
            });
            data.push({name:"checkeds",value:checkeds.join(';')});
            data.push({name:"not_checkeds",value:not_checkeds.join(';')});
            console.log(not_checkeds);
            console.log(checkeds);
        $.ajax({
            type    : 'POST',
            url     : "/tool/add_analysis",
            data    : data,
            dataType: 'json',
            success: function(data) {
                if(data.success === 'true'){
                    $.tip({width:300, position:'center', type:'success',msg:data.message, keep:1,});
                    //window.location.reload();
                }else{
                    $.tip({width:300, position:'center', type:'warning',msg:data.message, keep:1,});
                    return false;
                }
                
            }
        });
    }
    $('.module_info').find("select").change(function() {
        if (typeof($(this).find("option:selected").attr('type')) != 'undefined') {
            if($(this).parent().parent().attr('class') != 'genome_structure_file') {
                controllDisplay(2, {en_name:$(this).parent().parent().attr('class'),value:$(this).find("option:selected").attr('type')});
            }
            
        } else {
            if((cmd_command =='meta.meta_agri' ||cmd_command =='meta.meta_med') && $(this).attr('en_name') =='database'){
                initalDatabaseShow($(this).val());
            }else{
                if($.inArray($(this).parent().parent().attr('class'),['nr_database','kegg_database','strand_specific','assemble_method','seq_method','express_method','diff_method','exp_way','is_duplicate','fq_type','strand_dir','calculation']) == -1) {
                    controllDisplay(2, {en_name:$(this).parent().parent().attr('class'),value:$(this).find("option:selected").val()});
                } 
            }
            
        }
    });
    $('.module_info').find("input:radio").live('click', function() {
        controllDisplay(2, {en_name:$(this).parent().parent().parent().attr('class'),value:$(this).val()});
    });
    controllDisplay(1);
    if(typeof($('.ref_genome').find('select option:selected').val()) != 'undefined') {
        //setDefaultValue($('.ref_genome'), '', $('.ref_genome').find('select option:selected').val());
        if($('.ref_genome').find('select option:selected').val() != 'Custom') {
            $('.ref_genome_custom').hide();
            $('.genome_structure_file').hide();
            $('.go_upload_file').hide();
            $('.kegg_upload_file').hide();
        }else {
            $('.ref_genome_custom').show();
            $('.genome_structure_file').show();
            $('.go_upload_file').show();
            $('.kegg_upload_file').show();
        }
    }
    


    $('.module_info').find('tr.in_fastq select:first').change(function (){
        $('#file_hidden').val('');
        $('#file_hidden').next().val('');
        $('#sample_list').hide().find('tbody').empty();
    });



    /**
     * 保存参数
     *
     **/
    $(".save_parameter").click(function() {
        if ($('#project_id').val() == '0' ||$("#project_name").val()=='') {
            alert($.lang('get', 'ASSOCIATE_PROJECT'));
            return false;
        }
        if($('#task_id').val() == '0' || $("#task_title").val()==''){
            alert($.lang('get', 'ASSOCIATE_TASK'));
            return false;
        }
        
        postData(1);
    });
    /**
     * 运行参数
     *
     **/
    $(".run_parameter").click(function() {
        if ($('#project_id').val() == '0'  ||$("#project_name").val()=='') {
            alert($.lang('get', 'ASSOCIATE_PROJECT'));
            return false;
        }
        if($('#task_id').val() == '0' || $("#task_title").val()==''){
            alert($.lang('get', 'ASSOCIATE_TASK'));
            return false;
        }
        
        postData(2);
    });
    $(document).on('change','.database_type',function(){
        if(cmd_command =='meta.meta_agri'){
            var databases = meta_meta_agri;
        }else{
            var databases = meta_meta_med;
        }
        var database_type = $('.database_type').find('option:selected').val();
        if(database_type){
            initialDatabase(database_type,databases);  
        }
    });

    /**
     * 初始化数据库类型和数据库
     *
     **/
     function initialDatabase(database_type,databases)
     {

        $('select[en_name="database"]').html('<option value="">--请选择--</option>');
        var database_vs =[];
        if(task_workflow_parameter!=null && task_workflow_parameter.length !=0){
            var database_vs = task_workflow_parameter['database'].value.split(';');
        }
        for(var i in databases['datatype_to_database'][database_type] ){
            var selected ="";
            if(database_vs.length>0 && databases['datatype_to_database'][database_type][i] ==database_vs[1]){
                selected ='selected="selected"';
            }
            var display ="";
            if(login_member ==2 && databases['datatype_to_database'][database_type][i] =='Protist_PR2_v4.5'){
                display ='style="display:none;"';
            }
            $('select[en_name="database"]').append('<option '+ display+ ' '+selected+' value="'+databases['datatype_to_database'][database_type][i]+'">'+databases['datatype_to_database'][database_type][i]+'</option>')
        }
        
    }
    /**
     * 初始化数据库类型和数据库
     *
     **/
     function initialDatabaseType()
    {
        if(cmd_command =='meta.meta_agri' ||cmd_command =='meta.meta_med'){
            if(cmd_command =='meta.meta_agri'){
                var databases = meta_meta_agri;
            }else{
                var databases = meta_meta_med;
            }
            if(task_workflow_parameter==null || task_workflow_parameter.length ==0){
                var database_vs =[];
            }else{
                var database_vs = task_workflow_parameter['database'].value.split(';');
                console.log(database_vs);
            }
            $('select[en_name="database"]').before('<select class="database_type" name="database_type"></select> ');
            for(var i in databases['datatype_to_database']){
                var text_ = i;
                var selected ='';
                if(database_vs.length>0 && i==database_vs[0]){
                     selected = 'selected="selected"';
                }
               
                if(text_ =='function_gene'){
                    text_ ='功能基因';
                }else if(text_ =='others' ){
                    text_ ='其他';
                }
                $('.database_type').append('<option '+selected+' value="'+i+'">'+text_+'</option>');
            }
            var database_type = $('.database_type').find('option:selected').val();
            if(database_type){
                initialDatabase(database_type,databases,);  
            }
        }
     }


    /**
     * 运行
     *
     **/
    function postData(save,id)
    {

        if($('#sample_list tbody').find('tr').length != 0){
            var reg_name=/^[a-zA-Z0-9_]{0,}$/;
            var sample_error_message ='';
            $('#sample_list tbody').find('input[name="replace[]"]').css('border','1px solid #CCCCCC');
            $('#sample_list tbody').find('input[name="select[]"]').each(function(){
                if($(this).is(":checked")){
                    var current_replace_specimen = $(this).parent().parent().find('input[name="replace[]"]').val();
                    if(!reg_name.test(current_replace_specimen) || current_replace_specimen.substr(current_replace_specimen.length-1,1) =='_'){
                        sample_error_message = '分组方案必须以字母或数字开头，不能含有空格，并且不能以下划线结尾，支持字母，数字，下划线:'+current_replace_specimen;
                        $(this).parent().parent().find('input[name="replace[]"]').css('border','1px solid red');
                        return false;
                    }
                }
            });
            if(sample_error_message !=''){
                alert(sample_error_message);
                return false;
            }else{
                $('#sample_list tbody').find('input[name="replace[]"]').css('border','1px solid #CCCCCC');
            }
        }
        
        var message = '';
        var  flag = '';
        $('.parameter_step .block table tr').each(function(i, data) {
            if ($(data).css('display') != 'none') {
                if ($(data).attr('show_type') == 'upload' && $(data).find("span:eq(0)").hasClass('must')) {
                    if ($(data).find("input").val() == '') {
                        message = $(data).attr('name')+$.lang('get', 'CAN_NOT_EMPTY')+"\n";
                        flag = $(data);
                    }
                } else if ($(data).attr('show_type') == 'dropbox' && $(data).find("span:eq(0)").hasClass('must')) {
                    if ($(data).find("option:selected").val() == '') {
                        message = $.lang('get', 'PLEASE_SELECT')+$(data).attr('name')+"\n";
                        flag = $(data);
                    }
                } else if ($(data).attr('show_type') == 'checkbox' && $(data).find("span:eq(0)").hasClass('must')) {
                    if ($(data).find("input:checked").length == 0) {
                        message = $.lang('get', 'PLEASE_SELECT')+$(data).attr('name')+"\n";
                        flag = $(data);
                    }
                } else if ($(data).attr('show_type') == 'radio' && $(data).find("span:eq(0)").hasClass('must')) {
                    if ($(data).find("input:checked").length == 0) {
                        message = $.lang('get', 'PLEASE_SELECT')+$(data).attr('name')+"\n";
                        flag = $(data);
                    }
                } else if ($(data).attr('show_type') == 'text' && $(data).find("span:eq(0)").hasClass('must')) {
                    if ($.trim($(data).find("input").val()) == '') {   
                        message = $(data).attr('name')+$.lang('get','CAN_NOT_EMPTY')+"\n";
                        flag = $(data);
                    }
                }           
           }
            
        });
        if($('select[en_name="database"]').find('option:selected').val() ==''){
            message = $.lang('get', 'PLEASE_SELECT')+$('.database').attr('name')+"\n";
        }
        if (message != '') {
            alert(message);
            var  perentul =   flag.parentsUntil(".block").parent();
            perentul.find('.content').slideDown(200);
            perentul.siblings().find('.content').slideUp(200);
            perentul.find('i.fold icon-angle-up').attr('class','fold icon-angle-down');
            perentul.siblings().find('i.fold icon-angle-up').attr('class','fold icon-angle-down');  
            return false;
        }
       
       // $(".submit_loading").append('<i class="ico_loading"></i>提交中...');
        $("#save").val(save);
        
        if(cmd_command =='meta.meta_agri' ||cmd_command =='meta.meta_med'){
            addAnalysis();
        }
        //var url = "/tool/add.html";
        var url = $('#form_parameter').attr('action');
        var message = '';
        var task_id = $('#task_id').val();
        var cmd_id  = $('input[name="cmd_id"]').val();
        var post = $("#form_parameter").serialize();
        post += '&old_task_id=' + $('#task_id').attr('old_task_id');
        $.ajax({
            type    : 'POST',
            url     : url,
            data    : post,
            dataType: 'JSON',
            beforeSend:function(){
               $(".save_parameter").addClass('disabled');
               $(".run_parameter").addClass('disabled');
               $(".submit_loading").html('<i class="ico_loading"></i>'+$.lang('get', 'SUBMITING')); 
            },
            async   : false,
            success: function(data) {
                if (data.success == 'false') {
                    message = data.message;         
                    alert(message);
                    $(".save_parameter").removeClass('disabled');
                    $(".run_parameter").removeClass('disabled');
                    $(".submit_loading").html('');
           
                } else {
                    
                    alert(data.message);
                    var  project_id=data.data.project_id;
                    if(url.indexOf('add_tool') != -1){
                        if($('#is_visual').val()=='1'){
                            window.location.href="/task/project_tasks/project_id/"+project_id+".html";
                        }else{
                            window.location.href="/tool/add_tool/task_id/"+task_id+"/cmd_id/"+cmd_id+".html";
                        }
    
                        
                    }else{
                        $.cookie('task_run_tip',0,{path: '/' });
                        window.location.href="/task/project_tasks/project_id/"+project_id+".html";
                    }
                }
                  /*  $("#project_id").val('');
                    $("#project_name").val('');
                    $("#task_id").val('');
                    $("#task_title").val('');*/
            }
        });
       
    }

    /**
     * 控制显示值
     *
     **/
    function controllDisplay(initialize, current_parameter)
    {
        if (typeof(config_infos) == 'undefined') {
            return ;
        }
        var ranges = new Array();
        $('.parameter_step .block table tr').each(function(i, data) {
            var en_name = $(data).attr('class');
            var default_value = $(data).attr('default_value');
            var show_type = $(data).attr('show_type');
            var title = $(this).attr('name');
            if ($(data).css('display') != 'none') {
                if (show_type == 'checkbox') {
                    if (config_infos[en_name]) {
                        for (var j in config_infos[en_name]['values']) {
                            for (var k in config_infos[en_name]['values'][j]) {
                                var relation_en_name = config_infos[en_name]['values'][j][k].parameter;
                                var is_required   = $('.'+relation_en_name).attr('is_required');
                                var is_front_show = $('.'+relation_en_name).attr('is_front_show');
                                is_front_show == '1' && $('.'+relation_en_name).show();
                                is_front_show == '0' && $('.'+relation_en_name).hide();
                                if (is_required == '1') {
                                    if (!$('.'+relation_en_name).find("span:eq(0)").hasClass('must')) {
                                        $('.'+relation_en_name).find("span:eq(0)").addClass('must');
                                    }
                                    $('.'+relation_en_name).find("span:eq(0)").html('(<span style="color:#ff0000">*</span>)');
                                } else if (is_required == '2') {
                                    $('.'+relation_en_name).find("span:eq(0)").removeClass('must');
                                    $('.'+relation_en_name).find("span:eq(0)").html('');
                                }
                            }
                        }
                        
                        var change_object = {};
                        if ($(data).find("input:checked").length != 0) {
                            $(data).find("input:checked").each(function(ii, da) {
                                var value = $(da).val();

                                for (var j in config_infos[en_name]['values'][value]) {
                                    var relation_en_name = config_infos[en_name]['values'][value][j].parameter;
                                    var is_required      = config_infos[en_name]['values'][value][j].is_required;
                                    var is_front_show    = config_infos[en_name]['values'][value][j].is_front_show;
                                    
                                    is_front_show == 'Y' && $('.'+relation_en_name).css('display') == 'none' && $('.'+relation_en_name).show();
                                    is_front_show == 'N' && $('.'+relation_en_name).css('display') != 'none' && $('.'+relation_en_name).hide();
                                    
                                    if (is_required == 'Y') {
                                        if (!$('.'+relation_en_name).find("span:eq(0)").hasClass('must')) {
                                            $('.'+relation_en_name).find("span:eq(0)").addClass('must');
                                        }
                                        $('.'+relation_en_name).find("span:eq(0)").html('(<span style="color:#ff0000">*</span>)');
                                        change_object.relation_en_name = {
                                            is_required: 'Y'
                                        }
                                    } else if (is_required == 'N') {
                                        if (typeof(change_object.relation_en_name) != 'undefined' && change_object.relation_en_name.is_required == 'Y') {
                                            continue;
                                        }
                                        $('.'+relation_en_name).find("span:eq(0)").removeClass('must');
                                        $('.'+relation_en_name).find("span:eq(0)").html('');
                                    }
                                }
                                
                            });
                        }
                        for (var j in config_infos[en_name]['values']) {
                            for (var k in config_infos[en_name]['values'][j]) {
                                var relation_en_name = config_infos[en_name]['values'][j][k].parameter;
                                var default_value    = config_infos[en_name]['values'][j][k].default_value;
                                var current_value    = config_infos[en_name]['values'][j][k].current_value;
                                if (relation_en_name == 'permutations') {
                                    // console.log();
                                }
                                if (initialize == 2) {
                                    if (current_value != current_parameter.value) {
                                        // console.log(1);
                                        continue;
                                    }
                                    setDefaultValue($('.'+relation_en_name), default_value, current_parameter);
                                    ranges.push(relation_en_name);
                                } else if (initialize == 1) {
                                    setDefaultValue($('.'+relation_en_name), default_value, current_parameter);
                                }
                            }
                        }
                        
                    }
                } else if (show_type == 'dropbox') {
                    var value = $(data).find("select").find("option:selected").val();
                    if (config_infos[en_name]) {
                        for (var j in config_infos[en_name]['values']) {
                            for (var k in config_infos[en_name]['values'][j]) {
                                var relation_en_name = config_infos[en_name]['values'][j][k].parameter;
                                var is_required   = $('.'+relation_en_name).attr('is_required');
                                var is_front_show = $('.'+relation_en_name).attr('is_front_show');
                                is_front_show == '1' && $('.'+relation_en_name).show();
                                is_front_show == '0' && $('.'+relation_en_name).hide();
                                if (is_required == '1') {
                                    if (!$('.'+relation_en_name).find("span:eq(0)").hasClass('must')) {
                                        $('.'+relation_en_name).find("span:eq(0)").addClass('must');
                                    }
                                    $('.'+relation_en_name).find("span:eq(0)").html('(<span style="color:#ff0000">*</span>)');
                                } else if (is_required == '2') {
                                    $('.'+relation_en_name).find("span:eq(0)").removeClass('must');
                                    $('.'+relation_en_name).find("span:eq(0)").html('');
                                }
                            }
                        }
                        for (var j in config_infos[en_name]['values'][value]) {
                            var relation_en_name = config_infos[en_name]['values'][value][j].parameter;
                            var is_required      = config_infos[en_name]['values'][value][j].is_required;
                            var is_front_show    = config_infos[en_name]['values'][value][j].is_front_show;
                            var values           = config_infos[en_name]['values'][value][j].values;
                            var default_value    = config_infos[en_name]['values'][value][j].default_value;

                            is_front_show == 'Y' && $('.'+relation_en_name).show();
                            is_front_show == 'N' && $('.'+relation_en_name).hide();
                            if (is_required == 'Y') {
                                if (!$('.'+relation_en_name).find("span:eq(0)").hasClass('must')) {
                                    $('.'+relation_en_name).find("span:eq(0)").addClass('must');
                                }
                                $('.'+relation_en_name).find("span:eq(0)").html('<span style="color:#ff0000">*</span>');
                            } else if (is_required == 'N') {
                                $('.'+relation_en_name).find("span:eq(0)").removeClass('must');
                                $('.'+relation_en_name).find("span:eq(0)").html('');
                            }

                            
                            if (typeof(values) != 'undefined') {
                                var html = '<option value="">--'+$.lang('get', 'PLEASE_SELECT')+'--</option>';
                                for (var k in values) {
                                    if(initialize == 2 && typeof(current_parameter.value) != 'undefined' && values[k] == current_parameter.value) {
                                        html += '<option value="'+values[k]+'" selected="selected">'+values[k]+'</option>';
                                    }else {
                                        html += '<option value="'+values[k]+'">'+values[k]+'</option>';
                                    }
                                    
                                }
                                $('.'+relation_en_name).find('select[en_name='+relation_en_name+']').html(html);

                            }

                            if (typeof(default_value) != 'undefined') {
                                $('.'+relation_en_name).find('select option[value='+default_value+']').attr('selected', 'selected');
                            }
                        }

                        for (var j in config_infos[en_name]['values']) {
                            for (var k in config_infos[en_name]['values'][j]) {
                                var relation_en_name = config_infos[en_name]['values'][j][k].parameter;
                                var default_value    = config_infos[en_name]['values'][j][k].default_value;
                                var current_value    = config_infos[en_name]['values'][j][k].current_value;
                                if (initialize == 2) {
                                    if (ranges.join('') != '') {
                                        var range_string = '|'+ranges.join('|')+'|';
                                        if (range_string.indexOf(en_name)) {
                                            var selected_value = $("."+en_name).find("select").find("option:selected").val();
                                            if (selected_value == current_value) {
                                                setDefaultValue($('.'+relation_en_name), default_value, {en_name:en_name,value:selected_value});
                                            }
                                        }
                                    } else if (current_value != current_parameter.value) {
                                        continue;
                                    } else {
                                        setDefaultValue($('.'+relation_en_name), default_value, current_parameter);
                                    }
                                } else if (initialize == 1) {
                                    setDefaultValue($('.'+relation_en_name), default_value, current_parameter);
                                }
                            }
                        }
                    }   
                } else if (show_type == 'upload') {
                    var value = $(data).find("select").find("option:selected").attr('type');
                    if (typeof(value) != 'undefined' && config_infos[en_name]) {
                        for (var j in config_infos[en_name]['values']) {
                            for (var k in config_infos[en_name]['values'][j]) {
                                var relation_en_name = config_infos[en_name]['values'][j][k].parameter;
                                var is_required   = $('.'+relation_en_name).attr('is_required');
                                var is_front_show = $('.'+relation_en_name).attr('is_front_show');
                                is_front_show == '1' && $('.'+relation_en_name).show();
                                is_front_show == '0' && $('.'+relation_en_name).hide();
                                if (is_required == '1') {
                                    if (!$('.'+relation_en_name).find("span:eq(0)").hasClass('must')) {
                                        $('.'+relation_en_name).find("span:eq(0)").addClass('must');
                                    }
                                    $('.'+relation_en_name).find("span:eq(0)").html('(<span style="color:#ff0000">*</span>)');
                                } else if (is_required == '2') {
                                    $('.'+relation_en_name).find("span:eq(0)").removeClass('must');
                                    $('.'+relation_en_name).find("span:eq(0)").html('');
                                }
                            }
                        }

                        for (var j in config_infos[en_name]['values'][value]) {
                            var relation_en_name = config_infos[en_name]['values'][value][j].parameter;
                            //console.log(relation_en_name);
                            var is_required      = config_infos[en_name]['values'][value][j].is_required;
                            var is_front_show    = config_infos[en_name]['values'][value][j].is_front_show;
                            
                            is_front_show == 'Y' && $('.'+relation_en_name).show();
                            is_front_show == 'N' && $('.'+relation_en_name).hide();
                            
                            if (is_required == 'Y') {
                                if (!$('.'+relation_en_name).find("span:eq(0)").hasClass('must')) {
                                    $('.'+relation_en_name).find("span:eq(0)").addClass('must');
                                }
                                $('.'+relation_en_name).find("span:eq(0)").html('(<span style="color:#ff0000">*</span>)');
                            } else if (is_required == 'N') {
                                $('.'+relation_en_name).find("span:eq(0)").removeClass('must');
                                $('.'+relation_en_name).find("span:eq(0)").html('');
                            }
                        }

                        for (var j in config_infos[en_name]['values']) {
                            for (var k in config_infos[en_name]['values'][j]) {
                                var relation_en_name = config_infos[en_name]['values'][j][k].parameter;
                                var default_value    = config_infos[en_name]['values'][j][k].default_value;
                                var current_value    = config_infos[en_name]['values'][j][k].current_value;
                                if (initialize == 2) {
                                    if (ranges.join('') != '') {
                                        var range_string = '|'+ranges.join('|')+'|';
                                        if (range_string.indexOf(en_name)) {
                                            var selected_value = $("."+en_name).find("select").find("option:selected").val();
                                            if (selected_value == current_value) {
                                                setDefaultValue($('.'+relation_en_name), default_value, {en_name:en_name,value:selected_value});
                                            }
                                        }
                                    } else if (current_value != current_parameter.value) {
                                        continue;
                                    } else {
                                        setDefaultValue($('.'+relation_en_name), default_value, current_parameter);
                                    }
                                } else if (initialize == 1) {
                                    //setDefaultValue($('.'+relation_en_name), default_value, current_parameter);
                                }
                            }
                        }
                    } else if (config_infos[en_name]){
                        var value = $(data).find("input").val();
                        var upload_value = '';
                        if (value == '') {
                            upload_value = 'null';
                        } else {
                            upload_value = 'not_null';
                        }
                        for (var j in config_infos[en_name]['values'][upload_value]) {
                            var relation_en_name = config_infos[en_name]['values'][upload_value][j].parameter;
                            var is_required      = config_infos[en_name]['values'][upload_value][j].is_required;
                            var is_front_show    = config_infos[en_name]['values'][upload_value][j].is_front_show;
                            var relation_show_type = $('.'+relation_en_name).attr('show_type');
                            var relation_values    = config_infos[en_name]['values'][upload_value][j].values;
                            if (relation_show_type == 'checkbox' && relation_values != '') {
                                relation_values = ','+relation_values.join(',')+',';
                                $('.'+relation_en_name).find(':checkbox').each(function(i, check_data){
                                    if (relation_values.indexOf(','+$(check_data).val()+',') !== -1) {
                                        if (is_front_show == 'N') {
                                            $(check_data).attr('disabled', 'disabled').parent().attr('title', ' After select '+title+' can use');
                                        } else if (is_front_show == 'Y'){
                                            $(check_data).removeAttr('disabled').parent().removeAttr('title');
                                        }
                                    }
                                }); 
                            }else if(relation_show_type == 'dropbox'){

                                if (is_front_show == 'N'  && value == '') {
                                    $('.'+relation_en_name).hide();
                                    $('.'+relation_en_name).find('select[en_name='+relation_en_name+']').find('option').removeClass('selected');
                                } else if (is_front_show == 'Y'&& value != ''){
                                    $('.'+relation_en_name).show();
                                    //$('.'+relation_en_name).find('select[en_name='+relation_en_name+']').find('option:eq(1)').attr('selected','selected');
                                }
                            }else if(relation_show_type == 'text'){
                                if (is_front_show == 'N'  && value == '') {
                                    $('.'+relation_en_name).hide();
                                    $('.'+relation_en_name).find('select[en_name='+relation_en_name+']').find('option').removeClass('selected');
                                } else if (is_front_show == 'Y'&& value != ''){
                                    $('.'+relation_en_name).show();
                                    //$('.'+relation_en_name).find('select[en_name='+relation_en_name+']').find('option:eq(1)').attr('selected','selected');
                                }
                            }
                            
                            if (is_required == 'Y') {
                                if (!$('.'+relation_en_name).find("span:eq(0)").hasClass('must')) {
                                    $('.'+relation_en_name).find("span:eq(0)").addClass('must');
                                }
                                $('.'+relation_en_name).find("span:eq(0)").html('(<span style="color:#ff0000">*</span>)');
                            } else if (is_required == 'N') {
                                $('.'+relation_en_name).find("span:eq(0)").removeClass('must');
                                $('.'+relation_en_name).find("span:eq(0)").html('');
                            }
                        }
                    }
                } else if (show_type == 'radio') {
                    var value = $(data).find("input:checked").val();
                    if (config_infos[en_name]) {
                        for (var j in config_infos[en_name]['values']) {
                            for (var k in config_infos[en_name]['values'][j]) {
                                var relation_en_name = config_infos[en_name]['values'][j][k].parameter;
                                var is_required   = $('.'+relation_en_name).attr('is_required');
                                var is_front_show = $('.'+relation_en_name).attr('is_front_show');
                                is_front_show == '1' && $('.'+relation_en_name).show();
                                is_front_show == '0' && $('.'+relation_en_name).hide();
                                if (is_required == '1') {
                                    if (!$('.'+relation_en_name).find("span:eq(0)").hasClass('must')) {
                                        $('.'+relation_en_name).find("span:eq(0)").addClass('must');
                                    }
                                    $('.'+relation_en_name).find("span:eq(0)").html('(<span style="color:#ff0000">*</span>)');
                                } else if (is_required == '2') {
                                    $('.'+relation_en_name).find("span:eq(0)").removeClass('must');
                                    $('.'+relation_en_name).find("span:eq(0)").html('');
                                }
                            }
                        }

                        for (var j in config_infos[en_name]['values'][value]) {
                            var relation_en_name = config_infos[en_name]['values'][value][j].parameter;
                            // console.log(relation_en_name);
                            var is_required      = config_infos[en_name]['values'][value][j].is_required;
                            var is_front_show    = config_infos[en_name]['values'][value][j].is_front_show;
                            
                            is_front_show == 'Y' && $('.'+relation_en_name).show();
                            is_front_show == 'N' && $('.'+relation_en_name).hide();
                            
                            if (is_required == 'Y') {
                                if (!$('.'+relation_en_name).find("span:eq(0)").hasClass('must')) {
                                    $('.'+relation_en_name).find("span:eq(0)").addClass('must');
                                }
                                $('.'+relation_en_name).find("span:eq(0)").html('(<span style="color:#ff0000">*</span>)');
                            } else if (is_required == 'N') {
                                $('.'+relation_en_name).find("span:eq(0)").removeClass('must');
                                $('.'+relation_en_name).find("span:eq(0)").html('');
                            }
                        }

                        for (var j in config_infos[en_name]['values']) {
                            for (var k in config_infos[en_name]['values'][j]) {
                                var relation_en_name = config_infos[en_name]['values'][j][k].parameter;
                                var default_value    = config_infos[en_name]['values'][j][k].default_value;
                                var current_value    = config_infos[en_name]['values'][j][k].current_value;
                                if (initialize == 2) {
                                    if (ranges.join('') != '') {
                                        var range_string = '|'+ranges.join('|')+'|';
                                        if (range_string.indexOf(en_name)) {
                                            var selected_value = $("."+en_name).find("select").find("option:selected").val();
                                            if (selected_value == current_value) {
                                                setDefaultValue($('.'+relation_en_name), default_value, {en_name:en_name,value:selected_value});
                                            }
                                        }
                                    } else if (current_value != current_parameter.value) {
                                        continue;
                                    } else {
                                        setDefaultValue($('.'+relation_en_name), default_value, current_parameter);
                                    }
                                } else if (initialize == 1) {
                                    setDefaultValue($('.'+relation_en_name), default_value, current_parameter);
                                }
                            }
                        }
                    }
                }
            
            }else {
                setDefaultValue($(data));
            }
        });
    }

    /**
     * 设置默认值
     *
     **/
    function setDefaultValue(obj, change_value, current_parameter)
    {
        var show_type     = obj.attr('show_type');
        var default_value = obj.attr('default_value');
        var display       = obj.css('display');
        if (show_type == 'dropbox') {
            if (display == 'none') {
                obj.find("select").find("option[value='']").prop('selected', 'selected');
            } else {
                if (typeof(current_parameter) == 'undefined') {
                    default_value = typeof(change_value) != 'undefined' ? change_value : default_value;
                    obj.find("select").find("option[value='"+default_value+"']").prop('selected', 'selected');
                } else if (typeof(current_parameter) != 'undefined' && typeof(current_parameter.value) != 'undefined') {
                    if ($('.'+current_parameter.en_name).attr('show_type') == 'checkbox') {
                        if (typeof($('.'+current_parameter.en_name).find("input[value='"+current_parameter.value+"']").attr('checked')) == 'undefined') {
                            obj.find("select").find("option[value='"+default_value+"']").prop('selected', 'selected');
                        } else if (typeof(change_value) != 'undefined' && typeof($('.'+current_parameter.en_name).find("input[value='"+current_parameter.value+"']").attr('checked')) != 'undefined') {
                            obj.find("select").find("option[value='"+change_value+"']").prop('selected', 'selected');
                        }
                    } else if ($('.'+current_parameter.en_name).attr('show_type') == 'upload') {
                        default_value = typeof(change_value) != 'undefined' ? change_value : default_value;
                        obj.find("select").find("option[value='"+default_value+"']").prop('selected', 'selected');
                    } else if($('.' + current_parameter.en_name).attr('show_type') == 'dropbox') {
                        obj.find('select').find('option[value="'+change_value+'"]').prop('selected', 'selected');
                    }
                    
                }
            }
        } else if (show_type == 'input') {
            if (display == 'none') {
                if(obj.attr('class') =='confidence'){
                    obj.find("input").val('0.7');
                }else{
                    obj.find("input").val('');
                }
                
            } else {
                default_value = typeof(change_value) != 'undefined' ? change_value : default_value;
                obj.find("input").val(default_value);
            }
        } else if (show_type == 'upload') {
            if (display == 'none') {
                obj.find("input").val('');
            } else {
                default_value = typeof(change_value) != 'undefined' ? change_value : default_value;
                obj.find("input[type='text']").val(default_value);
            }
        }
    }

    /**
     * 清除文件
     *
     **/
    $(".clear_file").click(function() {
        $(this).parent().parent().find("input").val('');
        controllDisplay(2, {en_name:$(this).parent().parent().parent().attr('class'),value:''});
    });


/**
 * 根据项目ID获取任务信息
 *
 **/
function get_task_infos_by_project_id(remove_menu){
    var project_id  = $('#project_id').val();
    if(remove_menu == 1){
        $(".task-menu").remove();
        $('#task_title').val('');
        $('#task_id').val('');
    }else{
        if(isNaN(project_id) || project_id < 1){
            return false;
        }
    }

    if(typeof(project_id) == 'undefined' || project_id.length < 1 || isNaN(project_id)){
        $.tip({width:300, position:'center', type:'warning',msg:'project_id wrong', keep:1,});
        return false;
    }
    //取任务
    $.ajax({
        type    : 'POST',
        url     : "/task/get_task_tips_by_project_id",
        data    : {project_id:project_id},
        dataType: 'json',
        success: function(data) {
            if(data.success === 'true'){
                json_task_infos = $.parseJSON(data.data);
            }else{
                $.tip({width:300, position:'center', type:'warning',msg:data.message, keep:1,});
                return false;
            }
            //任务
            $("#task_title").autoComplete({
                data:json_task_infos,
                number: null,
                autoFocus: true,
                menu: '.task-menu',
                showBtmBar:true,
                callBack: function(data,self) {
                    $('#task_title').val(data.name);
                    $('#task_id').val(data.id);
                }
            });
        }
    });
}
/**
 * 项目autocomplete
 *
 **/
function autocomplete_project(json_project_infos){
    $(".autocomplete_project_name").autoComplete({
        data:json_project_infos,
        number: 300,
        autoFocus: true,
        menu: '.project-menu',
        showBtmBar:true,
        callBack: function(data,self) {
            if(typeof(data.name) == 'undefined' || typeof(data.id) == 'undefined' || data.name.length < 1 || data.id.length < 1){
                return false;
            }
            $('#project_name').val(data.name);
            $('#project_id').val(data.id);
            get_task_infos_by_project_id(1);
        }
    });
}