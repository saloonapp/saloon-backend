// http://www.daterangepicker.com/
(function(){
    var dateFormat = 'DD/MM/YYYY';

    // see common.models.utils.DateRange & common.views.forms.inputDateRange
    $('input.daterange').daterangepicker({
        autoUpdateInput: false,
        autoApply: true,
        locale: {
            format: dateFormat
        }
    });
    $('input.daterange').on('apply.daterangepicker', function(ev, picker) {
        $(this).val(picker.startDate.format(dateFormat) + ' - ' + picker.endDate.format(dateFormat));
    });
    $('input.daterange').on('cancel.daterangepicker', function(ev, picker) {
        $(this).val('');
    });

    // see common.views.forms.inputDate
    $('input.datepicker').daterangepicker({
        singleDatePicker: true,
        autoUpdateInput: false,
        locale: {
            format: dateFormat
        }
    });
    $('input.datepicker').on('apply.daterangepicker', function(ev, picker) {
        $(this).val(picker.startDate.format(dateFormat));
    });
    $('input.datepicker').on('cancel.daterangepicker', function(ev, picker) {
        $(this).val('');
    });
})();

(function(){
    if($('.select2-tags')[0]){
        $('.select2-tags').each(function(){
            $(this).select2({
                width: '100%',
                theme: 'bootstrap',
                placeholder: $(this).attr('placeholder'),
                tags: true,
                tokenSeparators: [',']
            });
        });
    }
})();

// automatically fill form (when possible)
(function(){
    $('input#siteUrl').on('change', function(event){
        var url = $(this).val();
        if(isUrl(url)){
            $.get('/tools/scrapers/utils/metas?url='+url, function(metas){
                setValue(metas, 'title.0', 'input#name');
                setValue(metas, 'description.0', 'textarea#description');
                setValue(metas, 'all.twitter:site.0', 'input#social_twitter_account', function(str){ return str ? str.replace('@', '') : ''; });
                setTags(metas, 'keywords', 'select#tags');
            });
        }
    });
    function isUrl(str) {
        var regexp = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
        return regexp.test(str);
    }
    function setValue(data, dataSelector, eltSelector, dataTransform) {
        var elt = $(eltSelector);
        if(elt.val() === ""){
            var value = getSafe(data, dataSelector);
            elt.val(dataTransform ? dataTransform(value) : value);
        }
    }
    function setTags(data, dataSelector, eltSelector) {
        var elt = $(eltSelector);
        if(elt.val() === null){
            var tags = getSafe(data, dataSelector) || [];
            tags.map(function(tag){
                if(elt.find('option[value="'+tag+'"]').length === 0){
                    elt.append('<option value="'+tag+'">'+tag+'</option>');
                }
            });
            elt.select2('val', tags);
        }
    }
    function getSafe(obj, path) {
        if(typeof path === 'string')  { return getSafe(obj, path.split('.')); }
        if(!Array.isArray(path))      { return obj; }
        if(path.length === 0 || !obj) { return obj; }
        var newObj = obj[path[0]];
        var newPath = path.slice(1);
        return getSafe(newObj, newPath);
    }
})();