/*
    Scripts specifics for conference form
 */

// automatically fill conference form with website metas
(function(){
    $('input#siteUrl').on('change', function(){
        var url = $(this).val();
        if(Utils.isUrl(url)){
            $.get('/tools/scrapers/utils/metas?url='+url, function(metas){
                formValue($('input#name'), Utils.getSafe(metas, 'title.0'));
                formValue($('textarea#description'), Utils.getSafe(metas, 'description.0'));
                formValue($('input#social_twitter_account'), (Utils.getSafe(metas, 'all.twitter:site.0') || '').replace('@', ''));
                setTags(metas, 'keywords', 'select#tags');
            });
        }
    });
    function formValue(elt, value){
        if(elt.val() === '' && value){
            elt.val(value).change();
        }
    }
    function setTags(data, dataSelector, eltSelector){
        var elt = $(eltSelector);
        if(elt.val() === null){
            var tags = Utils.getSafe(data, dataSelector) || [];
            tags.map(function(tag){
                if(elt.find('option[value="'+tag+'"]').length === 0){
                    elt.append('<option value="'+tag+'">'+tag+'</option>');
                }
            });
            elt.select2('val', tags);
        }
    }
})();

// get twitter avatar to fill logo
(function(){
    var $logo = $('input#logo');
    $('input#social_twitter_account').on('change', function(){
        if($logo.val() === '') {
            $.get('/tools/scrapers/twitter/profil?account=' + $(this).val(), function (data) {
                if ($logo.val() === '') {
                    $logo.val(data.result.avatar).change();
                }
            });
        }
    });
})();

// save and fill conference user data
(function(){
    var storageKey = 'conference-createdBy';
    setFormUser(Storage.get(storageKey));
    $('form.conference-form').on('submit', function(){
        Storage.set(storageKey, getFormUser());
    });
    function getFormUser(){
        return {
            name: $('input#createdBy_name').val(),
            email: $('input#createdBy_email').val(),
            siteUrl: $('input#createdBy_siteUrl').val(),
            twitter: $('input#createdBy_twitter').val(),
            public: $('input#createdBy_public').prop('checked')
        };
    }
    function setFormUser(user){
        function setInput(elt, value){ if(elt.val() === ""){ elt.val(value); } }
        if(user){
            setInput($('input#createdBy_name'), user.name);
            setInput($('input#createdBy_email'), user.email);
            setInput($('input#createdBy_siteUrl'), user.siteUrl);
            setInput($('input#createdBy_twitter'), user.twitter);
            $('input#createdBy_public').prop('checked', user.public);
        }
    }
})();

// look for potential duplicates and show them
(function(){
    var $siteUrl = $('input#siteUrl');
    var $dates = $('input#dates');
    if(!$('input#id').val()){
        $siteUrl.on('change', function(){
            showPotentialDuplicatesIfSome();
        });
        $dates.on('change', function(){
            showPotentialDuplicatesIfSome();
        });
    }
    function showPotentialDuplicatesIfSome(){
        var siteUrl = $siteUrl.val();
        var dates = $dates.val();
        fetchPotentialDuplicateConferences(siteUrl, dates).then(function(conferences){
            renderPotentialDuplicateConferences('duplicate-template', conferences);
        });
    }
    function fetchPotentialDuplicateConferences(siteUrl, dates){
        return $.when(
            siteUrl ? $.get('/api/conferences/search?q='+siteUrl) : [{result: []}],
            dates ? $.get('/api/conferences/search?period='+dates) : [{result: []}]
        ).then(function(r1, r2){
            return Utils.unique(r1[0].result.concat(r2[0].result), function(c){ return c.id; });
        });
    }
    function renderPotentialDuplicateConferences(templateId, conferences){
        var $template = $('#'+templateId);
        var $parent = $template.parent();
        if(conferences.length == 0){
            $parent.hide();
            $('.duplicate-warning').hide();
        } else {
            $parent.show();
            $('.duplicate-warning').show();
            $('.'+templateId).remove();
            conferences.map(function(conference){
                var html = $template.clone().html();
                var formatted = html
                    .replace('{{id}}', conference.id)
                    .replace('{{name}}', conference.name)
                    .replace('{{start}}', moment(conference.start).format('DD/MM/YYYY'))
                    .replace('{{end}}', moment(conference.end).format('DD/MM/YYYY'))
                    .replace('{{location}}', conference.location ? conference.location.formatted : '');
                $parent.append($(formatted).addClass(templateId));
            });
        }
    }
})();
